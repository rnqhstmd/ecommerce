# point

포인트 잔액 관리와 충전/사용/환불 이력 추적을 담당하는 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Point | userId, balance | 사용자별 포인트 잔액. userId 유니크 제약 |
| PointBalance | value | 잔액 Value Object |
| PointHistory | point, type, amount, balanceAfter | 포인트 변동 이력 (CHARGE/USE/REFUND) |
| PointHistoryType | CHARGE, USE, REFUND | 이력 타입 열거형 |
| PointInitializationEventListener | - | UserSignedUpEvent 수신하여 포인트 자동 생성 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/v1/points | 내 포인트 잔액 조회 | USER |
| POST | /api/v1/points/charge | 포인트 충전 | USER |
| GET | /api/v1/points/history | 포인트 이력 조회 (페이징) | USER |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Week 2 | 포인트 조회/충전, 동시성 테스트 |
| Phase 2 | PointHistory 엔티티, 충전/사용/환불 이력 기록 |

## 도메인 관계

- **user**: UserSignedUpEvent로 포인트 자동 생성
- **order**: 주문 시 포인트 사용. 취소 시 환불
