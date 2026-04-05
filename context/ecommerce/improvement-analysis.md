# Phase 7: Elasticsearch 검색 시스템 도입 — 개선 분석 보고서

- 작성일: 2026-03-29
- 관련 PR: #12

---

## 1. 도입 배경

### 1-1. 기존 검색 시스템의 기술적 한계

기존 검색은 `ProductQueryRepository`에서 QueryDSL + MySQL `LIKE` 쿼리로 구현되어 있었다.

```java
// 기존 ProductQueryRepository.java
builder.and(product.name.containsIgnoreCase(condition.keyword().trim()));
```

이 한 줄은 다음 문제들의 근원이다.

**풀 테이블 스캔 (Full Table Scan)**

`LIKE '%keyword%'`는 앞에 와일드카드가 있어 MySQL B-Tree 인덱스를 사용할 수 없다. 상품 수에 비례하여 응답 시간이 선형으로 증가한다. 상품 10만 건 이상에서 수백 ms 수준의 지연이 발생한다.

**COUNT 쿼리 이중 실행**

offset 페이지네이션 구조에서 content 쿼리와 count 쿼리를 매 요청마다 두 번 실행한다. 대량 데이터에서 두 쿼리 모두 풀 스캔을 수행하므로 부하가 두 배가 된다.

**한글 형태소 분석 부재**

MySQL은 한글 형태소 분석을 내장하지 않는다. "운동화를"로 검색하면 `LIKE '%운동화를%'`가 실행되고, 상품명에는 "운동화를"이라는 부분 문자열이 없으므로 0건이 반환된다. 조사("를", "의", "에서", "는")가 붙으면 검색에 실패한다.

**단일 필드 검색**

상품명(`name`) 하나만 검색 대상이었다. "나이키"를 검색해도 `brandName`이 "나이키"인 상품이 상품명에 "나이키"를 포함하지 않으면 누락된다. 카테고리명 검색도 불가능했다.

**관련도 점수(Relevance Score) 없음**

매칭 여부(true/false)만 판단하므로 "나이키 흰색 운동화"를 검색할 때 세 단어 모두 포함된 상품과 한 단어만 포함된 상품을 구분할 수 없다. 결과 품질이 낮다.

**기능 확장 불가**

자동완성, 집계(faceted search), 오타 교정 등 현대적 검색 기능은 MySQL LIKE 구조에서 구현할 수 없다.

### 1-2. 도입 목표

PRD에서 추출한 구체적 목표는 세 가지다.

1. **검색 품질**: Nori 형태소 분석기로 조사 분리, multi_match로 상품명·브랜드명·카테고리명 동시 검색, BM25 관련도 점수 적용
2. **성능**: 역인덱스(Inverted Index) 기반 토큰 매칭으로 풀 스캔 제거, MySQL을 트랜잭션 전용으로 분리
3. **기능 확장**: 자동완성(Edge N-gram), 집계(terms + range aggregation), 재인덱싱 배치

---

## 2. 아키텍처 설계

### 2-1. 도입 전후 아키텍처 비교

**도입 전**

```
[검색 요청]
Client
  └── Controller
        └── ProductFacade
              └── ProductService
                    └── ProductQueryRepository (QueryDSL)
                          └── MySQL  <-- LIKE '%keyword%' 풀 스캔
```

**도입 후**

```
[검색 요청]
Client
  └── Controller
        └── ProductFacade
              └── ProductSearchService
                    ├── [정상] ProductSearchPort
                    │     └── ElasticsearchProductSearchAdapter
                    │           └── Elasticsearch  <-- 역인덱스 조회
                    │                 │ ID 목록 반환
                    │           ProductService
                    │                 └── MySQL  <-- 상세 조회 (by IDs)
                    └── [Circuit Breaker 발동] ProductRepository
                                └── MySQL  <-- LIKE fallback

[CUD 요청]
Client
  └── Controller
        └── ProductFacade
              ├── ProductService
              │     └── MySQL  <-- 트랜잭션 처리
              └── ApplicationEventPublisher
                    └── [AFTER_COMMIT]
                          ProductIndexer
                                └── Elasticsearch  <-- 동기화
```

