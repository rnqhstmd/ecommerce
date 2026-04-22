# ecommerce 구현 추적

> PRD 요구사항별 구현 상태를 추적합니다.

- 수정일: 2026-04-22

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

> PR #5 머지 (2026-03-21)

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| GET /api/v1/orders — 내 주문 목록 | ✅ | OrderV1Controller 연결, E2E 테스트 |
| GET /api/v1/orders/{id} — 주문 상세 | ✅ | OrderV1Controller 연결, E2E 테스트 |
| GET /api/v1/products — 상품 목록 (필터/정렬/페이징) | ✅ | brandId 필터 + latest/price_asc/likes_desc 정렬 |
| GET /api/v1/products/{id} + isLiked | ✅ | X-USER-ID 헤더 시 좋아요 여부 포함 응답 |
| GET /api/v1/brands/{id} — 브랜드 단건 조회 | ✅ | BrandV1Controller 연결 |
| GET /api/v1/likes — 내 좋아요 목록 | ✅ | LikeRepository userId 기반 페이징 조회 |

**학습 포인트**: 페이징 응답 설계, 조건부 응답 필드 (isLiked), likeCount 비정규화 + 캐시 무효화

---

## Phase 2: 주문 생명주기 + 포인트 이력

> PR #7 머지 (2026-03-21)

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| POST /api/v1/orders/{id}/cancel — 주문 취소 | ✅ | 보상 트랜잭션 (재고 복구 + 포인트 환불) |
| OrderCancelledEvent 발행 | ✅ | Kafka AFTER_COMMIT, 실패 격리 |
| GET /api/v1/points/history — 포인트 이력 | ✅ | PointHistory 엔티티 신규 (CHARGE/USE/REFUND) |
| 상품 검색 조건 확장 | ✅ | keyword, minPrice, maxPrice — QueryDSL 동적 쿼리 |
| GET /api/v1/orders 필터링/페이징 | ✅ | status 필터 + PageResponse |

**학습 포인트**: 보상 트랜잭션 패턴, 도메인 이벤트 확장, QueryDSL 동적 쿼리, Audit Trail

---

## Phase 3: 장바구니 + 상품 관리 확장

> PR #8 머지 (2026-03-21)

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| Cart 도메인 (장바구니) | ✅ | Redis Hash 기반, TTL 7일, HINCRBY 원자적 수량 추가 |
| POST /api/v1/cart/items — 장바구니 추가 | ✅ | CartV1Controller |
| DELETE /api/v1/cart/items/{productId} — 장바구니 삭제 | ✅ | CartV1Controller |
| GET /api/v1/cart — 장바구니 조회 | ✅ | 상품 정보 조인 반환 (이름, 가격, 재고 상태) |
| POST /api/v1/cart/checkout — 장바구니 → 주문 전환 | ✅ | 기존 placeOrder 플로우 재활용 |
| PUT /api/v1/products/{id} — 상품 수정 | ✅ | 이름, 가격 변경 + 캐시 무효화 |
| POST /api/v1/products/{id}/stock — 재고 입고 | ✅ | Stock.increase() + 비관적 락 |
| GET /api/v1/brands — 브랜드 목록 | ✅ | 페이징 |
| GET /api/v1/products/popular — 인기 상품 TOP N | ✅ | Redis Sorted Set 캐싱 |

**학습 포인트**: Redis Hash/Sorted Set, 캐시 무효화 전략, 임시 데이터 관리

---

## Phase 4: 리뷰 + 쿠폰 + 알림 + 카테고리

> PR #9 머지 (2026-03-21)

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| Review 도메인 (리뷰) | ✅ | 구매 확인 후 작성, rating(1~5), 주문당 1회 유니크 제약 |
| POST /api/v1/reviews — 리뷰 작성 | ✅ | ReviewV1Controller |
| GET /api/v1/products/{id}/reviews — 상품 리뷰 목록 | ✅ | 페이징 + 평균 평점 |
| Coupon 도메인 (쿠폰) | ✅ | CouponPolicy + UserCoupon, 할인율/금액, 유효기간, 발급수량 |
| POST /api/v1/coupons/{id}/issue — 쿠폰 발급 | ✅ | Redis DECR 원자적 수량 제어 |
| 주문 시 쿠폰 적용 | ✅ | OrderPlaceCommand에 couponId 추가, 할인 계산 |
| Notification 도메인 (알림) | ✅ | commerce-streamer Kafka consumer 이벤트 소비 |
| 알림 발송 (이메일/푸시 stub) | ✅ | NotificationService interface + StubNotificationService |
| Category 도메인 (카테고리) | ✅ | 계층형 (parentId self-reference), Product에 categoryId 추가 |

**학습 포인트**: 선착순 동시성 제어 (Redis DECR), Kafka consumer 실전, 계층형 데이터 모델링, 전략 패턴

---

## Phase 5: 운영 안정성 + 성능 최적화

