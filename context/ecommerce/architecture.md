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
  MySQL 8.0 ─── Redis Master/Replica ─── Kafka (KRaft) ─── Prometheus/Grafana ─── Elasticsearch 8.x (Nori)
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
  │        │                        │
  │        └── OrderCancelledEvent   ├── Category (계층형)
  │                                  └── Review
  ├── Point (1:1) ──< PointHistory
  ├── Like >── Product
  ├── Cart (Redis Hash) ──< CartItem
  └── UserCoupon >── CouponPolicy

Notification ← Kafka consumer (OrderPlaced/OrderCancelled)
```

## 구현 완료 컴포넌트

```
Phase 1 — API 연결:
  ProductV1Controller ← GET /products (목록, isLiked), GET /products/{id}
  OrderV1Controller   ← GET /orders, GET /orders/{id}
  BrandV1Controller   ← GET /brands/{id}
  LikeV1Controller    ← GET /likes (내 좋아요 목록)

Phase 2 — 주문 생명주기:
  Order (CANCELLED 상태) ── OrderCancelledEvent → Kafka
  PointHistory (CHARGE/USE/REFUND) ── 포인트 이력 자동 저장
  ProductQueryRepository ── QueryDSL 동적 쿼리 (keyword, minPrice, maxPrice)

Phase 3 — 장바구니 + 상품 관리:
  Cart (Redis Hash, TTL 7d) ── HINCRBY 원자적 수량 추가 → checkout → placeOrder
  Product ── PUT 수정 (캐시 무효화), POST stock 입고 (비관적 락)
  PopularProductService ── Redis Sorted Set 인기 상품 TOP N

Phase 4 — 신규 도메인:
  Review ── rating(1~5) + content, 구매 확인 후 1회 작성
  CouponPolicy + UserCoupon ── Redis DECR 선착순 발급, 주문 시 할인 적용
  Category ── 계층형 (parentId self-reference)
  Notification ── commerce-streamer Kafka consumer → StubNotificationService

Phase 5 — 운영 안정성:
  DistributedLockService ── Redisson 분산 락 (쿠폰 발급 적용)
  RateLimitInterceptor ── Redis Sliding Window (분당 60회)
  RedisCacheManager ── Spring Cache 체계화 (brands 1h TTL)
  CursorPageResponse ── Cursor-based Pagination (주문/리뷰)
  Resilience4j ── Kafka @CircuitBreaker + Redis @Retry
  RedisHealthIndicator ── Graceful Shutdown 헬스체크

Phase 6 — 인증/인가:
  AuthV1Controller ── signup/login/refresh (BCrypt + JWT)
  JwtTokenProvider ── Access 30분 + Refresh 7일 (Redis)
  JwtAuthenticationFilter ── Bearer 토큰 → SecurityContext
  SecurityConfig ── RBAC (USER/ADMIN), 엔드포인트별 권한 설정
  SecurityContextHelper ── 기존 X-USER-ID 전면 대체

Phase 7 — 검색 + 개인화 (예정):
  Elasticsearch 8.x ── Nori 한글 형태소 분석기 + 역인덱스
  ProductSearchService ── ES multi_match 검색 (name, brandName, categoryName)
  ProductIndexer ── Application Event 기반 MySQL → ES 동기화
  Completion Suggester ── Edge N-gram 자동완성
  Aggregation ── 브랜드별/카테고리별 faceted search
  Circuit Breaker ── ES 장애 시 MySQL LIKE fallback
```

## 주제 문서

| 주제 | 설명 |
|------|------|
| [Elasticsearch 도입](elasticsearch-adoption.md) | 도입 배경, 현재 검색 한계, 예상 개선 사항, 인덱스 설계 초안 |
