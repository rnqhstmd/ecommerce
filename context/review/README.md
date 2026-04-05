# review

구매 확인 후 작성 가능한 상품 리뷰를 관리하는 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Review | order, productId, userId, rating, content | 상품 리뷰. rating(1~5), content(500자 이하). (orderId, productId) 유니크 제약 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/reviews | 리뷰 작성 (구매 확인 후) | USER |
| GET | /api/v1/products/{productId}/reviews | 상품 리뷰 목록 (페이징 + 평균 평점) | 불필요 |
| GET | /api/v1/products/{productId}/reviews/cursor | 상품 리뷰 목록 (커서 페이징) | 불필요 |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Phase 4 | 리뷰 도메인, 구매 확인 후 작성, rating + content |
| Phase 5 | Cursor-based Pagination |

## 도메인 관계

- **order**: 리뷰 작성 시 주문 확인 (구매한 상품만 리뷰 가능). Order와 ManyToOne 관계
- **product**: productId FK. 상품별 리뷰 목록 + 평균 평점
- **user**: userId 참조