### 2-2. Port/Adapter 패턴 (레이어 구조)

검색 기능은 도메인 레이어에 `ProductSearchPort` 인터페이스를 정의하고, 인프라 레이어에 `ElasticsearchProductSearchAdapter`가 구현하는 Port/Adapter 패턴으로 설계했다.

```
도메인 레이어 (domain/product/)
  ProductSearchPort                  <-- 인터페이스 (검색 계약 정의)
    - searchProducts(...)
    - autocomplete(...)
    - facets(...)

인프라 레이어 (infrastructure/search/)
  ElasticsearchProductSearchAdapter  <-- ProductSearchPort 구현체
  ProductDocument                    <-- ES 문서 매핑
  ProductSearchRepository            <-- Spring Data ES Repository
  ProductIndexer                     <-- 동기화 리스너
```

이 설계의 이점은 ES 장애 시 또는 테스트 시 도메인 코드 변경 없이 구현체를 교체할 수 있다는 점이다. Circuit Breaker fallback이 `ProductRepository`(MySQL)로 전환되는 것도 이 인터페이스 덕분에 가능하다.

### 2-3. 데이터 동기화 전략 (Application Event + AFTER_COMMIT)

MySQL의 상품 CUD 트랜잭션이 커밋된 뒤 ES 인덱스를 갱신한다. `AFTER_COMMIT` 시점을 사용하므로 트랜잭션이 롤백되면 ES 동기화도 발생하지 않는다.

```
[상품 생성 흐름]

ProductFacade.createProduct()
  ├── productService.createProduct()  -- MySQL INSERT (트랜잭션)
  └── eventPublisher.publishEvent(new ProductCreatedEvent(productId))

  [트랜잭션 COMMIT 완료]
       |
       v  @TransactionalEventListener(phase = AFTER_COMMIT)
  ProductIndexer.handleProductCreated()
       ├── productRepository.findById(productId)  -- MySQL 조회
       ├── brandService.getBrand(brandId)          -- 브랜드명 비정규화
       ├── categoryService.getById(categoryId)     -- 카테고리명 비정규화
       └── productSearchRepository.save(document)  -- ES 인덱싱

[상품 삭제 흐름]
  ProductDeletedEvent --> productSearchRepository.deleteById(productId)
```

동기화 실패 시 `log.error`만 기록하고 상위 트랜잭션을 롤백하지 않는다. 최대 수 초의 지연이 허용되며, 전체 재동기화가 필요할 때는 재인덱싱 배치 API를 사용한다.

### 2-4. Circuit Breaker (Resilience4j)

ES 장애 시 서비스 전체가 중단되지 않도록 Resilience4j Circuit Breaker를 적용했다.

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      elasticsearchSearch:
        sliding-window-size: 10
        failure-rate-threshold: 50        # 10건 중 5건 이상 실패 시 개방
        wait-duration-in-open-state: 10s  # 10초 후 half-open 전환
        permitted-number-of-calls-in-half-open-state: 3
        register-health-indicator: true
