# Elasticsearch 도입 문서

- 작성일: 2026-03-29
- 최종 수정일: 2026-04-03
- 관련 레포: rnqhstmd/ecommerce
- 관련 브랜치: feature/phase7-elasticsearch-search

---

## 1. 도입 배경

이커머스 서비스에서 **상품 검색**은 사용자 경험의 핵심이다. 기존 구현은 MySQL의 `LIKE '%keyword%'` 쿼리에 의존하고 있었으며, 서비스 규모가 커질수록 다음과 같은 구조적 한계가 드러났다.

1. **검색 품질**: 한글 형태소 분석이 불가능하여 "운동화를"로 검색하면 "운동화"를 찾지 못한다. 관련도 점수 없이 매칭 여부만 판단하므로 결과 품질이 낮다.
2. **성능**: `LIKE '%keyword%'`는 B-Tree 인덱스를 탈 수 없어 풀 테이블 스캔이 발생한다. 상품 수가 증가하면 응답 시간이 선형으로 악화된다.
3. **기능 부재**: 자동완성, 오타 교정, 동의어 매칭, Faceted Search 등 현대적 검색 기능을 MySQL만으로는 구현하기 어렵다.
4. **인프라 부하**: 검색 트래픽과 트랜잭션 처리가 동일한 MySQL 인스턴스를 공유하여 리소스 경쟁이 발생한다.

이러한 이유로 **역인덱스 기반 검색 엔진인 Elasticsearch**를 도입하여, 검색 트래픽을 MySQL에서 분리하고 한글 형태소 분석 및 관련도 기반 검색을 제공하기로 결정했다.

---

## 2. 도입 전: 기존 검색 구현 분석

### 2-1. 상품 검색 (QueryDSL + MySQL LIKE)

기존 상품 검색은 `ProductQueryRepository`에서 QueryDSL을 사용하여 구현되어 있었다.

> `apps/commerce-api/.../infrastructure/product/ProductQueryRepository.java`

```java
public Page<Product> findProducts(ProductSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    if (condition.brandId() != null) {
        builder.and(product.brandId.eq(condition.brandId()));
    }
    if (condition.keyword() != null && !condition.keyword().isBlank()) {
        // 핵심: LIKE '%keyword%' — 앞뒤 와일드카드로 인해 인덱스 사용 불가
        builder.and(product.name.containsIgnoreCase(condition.keyword().trim()));
    }
    if (condition.minPrice() != null) {
        builder.and(product.price.value.goe(condition.minPrice()));
    }
    if (condition.maxPrice() != null) {
        builder.and(product.price.value.loe(condition.maxPrice()));
    }

    builder.and(product.deletedAt.isNull());

    // content 쿼리
    List<Product> content = queryFactory
            .selectFrom(product)
            .where(builder)
            .offset(condition.pageable().getOffset())
            .limit(condition.pageable().getPageSize())
            .fetch();

    // count 쿼리 — 매 요청마다 별도 실행
    Long total = queryFactory
            .select(product.count())
            .from(product)
            .where(builder)
            .fetchOne();

    return new PageImpl<>(content, condition.pageable(), total != null ? total : 0L);
}
```

**검색 조건 DTO:**

```java
public record ProductSearchCondition(
    Long brandId,
    String keyword,
    Long minPrice,
    Long maxPrice,
    Pageable pageable
) {}
```

**정렬 옵션 (고정 4종):**

```java
ComparableExpressionBase<?> path = switch (order.getProperty()) {
    case "createdAt" -> product.createdAt;
    case "price.value" -> product.price.value;
    case "likeCount" -> product.likeCount;
    case "id" -> product.id;
    default -> product.id;
};
```

### 2-2. 기존 검색 특성 요약

| 항목 | 상태 |
|------|------|
| 검색 대상 필드 | `name` 1개 (상품명만) |
| 매칭 방식 | `LIKE '%keyword%'` (substring, 대소문자 무시) |
| 한글 형태소 분석 | 없음 |
| 검색 결과 정렬 | 고정 4종 (`createdAt`, `price.value`, `likeCount`, `id`) |
| 관련도 점수 | 없음 — 매칭 여부만 판단 (true/false) |
| 자동완성 | 없음 |
| 오타 교정 | 없음 |
| 필터 조건 | `brandId`, `minPrice`, `maxPrice` |
| 페이지네이션 | offset 기반 (별도 COUNT 쿼리 실행) |