> PR #10 머지 (2026-03-22)

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| Redisson 분산 락 | ✅ | DistributedLockService + 쿠폰 발급 적용 |
| API Rate Limiting | ✅ | Redis Sliding Window + RateLimitInterceptor (분당 60회) |
| Spring Cache 체계화 | ✅ | RedisCacheManager + brands 캐시 1h TTL |
| Cursor-based Pagination | ✅ | CursorPageResponse + 주문/리뷰 cursor 조회 |
| Resilience4j Circuit Breaker | ✅ | Kafka @CircuitBreaker + Redis @Retry |
| Graceful Shutdown | ✅ | RedisHealthIndicator 커스텀 헬스체크 |

**학습 포인트**: 분산 락 (Redisson), Rate Limiting (Sliding Window), Circuit Breaker 패턴, Cursor Pagination

---

## Phase 6: Spring Security + JWT 인증/인가

> PR #11 머지 (2026-03-24)

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| Auth API (signup/login/refresh) | ✅ | BCrypt + Access/Refresh Token |
| JwtTokenProvider | ✅ | Access Token 30분, Refresh Token 7일 (Redis 저장) |
| JwtAuthenticationFilter | ✅ | Bearer 토큰 파싱 + SecurityContext 설정 |
| User role 기반 RBAC | ✅ | USER/ADMIN 역할, 상품/브랜드/카테고리 관리는 ADMIN 전용 |
| 기존 컨트롤러 인증 전환 | ✅ | 8개 컨트롤러 X-USER-ID → SecurityContextHelper 전환 |
| E2E 테스트 JWT 전환 | ✅ | TestAuthHelper 기반 10개 테스트 전환 |

**학습 포인트**: Spring Security 필터 체인, JWT Access/Refresh 토큰 전략, RBAC, BCrypt

---

## Phase 7: 검색 + 개인화 (Elasticsearch)

> 도입 배경: [elasticsearch-adoption.md](elasticsearch-adoption.md)

| 요구사항 | 상태 | 상세 |
|----------|------|------|
| ES 인프라 구성 | ✅ | Docker Compose + Nori 커스텀 Dockerfile, Testcontainers |
| Product 인덱스 설계 + 매핑 | ✅ | name/brandName/categoryName 다중 필드, korean + edge_ngram 분석기 |
| ProductIndexer (MySQL → ES 동기화) | ✅ | @TransactionalEventListener(AFTER_COMMIT) 기반 실시간 동기화 |
| 상품 검색 API (multi_match + 필터) | ✅ | ES 역인덱스 + Circuit Breaker MySQL LIKE fallback |
| 자동완성 API (Completion Suggester) | ✅ | Edge N-gram match_phrase_prefix 기반 접두사 매칭 |
| 검색어 하이라이팅 | ✅ | PR #14 — multi_match + highlight DSL, name/brandName/categoryName 3필드, `<em>` 태그 고정, 키 생략 정책 |
| 집계 — Faceted Search | ✅ | 브랜드별/카테고리별/가격대별 상품 수 Aggregation |
| 오타 교정 (Fuzzy Query) | ✅ | PR #14 — `.fuzziness("AUTO")` (0~2자 정확 매칭, 3~5자 편집거리 1, 6자↑ 편집거리 2). 한글은 Nori 형태소 분해 결과 토큰 길이에 따라 편집거리 적용 구간이 바뀔 수 있음 |
| 동의어 사전 | ⬜ | Synonym Filter ("운동화" ↔ "스니커즈" ↔ "sneakers") |
| 인기 검색어 / 최근 검색어 | ⬜ | ES 검색 로그 집계 + Redis 사용자별 이력 |
| 전체 데이터 재인덱싱 배치 | ✅ | POST /admin/products/reindex (ADMIN 전용, 1000건 배치) |

**학습 포인트**: Elasticsearch 역인덱스, Nori 한글 형태소 분석, BM25 관련도 점수, Completion Suggester, Aggregation, Application Event 기반 CDC

---

## 다음 Phase 후보 (Roadmap)

> PR #12 머지 이후 식별된 개선 항목. PR 리뷰에서 Phase 7 범위 외로 거절한 이슈 + Phase 7 미완성 기능 + 테스트 기술 부채를 세 갈래로 분류한다.
> 추천 진행 순서: **A → B → C** (저비용 고효과 → 운영 결함 해소 → 아키텍처 학습).

### Phase 7-Extension (후보 A): ES 기능 마저 완성 ✅ (PR #14 머지)

> 단일 PR로 저비용 고효과 목표 달성. `ElasticsearchProductSearchAdapter.searchProducts()` 한 곳 + 데이터 파이프 4레이어 DTO 필드 추가 + API 계약 보안 가이드 주석.