```

Fallback 동작:
- 검색(`search`): MySQL LIKE 방식으로 전환, 동일한 응답 형식 유지
- 자동완성(`autocomplete`): 빈 배열 반환
- 집계(`facets`): 빈 집계 결과 반환

---

## 3. 구현 상세

### 3-1. 인프라 구성

**커스텀 Docker 이미지 (Nori 플러그인)**

```dockerfile
# docker/elasticsearch/Dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:8.17.0
RUN bin/elasticsearch-plugin install --batch analysis-nori
```

Nori 플러그인을 Dockerfile에서 설치하는 방식을 선택한 이유: CI 환경에서 매번 플러그인을 다운로드하지 않고 이미지를 캐시할 수 있어 재현성과 속도 모두 확보된다.

**Docker Compose 구성 (infra-compose.yml)**

```yaml
elasticsearch:
  build:
    context: ./elasticsearch
    dockerfile: Dockerfile
  container_name: elasticsearch
  ports:
    - "127.0.0.1:9200:9200"   # 로컬호스트로만 노출
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false   # 로컬 환경 한정
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 10
```

**Testcontainers 통합 테스트**

기존 `MySqlTestContainersConfig` 패턴을 따라 `ElasticsearchTestContainersConfig`를 `modules/elasticsearch/src/testFixtures`에 구현했다. 동일한 Nori 커스텀 Dockerfile을 사용하므로 로컬과 CI 테스트 환경이 일치한다.

### 3-2. ES 인덱스 설계

`products-index-settings.json`의 주요 설정:

**분석기 구성**

| 분석기 | 사용처 | 동작 |
|--------|--------|------|
| `korean` | `name`, `brandName`, `categoryName` | Nori 형태소 분석 + 읽기형 변환 + 소문자화 |
| `edge_ngram_analyzer` | `name.autocomplete` 인덱싱 | 접두사 방향으로 1~20글자 N-gram 생성 |
| `edge_ngram_search_analyzer` | `name.autocomplete` 검색 | standard tokenizer (입력을 그대로 사용) |

인덱싱 시와 검색 시 분석기를 다르게 설정한 이유: 인덱싱 시에는 N-gram으로 모든 접두사 토큰을 생성하고, 검색 시에는 입력 키워드를 그대로 사용해야 "나이"라는 입력이 N-gram 분해 없이 "나이"로 검색된다.

**필드 매핑**

| 필드 | 타입 | 분석기 | 서브필드 |
|------|------|--------|----------|
| `name` | text | korean | `name.autocomplete` (edge_ngram_analyzer) |
| `brandName` | text | korean | `brandName.keyword` (keyword, 집계용) |
| `categoryName` | text | korean | `categoryName.keyword` (keyword, 집계용) |
| `price` | long | - | 범위 필터, 가격대 집계 |
| `likeCount` | long | - | 정렬용 |
| `createdAt` | date | - | 정렬용 |
| `deletedAt` | date | - | null 여부로 삭제 필터링 |

`max_result_window: 10000`으로 설정하여 offset × size의 최대값을 제한한다. 이를 초과하는 요청은 400을 반환한다.

인덱스 자동 생성: `ElasticsearchConfig`의 `@PostConstruct`에서 `products` 인덱스 존재 여부를 확인하고 없으면 `products-index-settings.json`을 읽어 생성한다.

### 3-3. 검색 쿼리 구현

**키워드 검색 (multi_match)**

```java
// ElasticsearchProductSearchAdapter.java
Query.of(q -> q
    .multiMatch(mm -> mm
        .query(keyword)
        .fields("name", "brandName", "categoryName")
        .type(TextQueryType.BestFields)
        .tieBreaker(0.3)
    )
);
```

- `BestFields`: 여러 필드 중 가장 높은 점수를 가진 필드의 점수를 채택
- `tieBreaker(0.3)`: 나머지 필드 점수의 30%를 합산하여 여러 필드에 매칭되는 문서를 보상
- keyword가 blank이면 `match_all`로 전환

**필터 구성 (filter context)**

```java
// deletedAt 없는 문서만 (소프트 삭제되지 않은 상품)
filters.add(Query.of(f -> f
    .bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt"))))
));
// brandId, minPrice, maxPrice 조건 추가
```

filter context를 사용하면 관련도 점수 계산에 영향을 주지 않고, ES의 Filter Cache를 활용할 수 있다.

**자동완성 (match_phrase_prefix)**

```java
Query.of(q -> q
    .matchPhrasePrefix(mp -> mp
        .field("name.autocomplete")
        .query(prefix)
    )
);
```

**집계 (terms + range aggregation)**

`size(0)`으로 실제 문서를 반환하지 않고 집계 결과만 가져와 네트워크 비용을 최소화한다. 브랜드·카테고리는 `terms` 집계, 가격대는 `range` 집계(4개 고정 구간)를 사용한다.

### 3-4. Two-phase 검색 (ES ID 조회 → MySQL 상세 조회)

`ProductFacade.getProducts()`는 두 단계로 동작한다.

```
Phase 1: ES에서 ID 목록 + 총 건수 조회
         productSearchService.search(command)
         --> ProductSearchResult(productIds, totalHits)

Phase 2: MySQL에서 상세 조회 (ES 결과 순서 보존)
         productService.findProductsByIds(searchInfo.productIds())
         --> IN (id1, id2, ...) 조회

