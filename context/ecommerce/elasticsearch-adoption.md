# Elasticsearch 도입 배경 및 예상 개선 사항

- 작성일: 2026-03-29
- 관련 레포: rnqhstmd/ecommerce

## 1. 현재 검색 구현 현황

### 1-1. 상품 검색 (ProductQueryRepository)

현재 상품 검색은 QueryDSL + MySQL `LIKE` 쿼리 기반이다.

```java
// ProductQueryRepository.java
builder.and(product.name.containsIgnoreCase(condition.keyword().trim()));
```

| 항목 | 현재 상태 |
|------|----------|
| 검색 대상 필드 | `name` 1개 (상품명) |
| 매칭 방식 | `LIKE '%keyword%'` (substring, 대소문자 무시) |
| 한글 형태소 분석 | 없음 |
| 검색 결과 정렬 | 고정 4종 (`createdAt`, `price.value`, `likeCount`, `id`) |
| 관련도 점수 | 없음 — 매칭 여부만 판단 (true/false) |
| 자동완성 | 없음 |
| 오타 교정 | 없음 |
| 필터 조건 | `brandId`, `minPrice`, `maxPrice` |
| 페이지네이션 | offset 기반 (별도 COUNT 쿼리 실행) |

### 1-2. 기타 검색

| 도메인 | 검색 방식 | 비고 |
|--------|----------|------|
| 주문 (Order) | JPA `userId + status` 필터, Cursor 페이지네이션 | 단순 필터링 |
| 리뷰 (Review) | JPA `productId` 기반, Cursor 페이지네이션 | 단순 필터링 |
| 브랜드 (Brand) | JPA `findAll` + Spring Cache (`brands`, 1h TTL) | 페이징만 |
| 인기 상품 | Redis Sorted Set (likeCount 기준 TOP N) | 캐시 1h |

---

## 2. 현재 방식의 한계

### 2-1. 검색 품질

| 문제 | 설명 | 예시 |
|------|------|------|
| **부분 문자열 매칭만 가능** | `LIKE '%나이키%'`는 "나이키 에어맥스"를 찾지만, "나이키" 입력으로 "Nike Air Max"를 찾을 수 없다 | 동의어/다국어 매칭 불가 |
| **형태소 분석 부재** | "운동화를"로 검색하면 "운동화"를 못 찾는다. MySQL은 한글 형태소 분석을 지원하지 않는다 | 조사 결합 시 검색 실패 |
| **관련도 점수 없음** | "나이키 흰색 운동화" 검색 시 3개 단어 모두 포함된 상품과 1개만 포함된 상품을 구분할 수 없다 | 검색 결과 품질 저하 |
| **오타 허용 불가** | "나이키" → "나이케"로 오타 시 결과 0건 | 사용자 경험 저하 |
| **자동완성 없음** | 검색어 입력 중 추천이 불가능하다 | 검색 전환율 하락 |
| **단일 필드 검색** | 상품명만 검색 가능. 브랜드명, 카테고리명, 설명 등을 동시에 검색할 수 없다 | 검색 범위 제한 |

### 2-2. 성능

| 문제 | 설명 |
|------|------|
| **LIKE '%keyword%' 풀 테이블 스캔** | 앞에 와일드카드가 있으면 B-Tree 인덱스를 탈 수 없다. 상품 수가 증가하면 선형으로 느려진다 |
| **COUNT 쿼리 이중 실행** | offset 페이지네이션에서 content 쿼리와 count 쿼리를 별도로 실행한다 |
| **정렬 부하** | 대량 데이터에서 `ORDER BY likeCount DESC` + `LIMIT` 조합은 filesort를 유발할 수 있다 |
| **DB 부하 집중** | 검색 트래픽이 트랜잭션 처리와 같은 MySQL 인스턴스를 사용한다 |

---

## 3. Elasticsearch 도입 목표

### 3-1. 검색 품질 개선

| 개선 항목 | AS-IS | TO-BE |
|----------|-------|-------|
| 검색 대상 | `name` 1개 필드 | `name` + `description` + `brandName` + `categoryName` 다중 필드 |
| 한글 처리 | 없음 | Nori 형태소 분석기 (은전한닢 기반) |
| 매칭 방식 | substring (`LIKE`) | 형태소 토큰 매칭 + BM25 관련도 점수 |
| 오타 교정 | 없음 | Fuzzy Query (편집 거리 기반) |
| 자동완성 | 없음 | Completion Suggester (Edge N-gram) |
| 동의어 | 없음 | Synonym Filter ("운동화" ↔ "스니커즈" ↔ "sneakers") |
| 검색 결과 정렬 | 고정 4종 | 관련도 점수 + 필드 정렬 조합 (function_score) |

### 3-2. 성능 개선

| 개선 항목 | AS-IS | TO-BE |
|----------|-------|-------|
| 인덱스 구조 | B-Tree (LIKE '%x%' 사용 불가) | 역인덱스 (Inverted Index) — 토큰 기반 O(1) 조회 |
| 검색 응답 시간 | 상품 10만 건 기준 ~200ms+ (풀스캔) | 상품 10만 건 기준 ~10-20ms (역인덱스) |
| DB 부하 | 검색 + 트랜잭션 동일 MySQL | 검색 트래픽을 ES로 분리, MySQL은 트랜잭션 전용 |
| COUNT 성능 | 매 요청마다 COUNT 쿼리 | `track_total_hits` 옵션으로 제어 가능 |
| 캐싱 | 없음 (검색 결과) | ES Request Cache + Filter Cache 자동 적용 |

