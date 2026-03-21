# ecommerce 구현 추적

> PRD 요구사항별 구현 상태를 추적합니다.

- 수정일: 2026-03-21

## 범례

- ✅ 반영됨 — 코드에 구현 완료
- ⬜ 미반영 — 정책/설계만 확정, 코드 미구현

## Week 2 요구사항

| 요구사항 | 상태 | 비고 |
|----------|------|------|
| 사용자 등록 | ✅ | POST /api/v1/users, E2E 테스트 |
| 포인트 조회/충전 | ✅ | GET/POST /api/v1/points, 동시성 테스트 포함 |
| 브랜드/상품 조회 (필터/정렬) | ✅ | BrandV1Controller, ProductV1Controller, ProductSearchCondition, E2E 테스트 |
| 좋아요 추가/제거/목록 | ✅ | LikeV1Controller, LikeService, 유니크 제약 테스트, E2E 테스트 |
| 주문/결제 처리 | ✅ | OrderV1Controller, OrderService, StockDeductionService, OrderPlacedEvent, E2E 테스트 |

---

## Phase 1: 미노출 API 연결 + Week 2 완성

> Facade에 이미 구현되어 있으나 Controller에 노출되지 않은 기능 + Week 2 요구사항 누락분

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| GET /api/v1/orders — 내 주문 목록 | ⬜ | `orderFacade.getMyOrders()` Facade 구현 완료, Controller 미연결 |
| GET /api/v1/orders/{id} — 주문 상세 | ⬜ | `orderFacade.getOrderDetail()` Facade 구현 완료, Controller 미연결 |
| GET /api/v1/products — 상품 목록 (필터/정렬/페이징) | ⬜ | `productFacade.getProducts()` Facade 구현 완료, Controller 미연결. brandId 필터 + latest/price_asc/likes_desc 정렬 |
| GET /api/v1/products/{id} + isLiked | ⬜ | X-USER-ID 헤더 시 좋아요 여부 포함 응답. Week 2 요구사항 명시 |
| GET /api/v1/brands/{id} — 브랜드 단건 조회 | ⬜ | `brandFacade.getBrand()` Facade 구현 완료, Controller 미연결 |
| GET /api/v1/likes — 내 좋아요 목록 | ⬜ | LikeRepository에 userId 기반 페이징 조회 추가 필요 |

**학습 포인트**: 페이징 응답 설계, 조건부 응답 필드 (isLiked)

---

## Phase 2: 주문 생명주기 + 포인트 이력

> 보상 트랜잭션, 이력 관리, 검색 조건 확장

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| POST /api/v1/orders/{id}/cancel — 주문 취소 | ⬜ | OrderStatus에 CANCELLED 추가. 재고 복구 + 포인트 환불 보상 트랜잭션 |
| OrderCancelledEvent 발행 | ⬜ | 취소 시 Kafka 이벤트. commerce-streamer에서 소비 |
| GET /api/v1/points/history — 포인트 이력 | ⬜ | PointHistory 엔티티 신규. 충전/사용/환불 타입별 이력 기록 |
| 상품 검색 조건 확장 | ⬜ | ProductSearchCondition에 keyword, minPrice, maxPrice 추가. QueryDSL 동적 쿼리 |
| GET /api/v1/orders 필터링/페이징 | ⬜ | status 필터 + 페이징. OrderSearchCondition 추가 |

**학습 포인트**: 보상 트랜잭션 패턴, 도메인 이벤트 확장, QueryDSL 동적 쿼리, Audit Trail

---

## Phase 3: 장바구니 + 상품 관리 확장

> Redis 활용 심화, 상품 CRUD 완성, 인기 상품 캐싱

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| Cart 도메인 (장바구니) | ⬜ | Redis Hash 기반 임시 저장. CartItem(productId, quantity). TTL 7일 |
| POST /api/v1/cart/items — 장바구니 추가 | ⬜ | 재고 사전 검증은 주문 시점에만 수행 |
| DELETE /api/v1/cart/items/{productId} — 장바구니 삭제 | ⬜ | |
| GET /api/v1/cart — 장바구니 조회 | ⬜ | 상품 정보 조인하여 반환 (이름, 가격, 재고 상태) |
| POST /api/v1/cart/checkout — 장바구니 → 주문 전환 | ⬜ | 기존 placeOrder 플로우 재활용 |
| PUT /api/v1/products/{id} — 상품 수정 | ⬜ | 이름, 가격 변경. 캐시 무효화 |
| POST /api/v1/products/{id}/stock — 재고 입고 | ⬜ | Stock.increase() 추가. 비관적 락 |
| GET /api/v1/brands — 브랜드 목록 | ⬜ | 페이징 |
| GET /api/v1/products/popular — 인기 상품 TOP N | ⬜ | Redis Sorted Set 기반. 좋아요 수 캐싱 |

**학습 포인트**: Redis Hash/Sorted Set, 캐시 무효화 전략, 임시 데이터 관리

---

## Phase 4: 리뷰 + 쿠폰 + 알림

> 새 도메인 추가, Kafka consumer 활용, 복합 할인 로직

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| Review 도메인 (리뷰) | ⬜ | 구매 확인 후 작성 (Order PAID 상태 검증). rating(1~5) + content |
| POST /api/v1/reviews — 리뷰 작성 | ⬜ | 주문당 1회. 중복 방지 유니크 제약 (orderId + productId) |
| GET /api/v1/products/{id}/reviews — 상품 리뷰 목록 | ⬜ | 페이징 + 평균 평점 |
| Coupon 도메인 (쿠폰) | ⬜ | CouponPolicy(할인율/금액, 유효기간, 발급수량) + UserCoupon(발급 이력) |
| POST /api/v1/coupons/{id}/issue — 쿠폰 발급 | ⬜ | 선착순 발급. Redis 기반 수량 제어 |
| 주문 시 쿠폰 적용 | ⬜ | OrderFacade.placeOrder에 couponId 파라미터 추가. 할인 계산 로직 |
| Notification 도메인 (알림) | ⬜ | commerce-streamer에서 OrderPlacedEvent/OrderCancelledEvent 소비 |
| 알림 발송 (이메일/푸시 stub) | ⬜ | NotificationService 인터페이스 + Stub 구현. 향후 실제 연동 |
| Category 도메인 (카테고리) | ⬜ | 계층형 카테고리 (parentId self-reference). Product에 categoryId 추가 |

**학습 포인트**: 선착순 동시성 제어 (Redis DECR), Kafka consumer 실전, 계층형 데이터 모델링, 전략 패턴 (할인 계산)