### 2-3. 기타 도메인의 검색 방식

| 도메인 | 검색 방식 | 비고 |
|--------|----------|------|
| 주문 (Order) | JPA `userId + status` 필터, Cursor 페이지네이션 | 단순 필터링 |
| 리뷰 (Review) | JPA `productId` 기반, Cursor 페이지네이션 | 단순 필터링 |
| 브랜드 (Brand) | JPA `findAll` + Spring Cache (`brands`, 1h TTL) | 페이징만 |
| 인기 상품 | Redis Sorted Set (likeCount 기준 TOP N) | 캐시 1h |

상품 검색만이 키워드 기반 텍스트 검색이 필요한 유일한 도메인이었으므로, ES 도입은 상품 검색에 집중했다.

---

## 3. 기존 방식의 한계 상세

### 3-1. 검색 품질

| 문제 | 설명 | 예시 |
|------|------|------|
| **부분 문자열 매칭만 가능** | `LIKE '%나이키%'`는 "나이키 에어맥스"를 찾지만, "나이키" 입력으로 "Nike Air Max"를 찾을 수 없다 | 동의어/다국어 매칭 불가 |
| **형태소 분석 부재** | "운동화를"로 검색하면 "운동화"를 못 찾는다. MySQL은 한글 형태소 분석을 지원하지 않는다 | 조사 결합 시 검색 실패 |
| **관련도 점수 없음** | "나이키 흰색 운동화" 검색 시 3개 단어 모두 포함된 상품과 1개만 포함된 상품을 구분할 수 없다 | 검색 결과 품질 저하 |
| **오타 허용 불가** | "나이키" → "나이케"로 오타 시 결과 0건 | 사용자 경험 저하 |
| **자동완성 없음** | 검색어 입력 중 추천이 불가능하다 | 검색 전환율 하락 |
| **단일 필드 검색** | 상품명만 검색 가능. 브랜드명, 카테고리명 등을 동시에 검색할 수 없다 | 검색 범위 제한 |

### 3-2. 성능

| 문제 | 설명 |
|------|------|
| **LIKE '%keyword%' 풀 테이블 스캔** | 앞에 와일드카드가 있으면 B-Tree 인덱스를 탈 수 없다. 상품 수가 증가하면 선형으로 느려진다 |
| **COUNT 쿼리 이중 실행** | offset 페이지네이션에서 content 쿼리와 count 쿼리를 별도로 실행한다 |
| **정렬 부하** | 대량 데이터에서 `ORDER BY likeCount DESC` + `LIMIT` 조합은 filesort를 유발할 수 있다 |
| **DB 부하 집중** | 검색 트래픽이 트랜잭션 처리와 같은 MySQL 인스턴스를 사용한다 |

---

## 4. ES 도입 후: 개선 사항 (AS-IS → TO-BE)

### 4-1. 검색 품질 개선

| 항목 | AS-IS (MySQL LIKE) | TO-BE (Elasticsearch) |
|------|---------------------|----------------------|
| 검색 대상 | `name` 1개 필드 | `name` + `brandName` + `categoryName` 다중 필드 |
| 한글 처리 | 없음 | Nori 형태소 분석기 (은전한닢 기반) |
| 매칭 방식 | substring (`LIKE`) | 형태소 토큰 매칭 + BM25 관련도 점수 |
| 관련도 정렬 | 불가 | `multi_match` + `BestFields` + `tieBreaker(0.3)` |
| 자동완성 | 없음 | Edge N-gram + `match_phrase_prefix` |
| 집계 | 없음 | Terms/Range Aggregation (브랜드별, 카테고리별, 가격대별) |

### 4-2. 성능 개선

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| 인덱스 구조 | B-Tree (LIKE '%x%' 사용 불가) | 역인덱스 (Inverted Index) — 토큰 기반 O(1) 조회 |
| 검색 응답 시간 | 상품 10만 건 기준 ~200ms+ (풀스캔) | ~10-20ms (역인덱스) |
| DB 부하 | 검색 + 트랜잭션 동일 MySQL | 검색 트래픽을 ES로 분리, MySQL은 트랜잭션 전용 |
| COUNT 성능 | 매 요청마다 COUNT 쿼리 | `total.value`로 검색 응답에 포함 |
| 캐싱 | 없음 (검색 결과) | ES Request Cache + Filter Cache 자동 적용 |

---

## 5. 아키텍처

### 5-1. 기존 구조