순서 보존: ES가 반환한 productIds 순서대로
         Map<Long, Product>에서 재조립
         --> 관련도 점수 순서 유지
```

MySQL `IN (...)` 조회 결과는 순서가 보장되지 않으므로 `Map<Long, Product>`으로 변환 후 ES 순서대로 재조립한다.

### 3-5. 신규 API 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/products/search/autocomplete` | 자동완성 (keyword, size) | 불필요 |
| GET | `/api/v1/products/search/facets` | 집계 (keyword, minPrice, maxPrice) | 불필요 |
| POST | `/api/v1/admin/products/reindex` | 전체 재인덱싱 배치 | ADMIN 전용 |

자동완성 API: `size` 기본값 10, 최대 20. 초과 시 400 반환. 빈 keyword → 빈 배열 반환.

재인덱싱 배치: 1,000건 단위 bulk 처리(`BATCH_SIZE = 1000`). 부분 실패 시 `log.warn` 후 계속 진행. 완료 후 `{"indexed": N}` 응답.

---

## 4. 성능 측정 결과

### 4-1. 테스트 환경

| 항목 | 내용 |
|------|------|
| 테스트 방식 | Testcontainers (ES 8.17.0 + Nori, MySQL 8.0) |
| 측정 방법 | N회 반복 후 중간값(median) 사용 |
| 소규모 측정 횟수 | 3회 median |
| 대규모 측정 횟수 | 5회 median |
| 소규모 데이터셋 | 브랜드 10개, 카테고리 5개, 수식어 20개 조합, 상품 10,000건 |
| 대규모 데이터셋 | 브랜드 20개, 카테고리 10개, 수식어 50개 조합, 상품 500,000건 |
| 상품명 패턴 | `{브랜드명} {수식어} {카테고리명}` |

### 4-2. 소규모 테스트 (10,000건)

#### 키워드 검색 성능

| 키워드 | ES | MySQL LIKE | 개선율 |
|--------|-----|------------|--------|
| 나이키 운동화 | 14ms | 9ms | 0.6배 |
| 프리미엄 | 10ms | 13ms | 1.3배 |
| 에어 맥스 | 7ms | 10ms | 1.4배 |
| 울트라 | 6ms | 7ms | 1.2배 |

10,000건 규모에서는 ES와 MySQL의 응답 시간 차이가 미미하다. 테이블 크기가 작아 MySQL 버퍼 풀에 전체 데이터가 올라가므로 풀 스캔이더라도 메모리 내 처리로 빠른 편이다. 반면 ES는 JVM 오버헤드와 HTTP 통신 비용이 추가되어 이 규모에서는 우위가 크지 않다.

#### 형태소 분석 품질 (10,000건)

| 검색어 | ES 결과 | MySQL 결과 | 차이 |
|--------|---------|------------|------|
| 운동화를 | 6,000건 | 0건 | ES만 매칭 (조사 분리) |
| 프리미엄의 | 510건 | 0건 | ES만 매칭 (조사 분리) |
| 러닝화에서 | 6,000건 | 0건 | ES만 매칭 (조사 분리) |

10,000건에서도 형태소 분석 품질 차이는 명확하다. MySQL은 모든 케이스에서 0건이다.

### 4-3. 대규모 테스트 (500,000건)

#### 키워드 검색 성능

| 키워드 | ES | MySQL LIKE | 개선율 |
|--------|-----|------------|--------|
| 나이키 운동화 | 43ms | 424ms | **9.9배** |
| 프리미엄 러닝화 | 29ms | 167ms | **5.8배** |
| 에어 맥스 | 19ms | 439ms | **23.1배** |
| 울트라 부스트 | 15ms | 398ms | **26.5배** |
| 고어텍스 등산화 | 25ms | 171ms | **6.8배** |

데이터 규모가 50배(10,000 → 500,000)로 늘어났을 때 MySQL은 응답 시간이 10~40배 증가하지만, ES는 2~4배 수준의 증가에 그친다. ES가 최대 26.5배 빠른 결과를 보인다.

