# notification

주문 이벤트를 Kafka로 수신하여 알림을 생성하는 도메인. **commerce-streamer** 앱에서 동작 (commerce-api 아님).

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Notification | userId, type, title, content, readAt | 알림. Kafka consumer가 생성 |
| NotificationType | ORDER_PLACED, ORDER_CANCELLED | 알림 타입 열거형 |

## API 엔드포인트

> REST API 없음. Kafka consumer로 이벤트를 수신하여 알림 생성.

| Consumer | Topic | 설명 |
|----------|-------|------|
| OrderEventConsumer | order 이벤트 | OrderPlacedEvent / OrderCancelledEvent 수신 → Notification 생성 |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Phase 4 | Kafka consumer 이벤트 소비, StubNotificationService |

## 도메인 관계

- **order**: OrderPlacedEvent / OrderCancelledEvent를 Kafka로 수신
- **user**: 알림 대상 userId 참조