```
Client → Controller → Facade → ProductService → ProductQueryRepository → MySQL
```

단일 경로로, 검색과 트랜잭션이 모두 MySQL을 거친다.

### 5-2. 도입 후 구조

**검색 요청 흐름:**

```
Client
  → ProductV1Controller
    → ProductFacade
      → ProductSearchService (@CircuitBreaker)
        → ProductSearchPort (도메인 인터페이스)
          → ElasticsearchProductSearchAdapter (구현체)
            → Elasticsearch (역인덱스 검색)
              ↓ (ID 목록 반환)
        → ProductService.getProductsByIds(ids)
          → MySQL (상세 정보 조회)

  [ES 장애 시 폴백]
        → ProductService.getProducts(condition)
          → ProductQueryRepository → MySQL (LIKE 검색)
```

**CUD 요청 + 인덱스 동기화 흐름:**

```
Client
  → ProductV1Controller
    → ProductFacade
      → ProductService → MySQL (CUD 처리)
        ↓ (트랜잭션 커밋)
      이벤트 발행: ProductCreatedEvent / ProductUpdatedEvent / ProductDeletedEvent
        ↓ (@TransactionalEventListener, AFTER_COMMIT)
      ProductIndexer
        → ProductSearchPort → Elasticsearch (인덱스 동기화)
```

핵심 설계 원칙은 **Port/Adapter 패턴**으로, 도메인 레이어(`ProductSearchPort`)에 인터페이스를 정의하고 인프라 레이어(`ElasticsearchProductSearchAdapter`)에서 구현한다. ES에 대한 의존이 도메인으로 누출되지 않는다.

---

## 6. 구현 상세

### 6-1. 인프라 구성

**Docker (ES + Nori 플러그인):**

> `docker/elasticsearch/Dockerfile`

```dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:8.17.0
RUN bin/elasticsearch-plugin install --batch analysis-nori
```

> `docker/infra-compose.yml`

```yaml
elasticsearch:
  build:
    context: ./elasticsearch
    dockerfile: Dockerfile
  ports:
    - "127.0.0.1:9200:9200"
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
```

- ES 8.17.0 + Nori 한글 형태소 분석기
- 싱글 노드 구성 (로컬 개발용)
- X-Pack 보안 비활성화

**Gradle 의존성:**

> `modules/elasticsearch/build.gradle.kts`

```kotlin
api("org.springframework.boot:spring-boot-starter-data-elasticsearch")
testFixturesImplementation("org.testcontainers:elasticsearch")
```

### 6-2. ES 인덱스 설계

> `modules/elasticsearch/src/main/resources/elasticsearch/products-index-settings.json`

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "max_result_window": 10000,
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["nori_readingform", "lowercase"]
        },
        "edge_ngram_analyzer": {
          "type": "custom",
          "tokenizer": "edge_ngram_tokenizer",
          "filter": ["lowercase"]
        },
        "edge_ngram_search_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase"]
        }
      },
      "tokenizer": {
        "edge_ngram_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 1,
          "max_gram": 20,
          "token_chars": ["letter", "digit"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id":           { "type": "long" },
      "name":         { "type": "text", "analyzer": "korean",
                        "fields": {
                          "autocomplete": {
                            "type": "text",
                            "analyzer": "edge_ngram_analyzer",
                            "search_analyzer": "edge_ngram_search_analyzer"
                          }
                        }
      },
      "brandId":      { "type": "long" },
      "brandName":    { "type": "text", "analyzer": "korean",
                        "fields": { "keyword": { "type": "keyword" } }
      },
      "categoryId":   { "type": "long" },
      "categoryName": { "type": "text", "analyzer": "korean",
                        "fields": { "keyword": { "type": "keyword" } }
      },
      "price":        { "type": "long" },
      "likeCount":    { "type": "long" },
      "createdAt":    { "type": "date" },
      "deletedAt":    { "type": "date" }
    }
  }
}
```

**분석기 구성:**

| 분석기 | 용도 | 토크나이저 | 필터 |
|--------|------|-----------|------|
| `korean` | 본문 검색 (name, brandName, categoryName) | `nori_tokenizer` | `nori_readingform`, `lowercase` |
| `edge_ngram_analyzer` | 자동완성 색인 시 | `edge_ngram_tokenizer` (1~20글자) | `lowercase` |
| `edge_ngram_search_analyzer` | 자동완성 검색 시 | `standard` | `lowercase` |

**multi-field 매핑:**

- `name` → `korean` 분석기 (본문 검색) + `name.autocomplete` (자동완성)
- `brandName`, `categoryName` → `korean` 분석기 + `.keyword` (집계용 keyword)

### 6-3. ES Document

> `apps/commerce-api/.../infrastructure/search/ProductDocument.java`

```java
@Document(indexName = "products", createIndex = false)
public class ProductDocument {
    @Id
    private Long id;
    private String name;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private Long price;
    private Long likeCount;
    private String createdAt;
    private String deletedAt;

