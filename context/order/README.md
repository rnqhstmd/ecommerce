# order

주문 생성, 결제, 취소 등 주문 생명주기를 관리하는 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Order | userId, totalAmount, status, discountAmount, userCouponId, paidAt, cancelledAt | 주문. 상태 머신(PENDING → PAID → CANCELLED) |
| OrderItem | order, productId, productName, quantity, orderItemPrice | 주문 항목. Order와 1:N 관계 |
| OrderStatus | PENDING, PAID, CANCELLED | 주문 상태 열거형 |
| OrderTotalAmount | value | 주문 총액 Value Object |
| OrderItemPrice | value | 주문 항목 단가 Value Object |
| OrderSearchCondition | - | 주문 검색 조건 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/orders | 주문 생성 (재고 차감 + 포인트 사용 + 쿠폰 적용) | USER |
| GET | /api/v1/orders | 내 주문 목록 (status 필터, 페이징) | USER |
| GET | /api/v1/orders/cursor | 내 주문 목록 (커서 페이징) | USER |
| GET | /api/v1/orders/{id} | 주문 상세 | USER |
| POST | /api/v1/orders/{id}/cancel | 주문 취소 (보상 트랜잭션) | USER |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Week 2 | 주문/결제 처리 |
| Phase 1 | 주문 목록/상세 API 연결 |
| Phase 2 | 주문 취소, 보상 트랜잭션, status 필터링 |
| Phase 4 | 주문 시 쿠폰 적용 (discountAmount, userCouponId) |
| Phase 5 | Cursor-based Pagination |

## 도메인 관계

- **product**: 주문 시 StockDeductionService로 재고 차감. 취소 시 복구
- **point**: 주문 시 포인트 사용. 취소 시 환불
- **coupon**: 주문 시 쿠폰 적용. 취소 시 쿠폰 미사용 처리
- **cart**: 장바구니 checkout 시 주문 생성
- **notification**: OrderPlacedEvent / OrderCancelledEvent → Kafka → commerce-streamer 알림