ES 응답 시간이 상대적으로 일정한 이유: 역인덱스는 "토큰 → 문서 목록" 직접 조회 구조로, 전체 문서 수에 관계없이 해당 토큰을 포함하는 문서만 찾는다. 반면 MySQL LIKE는 전체 행을 스캔하므로 행 수에 비례한다.

#### 깊은 페이지 성능 (500,000건, 키워드: "나이키")

| 페이지 | ES | MySQL LIKE | 개선율 |
|--------|-----|------------|--------|
| page=0 | 7ms | 184ms | **26.3배** |
| page=10 | 10ms | 185ms | **18.5배** |
| page=100 | 7ms | 175ms | **25.0배** |
| page=499 | 13ms | 242ms | **18.6배** |

MySQL의 offset 기반 페이지네이션은 page가 깊어질수록 `OFFSET n × size`만큼 행을 건너뛰어야 한다. 검색 결과 전체를 스캔한 뒤 앞의 행들을 버리는 구조이므로 성능이 점진적으로 저하된다.

ES는 내부 우선순위 큐로 필요한 결과만 추출하므로 페이지 깊이에 따른 성능 저하가 훨씬 적다.

#### 인덱싱 성능 (500,000건)

| 작업 | 소요 시간 |
|------|----------|
| MySQL 50만 건 삽입 | 227.2s |
| ES 50만 건 인덱싱 | 39.5s |

ES의 bulk 인덱싱(5,000건 단위)이 MySQL 단건 INSERT 누적보다 5.7배 빠르다. ES는 세그먼트 단위로 데이터를 누적한 뒤 병합하는 구조로 대량 쓰기에 유리하다.

### 4-4. 데이터 규모별 성능 변화 추세

아래 표는 "나이키 운동화" 키워드 기준 비교다.

| 데이터 규모 | ES | MySQL LIKE | 개선율 |
|------------|-----|------------|--------|
| 10,000건 | 14ms | 9ms | 0.6배 (MySQL 우위) |
| 500,000건 | 43ms | 424ms | 9.9배 (ES 우위) |

10,000건에서는 MySQL이 더 빠를 수 있다. 그러나 데이터 규모가 50배 증가할 때 MySQL 응답 시간은 47배 증가하고 ES는 3배 증가한다. 이 추세로 보면 100만 건, 1,000만 건 규모에서는 ES의 우위가 더 극적으로 벌어진다.

소규모에서는 MySQL이 충분하지만, 데이터가 커질수록 ES의 역인덱스 구조가 압도적인 우위를 가진다.

---

## 5. 검색 품질 개선 결과

### 5-1. 한글 형태소 분석 (Nori)

Nori 형태소 분석기는 한국어 텍스트를 의미 단위 형태소로 분해한다.

**분석 예시**

```
인덱싱: "나이키 에어 운동화"
  Nori 토큰: ["나이키", "에어", "운동화"]
  역인덱스에 저장됨

검색어: "운동화를"
  Nori 토큰: ["운동화", "를"] --> 조사 "를" 제거 --> 검색 토큰: "운동화"
  역인덱스에서 "운동화" 토큰을 포함하는 문서 조회 --> 매칭됨
```

**MySQL이 0건인 이유**

`LIKE '%운동화를%'`는 "운동화를"이라는 완전한 부분 문자열을 찾는다. 상품명에는 "운동화를"이 없으므로 0건이 반환된다.

**500,000건 기준 형태소 검색 결과**

| 검색어 | ES 결과 | MySQL 결과 |
|--------|---------|------------|
| 운동화를 | 10,000건 (max_result_window 한도) | 0건 |
| 프리미엄의 | 10,000건 | 0건 |
| 러닝화에서 | 10,000건 | 0건 |
| 등산화는 | 10,000건 | 0건 |

참고: ES 결과가 10,000건으로 동일한 것은 `max_result_window: 10000` 설정으로 인한 totalHits 상한 때문이다. 실제 매칭 문서 수는 더 많다.

### 5-2. 멀티필드 검색

ES는 `name`, `brandName`, `categoryName` 세 필드를 동시에 검색한다. 기존 MySQL은 `name` 하나만 검색했다.