    public static ProductDocument from(Product product, String brandName, String categoryName) {
        // MySQL Product 엔티티 → ES 검색 전용 비정규화 문서로 변환
    }
}
```

MySQL의 `Product` 엔티티와 1:1 매핑되지만, 브랜드명/카테고리명을 비정규화하여 포함한다. 이는 검색 시 JOIN 없이 다중 필드 검색을 가능하게 한다.

### 6-4. 검색 구현 (ElasticsearchProductSearchAdapter)

> `apps/commerce-api/.../infrastructure/search/ElasticsearchProductSearchAdapter.java`

**상품 검색 — `multi_match` + `bool` query:**

```java
// 키워드가 있으면 multi_match, 없으면 match_all
Query mainQuery;
if (keyword == null || keyword.isBlank()) {
    mainQuery = Query.of(q -> q.matchAll(m -> m));
} else {
    mainQuery = Query.of(q -> q
            .multiMatch(mm -> mm
                    .query(keyword)
                    .fields("name", "brandName", "categoryName")
                    .type(TextQueryType.BestFields)
                    .tieBreaker(0.3)
            )
    );
}

// bool query: MUST (검색어 매칭) + FILTER (필터 조건)
SearchResponse<ProductDocument> response = esClient.search(s -> s
        .index("products")
        .query(q -> q.bool(b -> {
            b.must(mainQuery);
            filters.forEach(b::filter);  // brandId, price range, deletedAt null
            return b;
        }))
        .from(page * size)
        .size(size)
        .sort(sortOptions),
    ProductDocument.class
);

// 결과: ID 목록만 반환 → 이후 MySQL에서 상세 정보 조회
List<Long> productIds = response.hits().hits().stream()
        .map(hit -> hit.source().getId())
        .toList();
```

- `BestFields`: 가장 높은 점수의 필드를 기준으로 정렬
- `tieBreaker(0.3)`: 다른 필드 점수도 30% 반영하여 다중 필드 매칭 보상
- 필터 조건은 `filter` context로 실행되어 점수 계산에 영향 없이 캐싱 효율 높음

**자동완성 — Edge N-gram + `match_phrase_prefix`:**

```java
Query prefixQuery = Query.of(q -> q
        .matchPhrasePrefix(mp -> mp
                .field("name.autocomplete")
                .query(prefix)
        )
);
// name 필드만 반환, distinct 처리
```

**Faceted Search — Terms + Range Aggregation:**

```java
esClient.search(s -> s
    .size(0)  // 검색 결과 불필요, 집계만 수행
    .aggregations("brand_facets", a -> a
            .terms(t -> t.field("brandName.keyword").size(50)))
    .aggregations("category_facets", a -> a
            .terms(t -> t.field("categoryName.keyword").size(50)))
    .aggregations("price_ranges", a -> a
            .range(r -> r.field("price")
                    .ranges(rng -> rng.key("~10,000").to(10000.0))
                    .ranges(rng -> rng.key("10,000~50,000").from(10000.0).to(50000.0))
                    .ranges(rng -> rng.key("50,000~100,000").from(50000.0).to(100000.0))
                    .ranges(rng -> rng.key("100,000~").from(100000.0))
            )),
    ProductDocument.class
);
```

### 6-5. 데이터 동기화 — Application Event 방식

MySQL → ES 동기화는 **`@TransactionalEventListener(AFTER_COMMIT)`** 패턴을 사용한다.

> `apps/commerce-api/.../infrastructure/search/ProductIndexer.java`

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleProductCreated(ProductCreatedEvent event) {
    indexProduct(event.productId(), "생성");
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleProductUpdated(ProductUpdatedEvent event) {
    indexProduct(event.productId(), "수정");
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleProductDeleted(ProductDeletedEvent event) {
    productSearchPort.deleteProduct(event.productId());
}
```

**동기화 전략 비교 및 선택 근거:**