| 우선순위 | 요구사항 | 상태 | 상세 |
|---------|---------|------|------|
| P0 | 검색어 하이라이팅 | ✅ | `highlight` DSL 추가, `ProductContentResponse.highlight: Map<String, List<String>>` 필드 추가. `<em>` 고정 태그, 매칭 없는 필드 키 생략 정책 |
| P0 | 오타 교정 (Fuzzy Query) | ✅ | `multi_match`에 `.fuzziness("AUTO")` 추가. AC-3 한글 3자 "나이크" 시나리오는 Nori 분해 결과 단일 토큰이 아니면 AUTO 편집거리가 적용되지 않는 한계로 영문 `Nile→Nike`(4자, 편집거리 1)로 테스트 대체 |

**학습 포인트**: ES Highlighter + HighlightField DSL (Java Client 8.15.5 — Spring Boot 3.4.4 관리), Fuzzy AUTO 편집 거리 정책(0~2자 정확 / 3~5자 편집거리 1 / 6자↑ 편집거리 2), Nori 한글 형태소 분석기와 Fuzzy의 토큰 단위 한계, Query-time 파라미터 기반 검색 고도화 (색인 스키마 무변경), API 계약서 보안 가이드 명시화

---

### Phase 8 (후보 B): 운영 안정성 2차

> 성격: 실제 운영 결함 해소. PR #12 리뷰에서 "Phase 7 범위 외"로 거절했지만 실제 버그 성격의 이슈들.

| 우선순위 | 요구사항 | 상태 | 상세 |
|---------|---------|------|------|
| P0 | Kafka 배치 리스너 멱등성 + DLQ | ⬜ | `OrderEventConsumer`: 배치 내 일부 실패 → ack 생략 → 중복 알림 발송 가능. `Notification` 유니크 제약 `(userId, type, orderId)` + `existsByUniqueKey` 체크. 실패 레코드 DLQ 전송 |
| P1 | SecurityContextHelper anonymous user 처리 | ⬜ | `getCurrentUserId()`가 `AnonymousAuthenticationToken` 캐스팅 → 잘못된 userId. principal 타입 검증 + UNAUTHORIZED 던지기 |
| P2 | Rate Limiter 알고리즘/용어 정합화 | ⬜ | 현재 `INCR + EXPIRE`는 Fixed Window지만 docs/주석은 "Sliding Window"로 표기. 문서 수정 또는 ZSET 기반 Sliding Window 재구현 |

**학습 포인트 예정**: Exactly-once semantics (Kafka), Idempotency key 설계, Spring Security principal 검증, Sliding vs Fixed Window Rate Limiting

---

### Phase 9 (후보 C): 검색 고도화 + 무중단 배포

> 성격: 아키텍처 학습 임팩트. 동의어 사전 작업과 Alias 재인덱싱 전략을 묶어서 일타쌍피.

| 우선순위 | 요구사항 | 상태 | 상세 |
|---------|---------|------|------|
| P1 | 동의어 사전 (Synonym Filter) | ⬜ | ES `synonym` 필터 + 사전 파일 관리 ("운동화" ↔ "스니커즈" ↔ "sneakers"). 인덱스 재생성 필요 |
| P2 | ES Alias 기반 Zero-downtime Reindexing | ⬜ | `products_v2` 생성 → 배치 인덱싱 → `products` alias 원자적 스위칭 → 기존 인덱스 삭제. 동의어 사전 적용 시 동시 진행 |
| P2 | 인기 검색어 / 최근 검색어 | ⬜ | Redis Sorted Set (인기, 시간 감쇠) + Redis List (사용자별 최근). `SearchEventPublisher` 도입 |

**학습 포인트 예정**: ES Alias 전략, Blue-Green 인덱싱, Synonym Filter, 시계열 감쇠 알고리즘, 이벤트 소싱 패턴

---

### 기술 부채 (상시 개선, 별도 PR로 분리 가능)

> PR #12 코드 리뷰에서 식별된 latent 이슈. 기능 추가 시 함께 정리 권장.

| 항목 | 위치 | 설명 |
|------|------|------|
| `ElasticsearchPerformanceTest.flushBatch` brand/category lookup 견고화 | `apps/commerce-api/.../search/ElasticsearchPerformanceTest.java` | `(p.getId() - 1) % length` 패턴이 AUTO_INCREMENT 리셋에 의존. `brandIdMap` reverse lookup으로 명시적 매핑 |
| 정적 필드 기반 테스트 상태 관리 리팩터링 | `ElasticsearchPerformanceTest.java` | `static dataInitialized`, `static Map` 사용. `@TestInstance(PER_CLASS)` + 인스턴스 필드로 개선 |
| `Thread.sleep` 기반 integration 테스트 안정화 | `ProductSearchV1ApiE2ETest`, `ProductIndexerIntegrationTest` | 현재는 안정적이지만 CI 부하 증가 시 플래키 가능. Awaitility + ES refresh 조합으로 전환 |
| ES facets price range 하드코딩 | `ElasticsearchProductSearchAdapter.facets()` | 현재는 한국 이커머스 가격대 기준 하드코딩. 요구사항 변경 시 설정화 또는 percentile aggregation 검토 |