"아디다스"를 검색하면 MySQL은 상품명에 "아디다스"가 포함된 것만 반환했다. ES는 `brandName` 필드에서도 매칭하므로 상품명에 브랜드명이 없더라도 해당 브랜드 상품 전체를 찾는다.

"러닝"을 검색하면 `categoryName`이 "러닝화"인 상품도 Nori 형태소 분석("러닝화" → ["러닝", "화"])을 통해 매칭된다.

### 5-3. 관련도 점수 (BM25)

ES는 기본적으로 BM25(Best Match 25) 알고리즘으로 관련도 점수를 계산한다. MySQL에는 이 기능이 없다.

BM25가 반영하는 요소:
- **TF (Term Frequency)**: 문서에서 검색어가 자주 등장할수록 점수가 높다 (포화 효과 적용)
- **IDF (Inverse Document Frequency)**: 전체 문서에서 희귀한 단어일수록 점수가 높다
- **문서 길이 정규화**: 짧은 문서에서 매칭된 것이 긴 문서보다 더 관련성이 높다고 판단

"나이키 흰색 운동화"를 검색하면 세 단어 모두 포함된 상품이 상위에 노출되고, 한 단어만 포함된 상품은 하위에 위치한다. MySQL은 이 구분 없이 매칭 여부만 판단한다.

### 5-4. 신규 기능

**자동완성 (Edge N-gram)**

`name.autocomplete` 필드에 edge_ngram_analyzer를 적용하여 접두사 매칭을 지원한다. "나이"를 입력하면 "나이키 에어맥스", "나이키 덩크 로우" 등 "나이"로 시작하는 상품명이 반환된다.

**집계 (Faceted Search)**

브랜드별, 카테고리별, 가격대별 상품 수를 단일 쿼리로 집계한다. 가격 구간은 4개 고정 구간(~10,000 / 10,000~50,000 / 50,000~100,000 / 100,000~)으로 제공한다. 기존 MySQL 구조에서는 집계마다 별도 쿼리가 필요했다.

**재인덱싱 배치**

`POST /api/v1/admin/products/reindex` API로 MySQL 전체 상품을 ES에 재인덱싱한다. 1,000건 단위 bulk 처리, 부분 실패 시 계속 진행, 완료 후 인덱싱 건수 반환. ES와 MySQL 간 데이터 불일치가 발생했을 때 전체 재동기화 수단으로 활용한다.

---

## 6. 테스트 전략

### 6-1. 테스트 구성

| 테스트 클래스 | 유형 | 케이스 수 | 검증 대상 |
|--------------|------|----------|----------|
| `ProductSearchServiceIntegrationTest` | 통합 | 5건 | 형태소 검색, 멀티필드, 필터, match_all, 자동완성 |
| `ProductIndexerIntegrationTest` | 통합 | 3건 | 생성/수정/삭제 이벤트 → ES 동기화 |
| `ProductSearchServiceFallbackTest` | 통합 | Circuit Breaker | ES 장애 시 MySQL fallback |
| `ProductSearchV1ApiE2ETest` | E2E | 4건 | 자동완성 API, 집계 API, 유효성 검증 |
| `AdminProductV1ApiE2ETest` | E2E | 재인덱싱 | 재인덱싱 배치 API |
| `ElasticsearchPerformanceTest` | 성능/품질 | 3건 | 10,000건 성능 비교, 형태소 품질, 멀티필드 품질 |
| `ElasticsearchLargeScaleTest` | 성능/품질 | 4건 | 500,000건 성능, 형태소, 깊은 페이지네이션 |

### 6-2. 성능 테스트 방법론

- **측정 방식**: N회 반복 후 중간값(median) 사용. 평균값 대신 중간값을 사용하는 이유는 JIT 컴파일 준비 완료 후의 안정적인 응답 시간을 반영하기 위해서다.
- **소규모 테스트**: 3회 반복 (`MEDIAN_RUNS = 3`)
- **대규모 테스트**: 5회 반복 (`MEDIAN_RUNS = 5`)
- **JIT Warmup 고려**: 각 테스트 케이스 내에서 반복 측정하므로 첫 번째 실행의 JIT 비용이 중간값에서 자연스럽게 제외된다.
- **Circuit Breaker 초기화**: 각 테스트 `@BeforeEach`에서 `circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> cb.reset())`으로 이전 테스트의 실패 상태가 전파되지 않게 한다.
- **대규모 테스트 실행 조건**: `@EnabledIfSystemProperty(named = "test.large-scale", matches = "true")`로 기본 `./gradlew test`에서는 제외되고 명시적 활성화 시에만 실행된다.