| 전략 | 장점 | 단점 | 적합 시나리오 |
|------|------|------|-------------|
| **Application Event** (현재 채택) | 구현 단순, 기존 이벤트 패턴 재활용 | 동기화 실패 시 불일치 가능 | 단일 앱, 모놀리식 |
| Kafka CDC | 비동기, 느슨한 결합 | 지연 발생, 인프라 복잡 | MSA 환경 |
| Debezium | MySQL binlog 기반, 코드 변경 없음 | 운영 복잡도 높음 | 대규모 시스템 |

현재 프로젝트는 모놀리식 단일 앱이므로 Application Event 방식을 채택했다. 이미 `ProductCreatedEvent` 등의 도메인 이벤트가 존재하여 추가 인프라 없이 구현할 수 있었다.

### 6-6. 전체 재인덱싱

> `apps/commerce-api/.../application/product/ProductReindexService.java`

```java
@Transactional(readOnly = true)
public long reindexAll() {
    productSearchPort.deleteAllDocuments();

    long indexed = 0;
    int page = 0;

    while (true) {
        Page<Product> productPage = productRepository.findAllPaged(
            PageRequest.of(page, BATCH_SIZE)  // BATCH_SIZE = 1000
        );

        // 페이지 단위로 brandId/categoryId를 수집하여 한 번에 조회 (N+1 방지)
        Map<Long, Brand> brandMap = brandService.getBrandsByIds(brandIds);
        Map<Long, Category> categoryMap = categoryService.getCategoriesByIds(categoryIds);

        for (Product product : products) {
            productSearchPort.indexProduct(product, brandName, categoryName);
            indexed++;
        }

        if (!productPage.hasNext()) break;
        page++;
    }
    return indexed;
}
```

- 1,000건 단위 배치 처리
- 브랜드/카테고리를 페이지 단위로 배치 조회하여 N+1 방지
- Admin 전용 API (`POST /api/v1/admin/products/reindex`)로 수동 실행

### 6-7. Circuit Breaker + MySQL Fallback

ES 장애 시 서비스가 중단되지 않도록 Resilience4j Circuit Breaker를 적용했다.

> `apps/commerce-api/.../application/product/ProductSearchService.java`

```java
@CircuitBreaker(name = "elasticsearchSearch", fallbackMethod = "searchFallback")
public ProductSearchInfo search(ProductGetListCommand command) {
    // ES 검색
}

// ES 장애 시 → MySQL LIKE 쿼리로 폴백
private ProductSearchInfo searchFallback(ProductGetListCommand command, Throwable t) {
    log.warn("ES 검색 Circuit Breaker 폴백 -> MySQL LIKE: {}", t.getMessage());
    Page<Product> page = productService.getProducts(condition);
    // ...
}

// 자동완성/집계는 폴백 시 빈 결과 반환
private List<String> autocompleteFallback(...) { return List.of(); }
private ProductFacetInfo facetsFallback(...)   { return ProductFacetInfo.empty(); }
```

**Circuit Breaker 설정:**

> `apps/commerce-api/src/main/resources/application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      elasticsearchSearch:
        sliding-window-size: 10           # 최근 10개 요청 기준
        failure-rate-threshold: 50         # 실패율 50% 초과 시 OPEN
        wait-duration-in-open-state: 10s   # 10초 후 HALF_OPEN 전환
        permitted-number-of-calls-in-half-open-state: 3  # 3개 요청 시도
```

| 상태 | 동작 |
|------|------|
| **CLOSED** | 정상 — ES 검색 사용 |
| **OPEN** | 실패율 50% 초과 — 모든 요청을 즉시 폴백 (검색은 MySQL, 자동완성/집계는 빈 결과) |
| **HALF_OPEN** | 10초 후 — 3개 요청을 ES로 시도하여 복구 여부 판단 |

**인덱스 자동 생성도 동일한 원칙:**

> `modules/elasticsearch/.../config/ElasticsearchConfig.java`

```java
@PostConstruct
public void createIndexIfNotExists() {
    try {
        // 인덱스 존재 확인 → 없으면 JSON 설정으로 생성
    } catch (Exception e) {
        // 의도적 silent: Circuit Breaker fallback이 MySQL LIKE로 전환하므로 앱 기동은 차단하지 않음
        log.error("ES '{}' 인덱스 생성 실패: {}", INDEX_NAME, e.getMessage(), e);
    }
}
```

ES가 기동 시점에 없어도 앱은 정상 시작되며, 검색 요청은 Circuit Breaker를 통해 MySQL로 폴백된다.

