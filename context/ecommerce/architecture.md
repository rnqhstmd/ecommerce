# ecommerce 아키텍처

> 전체 구조 요약과 주제별 상세 문서 링크를 관리합니다.

## 시스템 구조

```
┌─────────────────────────────────────────────────┐
│                    apps/                         │
│  ┌──────────────┐     ┌───────────────────┐     │
│  │ commerce-api │     │ commerce-streamer  │     │
│  │  (REST API)  │     │  (Kafka Consumer)  │     │
│  └──────┬───────┘     └────────┬──────────┘     │
├─────────┼──────────────────────┼────────────────┤
│         │       modules/       │                 │
│  ┌──────┴───┐  ┌───────┐  ┌───┴────┐           │
│  │   jpa    │  │ redis  │  │ kafka  │           │
│  │ QueryDSL │  │ master │  │ batch  │           │
│  │  MySQL   │  │ replica│  │ manual │           │
│  └──────────┘  └────────┘  └────────┘           │
├─────────────────────────────────────────────────┤
│                  supports/                       │
│  ┌─────────┐  ┌─────────┐  ┌────────────┐      │
│  │ jackson  │  │ logging │  │ monitoring │      │
│  └─────────┘  └─────────┘  └────────────┘      │
└─────────────────────────────────────────────────┘

외부 인프라 (Docker Compose):
  MySQL 8.0 ─── Redis Master/Replica ─── Kafka (KRaft) ─── Prometheus/Grafana
```

## 레이어 구조 (Clean Architecture)

```
Interface Layer  → Controller, DTO, ApiSpec, ApiControllerAdvice
Application Layer → Facade, Command, Info
Domain Layer     → Entity, ValueObject, Service, Repository (interface)
Infrastructure   → RepositoryImpl, JpaRepository, Config
```

## 도메인 관계

```
User ──< Order ──< OrderItem >── Product >── Brand
User ──< Point (1:1)
User ──< Like >── Product
```

## 확장 예정 컴포넌트

```
Phase 1 (미노출 API):
  ProductV1Controller ← GET /products (목록), isLiked 응답 확장
  OrderV1Controller   ← GET /orders, GET /orders/{id}
  BrandV1Controller   ← GET /brands/{id}
  LikeV1Controller    ← GET /likes (내 좋아요 목록)

Phase 2 (주문 생명주기):
  Order (CANCELLED 상태) ── OrderCancelledEvent → Kafka
  PointHistory (신규 엔티티) ── 충전/사용/환불 이력
  ProductSearchCondition ── keyword, minPrice, maxPrice 확장

Phase 3 (장바구니 + 상품 관리):
  Cart (Redis Hash) ── CartItem → 주문 전환
  Product ── PUT 수정, POST stock 입고
  Redis Sorted Set ── 인기 상품 TOP N

Phase 4 (신규 도메인):
  Review ── rating + content, 구매 확인 후 작성
  Coupon ── CouponPolicy + UserCoupon, 선착순 발급 (Redis DECR)
  Category ── 계층형 (parentId self-reference)
  Notification ── commerce-streamer에서 이벤트 소비 → 알림 발송
```

## 주제 문서

| 주제 | 설명 |
|------|------|