---

## 7. 한계 및 향후 과제

### 7-1. 현재 한계

**미구현 항목 (PRD Could)**

- 오타 교정 (Fuzzy Query): 편집 거리 기반 오타 허용 미적용
- 동의어 사전: "운동화" ↔ "스니커즈" 동의어 매핑 미구현
- 인기 검색어 (Redis ZINCRBY): 검색 로그 집계 미구현
- 최근 검색어 (Redis List): 사용자별 검색 이력 미구현
- Zero-downtime 재인덱싱 (Alias 전략): 현재 재인덱싱 중 서비스 중단 가능

**알려진 제약**

- `max_result_window: 10000`으로 offset 페이지네이션 상한이 10,000건으로 제한된다. 이를 초과하는 페이지 요청은 400을 반환한다.
- `AFTER_COMMIT` 이벤트 방식은 애플리케이션 크래시 시 동기화 누락 가능성이 있다. 재인덱싱 배치로 주기적 재동기화가 필요하다.
- 현재 구현은 단일 노드 ES(`discovery.type=single-node`)로, 프로덕션 환경에서는 클러스터 구성이 필요하다.
- `xpack.security.enabled=false`는 로컬 환경 한정이며, 프로덕션에서는 보안 설정이 필수다.

### 7-2. 향후 개선 방향

| 항목 | 설명 | 우선순위 |
|------|------|---------|
| 검색어 하이라이팅 | 결과에서 매칭 부분을 `<em>` 태그로 강조 | P1 |
| Fuzzy Query | 오타 허용 (편집 거리 1~2) | P1 |
| 동의어 사전 | "운동화" ↔ "스니커즈" ↔ "sneakers" | P1 |
| Zero-downtime 재인덱싱 | Alias 전략으로 무중단 스키마 변경 | P1 |
| 인기 검색어 | 검색 로그 ES 집계 또는 Redis ZINCRBY | P2 |
| 최근 검색어 | Redis List로 사용자별 이력 관리 | P2 |
| `sort=relevance` | 관련도 점수 기반 정렬 옵션 추가 | P2 |
| ES 클러스터 구성 | 프로덕션 고가용성을 위한 다중 노드 설정 | P3 |

---

## 8. 결론

Phase 7에서 Elasticsearch 역인덱스 + Nori 형태소 분석기 도입으로 두 가지 근본적인 개선이 이루어졌다.

**성능**

| 지표 | 개선 결과 |
|------|----------|
| 키워드 검색 (500,000건) | 최대 26.5배 빠름 (울트라 부스트: 15ms vs 398ms) |
| 깊은 페이지 (500,000건) | 최대 26.3배 빠름 (page=0: 7ms vs 184ms) |
| 데이터 규모 증가 대응 | MySQL 47배 증가 vs ES 3배 증가 (50배 규모 증가 시) |
| 인덱싱 속도 | ES bulk 5.7배 빠름 (39.5s vs 227.2s) |

**검색 품질**

| 지표 | 개선 결과 |
|------|----------|
| 형태소 검색 | MySQL 0건 → ES 대량 매칭 (조사 결합 검색어) |
| 멀티필드 검색 | 상품명 단일 → 상품명 + 브랜드명 + 카테고리명 |
| 관련도 점수 | 없음 → BM25 적용 (복수 단어 검색 품질 향상) |
| 신규 기능 | 자동완성, 집계(faceted search), 재인덱싱 배치 추가 |

소규모(10,000건)에서는 ES와 MySQL의 성능 차이가 작지만, 데이터 규모가 커질수록 역인덱스 구조의 우위가 압도적으로 커진다. 검색 품질 측면에서는 데이터 규모와 관계없이 형태소 분석과 멀티필드 검색이 즉각적인 개선을 제공한다.