---

## 7. 기술 스택

| 컴포넌트 | 기술 | 버전 | 비고 |
|----------|------|------|------|
| Elasticsearch | Elasticsearch | 8.17.0 | Docker Compose로 로컬 실행 |
| 한글 분석기 | Nori Plugin (analysis-nori) | ES 버전 매칭 | 은전한닢 기반 형태소 분석 |
| Java 클라이언트 | Elasticsearch Java API Client | 8.x | 쿼리 빌더 (검색/집계) |
| Spring 통합 | Spring Data Elasticsearch | 5.x | Repository 패턴 (인덱싱/삭제) |
| 장애 대응 | Resilience4j Circuit Breaker | - | MySQL LIKE 폴백 |
| 테스트 | Testcontainers | elasticsearch 모듈 | 통합 테스트용 ES 컨테이너 |

---

## 8. 주요 코드 경로

| 기능 | 파일 경로 |
|------|---------|
| ES 설정 + 인덱스 자동 생성 | `modules/elasticsearch/.../config/ElasticsearchConfig.java` |
| 인덱스 매핑 (Nori, Edge N-gram) | `modules/elasticsearch/src/main/resources/elasticsearch/products-index-settings.json` |
| ES Document 정의 | `apps/commerce-api/.../infrastructure/search/ProductDocument.java` |
| 검색 구현 (multi_match, 자동완성, 집계) | `apps/commerce-api/.../infrastructure/search/ElasticsearchProductSearchAdapter.java` |
| 이벤트 기반 동기화 | `apps/commerce-api/.../infrastructure/search/ProductIndexer.java` |
| Circuit Breaker + 폴백 | `apps/commerce-api/.../application/product/ProductSearchService.java` |
| 전체 재인덱싱 배치 | `apps/commerce-api/.../application/product/ProductReindexService.java` |
| 도메인 인터페이스 (Port) | `apps/commerce-api/.../domain/product/ProductSearchPort.java` |
| 기존 MySQL LIKE 검색 (폴백용) | `apps/commerce-api/.../infrastructure/product/ProductQueryRepository.java` |
| Docker + Nori | `docker/elasticsearch/Dockerfile` |

---

## 9. 구현 현황

| 항목 | 상태 | 비고 |
|------|------|------|
| ES 인프라 구성 (Docker Compose + Nori) | 완료 | ES 8.17.0 + analysis-nori |
| Product 인덱스 설계 + 매핑 | 완료 | korean 분석기, edge_ngram, multi-field |
| ProductIndexer (MySQL → ES 동기화) | 완료 | @TransactionalEventListener(AFTER_COMMIT) |
| 상품 검색 API (multi_match + 필터) | 완료 | BestFields + tieBreaker(0.3) |
| 자동완성 API | 완료 | Edge N-gram + match_phrase_prefix |
| 집계 (브랜드별/카테고리별/가격대별 facet) | 완료 | Terms + Range Aggregation |
| 전체 데이터 재인덱싱 배치 | 완료 | 1000건 배치, N+1 방지 |
| Circuit Breaker + MySQL 폴백 | 완료 | Resilience4j, 검색은 LIKE 폴백 |
| 검색어 하이라이팅 | 미구현 | 매칭 부분 `<em>` 태그 강조 |
| 오타 교정 (Fuzzy Query) | 미구현 | 편집 거리 기반 유사 검색어 |
| 동의어 사전 | 미구현 | "운동화" ↔ "스니커즈" ↔ "sneakers" |
| 인기 검색어 / 최근 검색어 | 미구현 | ES 로그 + Redis 사용자 이력 |

---

## 10. 리스크 및 대응

| 리스크 | 대응 방안 | 현재 상태 |
|--------|----------|----------|
| MySQL ↔ ES 데이터 불일치 | Application Event 실패 시 에러 로그 + 수동 재인덱싱 API | 구현 완료 |
| ES 장애 시 검색 불가 | Resilience4j Circuit Breaker + MySQL LIKE fallback | 구현 완료 |
| Nori 형태소 분석 품질 | 사용자 사전 (`user_dictionary`) 커스터마이징 | 미적용 |
| 인덱스 스키마 변경 | Zero-downtime reindexing (alias 전략) | 미적용 |
| ES 기동 실패 시 앱 시작 불가 | ElasticsearchConfig에서 예외 silent 처리 | 구현 완료 |