### 3-3. 기능 확장

| 신규 기능 | 설명 |
|----------|------|
| **다중 필드 검색** | 상품명, 브랜드명, 카테고리명, 설명을 한 번에 검색 (`multi_match`) |
| **자동완성** | Completion Suggester로 검색어 입력 중 실시간 추천 |
| **검색어 하이라이팅** | 검색 결과에서 매칭된 부분을 `<em>` 태그로 강조 |
| **집계(Aggregation)** | 브랜드별/카테고리별/가격대별 상품 수 집계 (faceted search) |
| **최근 검색어** | ES + Redis 조합으로 사용자별 검색 이력 관리 |
| **인기 검색어** | ES 검색 로그 집계로 실시간 인기 검색어 |

---

## 4. 아키텍처 변경 사항

### 4-1. 현재 구조

```
Client → Controller → Facade → ProductService → ProductQueryRepository → MySQL
```

### 4-2. 도입 후 구조

```
[검색 요청]
Client → Controller → Facade → ProductSearchService → Elasticsearch
                                    ↓ (ID 목록)
                              ProductService → MySQL (상세 정보)

[CUD 요청]
Client → Controller → Facade → ProductService → MySQL
                                    ↓ (이벤트)
                              ProductIndexer → Elasticsearch (인덱스 동기화)
```

### 4-3. 데이터 동기화 전략

| 전략 | 장점 | 단점 | 적합 시나리오 |
|------|------|------|-------------|
| **Application Event** | 구현 단순, 트랜잭션 내 동기화 | 동기화 실패 시 불일치 가능 | 현재 프로젝트 (단일 앱) |
| **Kafka CDC** | 비동기, 느슨한 결합 | 지연 발생, 인프라 복잡 | MSA 환경 |
| **Debezium** | MySQL binlog 기반, 코드 변경 없음 | 운영 복잡도 높음 | 대규모 시스템 |

현재 프로젝트는 **Application Event 방식**이 적합하다. 기존 `@TransactionalEventListener(AFTER_COMMIT)` 패턴을 재활용할 수 있다.

---

## 5. 기술 스택

| 컴포넌트 | 기술 | 버전 | 비고 |
|----------|------|------|------|
| Elasticsearch | Elasticsearch OSS | 8.x | Docker Compose로 로컬 실행 |
| 한글 분석기 | Nori Plugin | ES 버전 매칭 | `analysis-nori` 플러그인 |
| Java 클라이언트 | Spring Data Elasticsearch | 5.x | Spring Boot 3.4.4 호환 |
| 테스트 | Testcontainers | elasticsearch 모듈 | 기존 Testcontainers 패턴 재활용 |

---

## 6. ES 인덱스 설계 (초안)

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["nori_readingform", "lowercase"]
        },
        "autocomplete": {
          "type": "custom",
          "tokenizer": "edge_ngram_tokenizer",
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
                        "fields": { "autocomplete": { "type": "text", "analyzer": "autocomplete" } } },
      "brandId":      { "type": "long" },
      "brandName":    { "type": "text", "analyzer": "korean",
                        "fields": { "keyword": { "type": "keyword" } } },
      "categoryId":   { "type": "long" },
      "categoryName": { "type": "text", "analyzer": "korean",
                        "fields": { "keyword": { "type": "keyword" } } },
      "price":        { "type": "long" },
      "likeCount":    { "type": "integer" },
      "createdAt":    { "type": "date" },
      "suggest":      { "type": "completion", "analyzer": "korean" }
    }
  }
}
```

---

## 7. 예상 구현 범위

| 항목 | 우선순위 | 난이도 |
|------|---------|--------|
| ES 인프라 구성 (Docker Compose + Nori) | P0 | 낮음 |
| Product 인덱스 설계 + 매핑 | P0 | 중간 |
| ProductIndexer (MySQL → ES 동기화) | P0 | 중간 |
| 상품 검색 API (multi_match + 필터) | P0 | 중간 |
| 자동완성 API (Completion Suggester) | P1 | 중간 |
| 검색어 하이라이팅 | P1 | 낮음 |
| 집계 (브랜드별/카테고리별 facet) | P1 | 중간 |
| 오타 교정 (Fuzzy Query) | P2 | 낮음 |
| 동의어 사전 | P2 | 낮음 |
| 인기 검색어 / 최근 검색어 | P2 | 중간 |
| 전체 데이터 재인덱싱 배치 | P2 | 중간 |

---

## 8. 리스크

| 리스크 | 대응 방안 |
|--------|----------|
| MySQL ↔ ES 데이터 불일치 | Application Event 실패 시 재시도 큐 + 주기적 전체 재인덱싱 배치 |
| ES 장애 시 검색 불가 | Resilience4j Circuit Breaker + MySQL LIKE fallback |
| Nori 형태소 분석 품질 | 사용자 사전 (user_dictionary) 커스터마이징 |
| 인덱스 스키마 변경 | Zero-downtime reindexing (alias 전략) |
