# coupon

쿠폰 정책 관리와 선착순 발급, 주문 시 할인 적용을 담당하는 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| CouponPolicy | name, discountType, discountValue, validFrom, validTo, totalQuantity, issuedQuantity | 쿠폰 정책. 할인율/금액, 유효기간, 발급수량 한도 |
| UserCoupon | userId, couponPolicyId, issuedAt, usedAt, used | 사용자별 쿠폰 발급 이력. (userId, couponPolicyId) 유니크 제약 |
| DiscountType | RATE, AMOUNT | 할인 타입 열거형 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/coupons/{id}/issue | 쿠폰 발급 (선착순 동시성 제어) | USER |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Phase 4 | CouponPolicy + UserCoupon 도메인, Redis DECR 원자적 수량 제어 |
| Phase 5 | Redisson 분산 락 적용 |

## 도메인 관계

- **order**: 주문 생성 시 쿠폰 적용 (할인 계산). 주문 취소 시 미사용 처리
- **user**: 발급 시 userId 참조
