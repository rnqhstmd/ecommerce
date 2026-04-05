# cart

Redis Hash 기반 장바구니. 상품 추가/삭제, 조회, 주문 전환을 담당.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| CartItem | productId, quantity | 장바구니 항목 (record). Redis Hash에 저장, TTL 7일 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/cart/items | 장바구니에 상품 추가 (HINCRBY 원자적 수량 추가) | USER |
| DELETE | /api/v1/cart/items/{productId} | 장바구니에서 상품 제거 | USER |
| GET | /api/v1/cart | 장바구니 조회 (상품 정보 조인) | USER |
| POST | /api/v1/cart/checkout | 장바구니 → 주문 전환 (기존 placeOrder 플로우 재활용) | USER |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Phase 3 | Redis Hash 기반 장바구니, checkout → 주문 전환 |

## 도메인 관계

- **product**: 장바구니 조회 시 상품 정보(이름, 가격, 재고 상태) 조인
- **order**: checkout 시 주문 생성 (placeOrder 플로우 재활용)
