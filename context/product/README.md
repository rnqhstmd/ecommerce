# product

상품 등록, 수정, 조회, 검색, 자동완성, 집계를 담당하는 핵심 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Product | name, price, stock, brandId, categoryId, likeCount | 상품. 가격/재고는 Value Object |
| ProductPrice | value | 가격 Value Object (1 이상) |
| Stock | value | 재고 Value Object. 비관적 락 기반 차감 |
| ProductSearchCondition | keyword, brandId, minPrice, maxPrice | 검색 조건 DTO |
| ProductDocument | ES 인덱스 문서 | MySQL Product를 ES에 동기화한 검색 전용 문서 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/products | 상품 등록 | ADMIN |
| GET | /api/v1/products/{id} | 상품 상세 (isLiked 포함) | 불필요 |
| PUT | /api/v1/products/{id} | 상품 수정 (이름, 가격) | ADMIN |
| POST | /api/v1/products/{id}/stock | 재고 입고 | ADMIN |
| GET | /api/v1/products | 상품 목록 (필터/정렬/페이징) | 불필요 |
| GET | /api/v1/products/popular | 인기 상품 TOP N | 불필요 |
| GET | /api/v1/products/search/autocomplete | 자동완성 | 불필요 |
| GET | /api/v1/products/search/facets | 집계 (브랜드/카테고리/가격대) | 불필요 |
| POST | /api/v1/admin/products/reindex | 전체 재인덱싱 | ADMIN |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Week 2 | 상품 조회, 필터/정렬 |
| Phase 1 | 상품 목록 페이징, isLiked 조건부 필드 |
| Phase 2 | keyword/minPrice/maxPrice 검색 조건 확장 |
| Phase 3 | 상품 수정, 재고 입고, 인기 상품 (Redis Sorted Set) |
| Phase 7 | Elasticsearch 검색, 자동완성, Faceted Search, 재인덱싱 |

## 도메인 관계

- **brand**: brandId FK. 상품은 반드시 하나의 브랜드에 속함
- **category**: categoryId FK (nullable). 상품은 하나의 카테고리에 속할 수 있음
- **like**: likeCount 비정규화. 좋아요 추가/제거 시 증감
- **order**: 주문 시 재고 차감 (StockDeductionService, 비관적 락)
- **review**: 상품별 리뷰 목록
- **cart**: 장바구니에 상품 추가
