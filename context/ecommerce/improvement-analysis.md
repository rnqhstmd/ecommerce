# ecommerce 영역별 개선 전후 분석

> 각 Phase를 진행하면서 왜 이런 개선이 필요했고, 이전에는 어떤 문제가 있었고, 어떻게 풀었는지를 정리했습니다.

- 작성일: 2026-03-29

---

## 1. 동시성 제어

### 1-1. 재고 차감 — 비관적 락 + 데드락 방지

Week 2 초기에는 `Product.decreaseStock(quantity)`를 그냥 호출하는 방식이었습니다. 동시에 여러 주문이 같은 상품을 사면 lost update가 생길 수 있었습니다.

```
Thread A: read stock=10 → decrease → stock=8
Thread B: read stock=10 → decrease → stock=7
결과: 실제로 5개가 빠져야 하는데 stock=7 (3개 유실)
```

두 트랜잭션이 같은 row를 동시에 읽으면, 나중에 커밋하는 쪽이 먼저 커밋한 변경을 덮어쓰는 게 문제였습니다. 거기에 복수 상품을 동시에 락 잡을 때도, 주문 A가 상품 [1, 2], 주문 B가 상품 [2, 1] 순서로 락을 잡으면 데드락이 걸렸습니다.

`StockDeductionService`에서 비관적 락(PESSIMISTIC_WRITE)과 ID 오름차순 정렬 조합으로 해결했습니다.

```java
// ProductJpaRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id ASC")
List<Product> findAllByIdsWithLock(@Param("ids") List<Long> ids);
```

```java
// StockDeductionService — Propagation.MANDATORY로 반드시 기존 트랜잭션 내에서 실행
List<Long> sortedProductIds = commands.stream()
        .map(StockDeductionCommand::productId)
        .sorted()  // 데드락 방지: 항상 같은 순서로 락 획득
        .toList();
List<Product> products = productService.getProductsByIdsWithLock(sortedProductIds);
```

`SELECT ... FOR UPDATE`로 DB 레벨에서 동시 읽기를 막고, `ORDER BY p.id ASC`로 모든 트랜잭션이 같은 순서로 락을 잡게 했습니다. `Propagation.MANDATORY`를 걸어서 이 메서드가 트랜잭션 없이 단독 호출되는 것도 막았습니다.

---

### 1-2. 쿠폰 선착순 발급 — 3단계 진화

선착순 쿠폰 발급은 프로젝트에서 가장 여러 번 고친 부분입니다. 총 3단계에 걸쳐 동시성 보호 수준을 올렸습니다.

**Phase 4 초기 — hasKey + set (비원자적)**

```java
Boolean hasKey = redisTemplate.hasKey(stockKey);
if (Boolean.FALSE.equals(hasKey)) {
    int remaining = policy.getTotalQuantity() - policy.getIssuedQuantity();
    redisTemplate.opsForValue().set(stockKey, String.valueOf(remaining));  // 비원자적!
}
Long remaining = redisTemplate.opsForValue().decrement(stockKey);
```

`hasKey` 체크와 `set` 사이에 다른 스레드가 끼어드는 게 문제였습니다. 잔여 수량 3인 상태에서 두 스레드가 동시에 `hasKey=false`를 읽으면, 둘 다 `set(3)`을 실행해서 이미 차감된 수량이 리셋됩니다.

**PR #9 리뷰 반영 — setIfAbsent (원자적)**

```java
// Redis에 키가 없으면 원자적으로 초기화 (SETNX)
redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(remainingStock));
```

`setIfAbsent`(= Redis SETNX)는 키가 없을 때만 값을 세팅하는 원자적 연산이라, 동시에 여러 스레드가 호출해도 최초 1개만 성공합니다. catch 범위도 `CoreException`에서 `Exception`으로 넓혀서, DB 예외가 나도 Redis 수량이 복구되게 했습니다.

**Phase 5 (PR #10 리뷰) — Redisson 분산 락 + DB 비관적 락**

```java
// CouponFacade — 3중 보호
public CouponInfo issueCoupon(Long couponPolicyId, String userId) {
    String lockKey = COUPON_LOCK_KEY_PREFIX + couponPolicyId;
    return distributedLockService.executeWithLock(   // 1) Redisson 분산 락
            lockKey, waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS,
            () -> doIssueCoupon(couponPolicyId, userId));
}

private CouponInfo doIssueCoupon(Long couponPolicyId, String userId) {
    return transactionTemplate.execute(status -> {
        CouponPolicy policy = couponService.getCouponPolicyWithLock(couponPolicyId);  // 2) DB 비관적 락
        // ... setIfAbsent + DECR ...                                                  // 3) Redis 원자적 연산
    });
}
```

왜 3중으로 보호해야 했냐면, 각 계층 하나만으로는 빈틈이 있었기 때문입니다.

| 계층 | 역할 | 혼자서는 부족한 이유 |
|------|------|---------------------|
| Redisson 분산 락 | 애플리케이션 인스턴스 간 직렬화 | Redis 단일 장애 시 보호 불가 |
| DB 비관적 락 | `issuedQuantity` lost update 방지 | 분산 환경에서 DB만으로는 Redis 수량과 동기화 불가 |
| Redis SETNX + DECR | 빠른 수량 소진 판단 | DB 반영 실패 시 Redis와 DB 간 불일치 발생 가능 |

분산 락이 요청을 직렬화하고, DB 락이 데이터 정합성을 잡고, Redis가 빠른 수량 체크를 맡는 구조입니다.

---

### 1-3. DistributedLockService — Redisson 분산 락 서비스

Phase 5에서 운영 안정성을 신경 쓰면서, 서버를 여러 대 띄우는 수평 확장 상황을 고려하게 됐습니다. `synchronized`나 DB 락만으로는 JVM이 여러 개일 때 동시성을 못 잡습니다.

```java
public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
    RLock lock = redissonClient.getLock(key);
    boolean acquired = false;
    try {
        acquired = lock.tryLock(waitTime, leaseTime, unit);
        if (!acquired) {
            throw new CoreException(ErrorType.CONFLICT, "다른 요청이 처리 중입니다.");
        }
        return supplier.get();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CoreException(ErrorType.INTERNAL_ERROR, "락 획득 중 인터럽트가 발생했습니다.");
    } finally {
        if (acquired && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

`tryLock`으로 일정 시간만 대기하고 실패하게 해서 무한 대기를 막았고, `leaseTime`을 줘서 프로세스가 죽어도 락이 자동 해제되게 했습니다. `isHeldByCurrentThread()` 체크는 다른 스레드가 가진 락을 실수로 풀지 않기 위한 안전장치입니다. `waitTime`과 `leaseTime`은 `CouponLockProperties`로 빼서 운영 중에 조정할 수 있게 했습니다.

---

## 2. 캐시 전략

### 2-1. 장바구니 — Redis Hash (TTL 7일)

Phase 3 전에는 장바구니 기능 자체가 없었습니다. 상품을 고르면 바로 주문해야 했습니다.

장바구니는 임시 데이터이고, 수량 변경이 잦고, 사용자별로 독립적입니다. RDB에 넣으면 수량 바꿀 때마다 UPDATE 쿼리가 나가고, 유효기간 관리용 배치도 따로 돌려야 합니다. Redis Hash가 이 세 가지를 자연스럽게 해결해줬습니다.

```java
// key: cart:{userId}, field: productId, value: quantity
public long addItem(String userId, Long productId, int quantity) {
    String key = cartKey(userId);
    Long result = redisTemplate.opsForHash().increment(key, productId.toString(), quantity);
    return result != null ? result : quantity;
}
```

`HINCRBY`가 원자적 연산이라 동시에 같은 상품을 담아도 수량이 정확합니다. TTL 7일을 줘서 방치된 장바구니는 알아서 정리되고, `checkout` 시에는 기존 `placeOrder` 플로우를 그대로 재사용해서 주문 로직을 건드리지 않았습니다.

---

### 2-2. 인기 상품 — Redis Sorted Set

이전에는 인기 상품을 보여주려면 매번 `SELECT ... ORDER BY like_count DESC LIMIT N` 쿼리를 날려야 했습니다. 트래픽이 몰리면 DB 부하가 집중됩니다.

Sorted Set은 score 기반 정렬을 O(log N)으로 해주고, `ZREVRANGE`로 TOP N을 바로 꺼낼 수 있습니다. like_count를 score로 쓰면 정렬이 공짜입니다.

```java
// 캐시 히트: Sorted Set에서 상위 N개 반환
Set<ZSetOperations.TypedTuple<String>> cached = redisTemplate.opsForZSet()
        .reverseRangeWithScores(POPULAR_KEY, 0, limit - 1);

// 캐시 미스: DB 조회 → Sorted Set 적재 → TTL 1시간
redisTemplate.opsForZSet().add(POPULAR_KEY, tuples);
redisTemplate.expire(POPULAR_KEY, 1, TimeUnit.HOURS);
```

TTL은 1시간으로 잡았습니다. 인기 상품은 실시간 정확도보다 DB 부하를 줄이는 게 더 중요하다고 판단했습니다. `@Retry` + fallback을 걸어서 Redis가 죽어도 DB에서 직접 조회하게 했습니다.

---

### 2-3. Spring Cache 체계화 — RedisCacheManager

Phase 4까지는 캐시를 `redisTemplate`으로 직접 get/set 하는 방식이었습니다. 캐시 키 관리가 여기저기 흩어져 있고, TTL 설정도 코드 곳곳에 박혀 있어서 관리가 힘들었습니다.

Phase 5에서 Spring Cache 추상화(`@Cacheable`, `@CacheEvict`)를 도입하고, `RedisCacheManager`로 캐시별 TTL을 한 곳에서 관리하게 바꿨습니다.

```java
Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
cacheConfigurations.put("product", defaultConfig.entryTtl(Duration.ofMinutes(10)));
cacheConfigurations.put("brands", defaultConfig.entryTtl(Duration.ofHours(1)));
```

```java
// BrandService — 선언적 캐싱
@Cacheable(value = "brands", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
public Page<Brand> getBrands(Pageable pageable) { ... }
```

비즈니스 로직과 캐시 로직이 분리되니 코드가 깔끔해졌고, TTL 변경도 한 곳만 고치면 됩니다. `@ConditionalOnProperty`로 local/test에서는 캐시를 끄게 해서 테스트 격리도 챙겼습니다.

---

### 2-4. 좋아요 수 비정규화 + 캐시 무효화

이전에는 `likeCount`를 매번 `SELECT COUNT(*) FROM likes WHERE product_id = ?`로 계산했습니다. 상품 목록에서 각 상품마다 COUNT 쿼리가 나가는 N+1 문제가 있었습니다.

Product 엔티티에 `likeCount` 필드를 추가해서 비정규화하고, 좋아요 추가/제거 시 원자적으로 업데이트하는 방식으로 바꿨습니다.

```java
// LikeService
productRepository.incrementLikeCount(productId);  // UPDATE product SET like_count = like_count + 1
productService.evictProductCache(productId);       // @CacheEvict
```

상품 목록 조회는 읽기 빈도가 쓰기보다 압도적으로 높아서, 비정규화의 이점이 큽니다. COUNT 쿼리가 사라지니 목록 응답이 빨라졌고, 캐시 무효화로 비정규화 데이터 정합성도 잡았습니다.

---

### 2-5. Rate Limiting — Redis Lua 스크립트

Phase 5 초기에는 INCR과 EXPIRE를 따로 호출했습니다.

```java
Long count = redisTemplate.opsForValue().increment(key);
if (count != null && count == 1L) {
    redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);  // 별도 호출!
}
```

INCR 후 프로세스가 죽으면 EXPIRE가 안 걸려서 해당 키가 영구적으로 남습니다. 그러면 해당 사용자/IP가 영구 차단되는 셈입니다.

PR #10 리뷰에서 Lua 스크립트로 두 연산을 원자적으로 묶었습니다.

```lua
local count = redis.call('INCR', KEYS[1])
if count == 1 then
  redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return count
```

사용자 식별 방식도 바꿨습니다. 이전에는 `X-Forwarded-For` 헤더를 썼는데, 이건 클라이언트가 조작할 수 있어서 `SecurityContext`의 userId + `remoteAddr` 폴백으로 변경했습니다.

---

## 3. 이벤트 시스템 + 복원력

### 3-1. Kafka 이벤트 발행 — AFTER_COMMIT 보장

이전에는 이벤트 없이 동기적으로 후속 작업(알림 발송 등)을 다 처리했습니다. 주문 API 응답 시간에 알림 발송 시간이 포함되고, 알림 발송이 실패하면 주문 자체가 실패하는 문제가 있었습니다.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "handleOrderPlacedFallback")
public void handle(OrderPlacedEvent event) {
    kafkaTemplate.send(TOPIC_ORDER_PLACED, String.valueOf(event.orderId()), event)
            .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
}
```

`AFTER_COMMIT`을 쓴 이유는 명확합니다. 트랜잭션 커밋 전에 Kafka에 이벤트를 보내면, consumer가 이벤트를 받았는데 원본 트랜잭션이 롤백되는 상황이 생깁니다. 존재하지 않는 주문에 대한 알림이 발송되는 거죠. 트랜잭션이 확정된 후에만 이벤트를 발행하게 해서 이 문제를 막았습니다.

CircuitBreaker도 같이 걸었습니다. Kafka 장애 시 이벤트 발행 실패가 반복되면 Circuit이 OPEN되어 빠르게 실패하는데, 이벤트 발행이 트랜잭션 밖이라 주문 자체에는 영향이 없습니다.

---

### 3-2. 보상 트랜잭션 — 주문 취소

Week 2에는 주문 취소 기능이 아예 없었습니다. 주문은 생성만 되고 취소할 수 없었습니다.

주문 취소가 까다로운 건, 원복해야 하는 항목이 여러 도메인에 걸쳐 있기 때문입니다. 주문 상태(PAID → CANCELLED), 재고 복구, 포인트 환불, 쿠폰 사용 플래그 복원 — 이 4가지 중 하나라도 빠지면 데이터 불일치가 생깁니다.

```java
@Transactional
public OrderInfo.CancelInfo cancelOrder(Long orderId, String userId) {
    Order order = orderService.getOrderByIdWithLock(orderId);    // 비관적 락
    order.cancel();                                                // 상태 전이
    restoreStock(order);                                           // 재고 복구 (ID 정렬 비관적 락)
    pointService.refundPoint(userId, order.getActualPaymentAmount()); // 포인트 환불
    if (order.getUserCouponId() != null) {
        couponService.restoreCoupon(order.getUserCouponId());      // 쿠폰 복원
    }
    eventPublisher.publishEvent(OrderCancelledEvent.from(order));  // 이벤트 발행
    return OrderInfo.CancelInfo.from(order);
}
```

단일 `@Transactional` 안에서 모든 보상 작업을 수행해서, 하나라도 실패하면 전체가 롤백됩니다. `getActualPaymentAmount()`로 쿠폰 할인 적용 후 실제 결제 금액만 환불하게 한 것도 신경 쓴 부분입니다. 재고 복구도 ID 오름차순 비관적 락으로 데드락을 방지했습니다.

---

### 3-3. Resilience4j — Circuit Breaker + Retry

Phase 5에서 외부 의존성(Kafka, Redis)의 장애 전파를 막기 위해 도입했습니다. 장애 전파를 막지 않으면 Kafka 장애가 이벤트 발행 타임아웃으로 번지고, 그게 주문 API 응답 지연으로, 결국 전체 시스템이 느려지는 상황이 생깁니다. Redis 장애 시 인기 상품 조회가 실패하는 것도 마찬가지인데, 이건 DB로 대체 가능한 기능이라 에러를 보여줄 필요가 없었습니다.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      kafkaPublisher:
        sliding-window-size: 10        # 최근 10건 기준
        failure-rate-threshold: 50      # 50% 실패 시 OPEN
        wait-duration-in-open-state: 10s # 10초 후 HALF_OPEN
  retry:
    instances:
      redisRetry:
        max-attempts: 3
        wait-duration: 500ms
```

| 패턴 | 적용 대상 | 동작 |
|------|----------|------|
| Circuit Breaker | Kafka 이벤트 발행 | 연속 실패 시 빠르게 실패, fallback으로 로그만 기록 |
| Retry | Redis 인기 상품 조회 | 3회 재시도 후 DB 직접 조회로 폴백 |

---

### 3-4. Graceful Shutdown

서버를 재시작할 때 진행 중인 요청이 강제 종료되면, 주문이 중간에 끊기거나 Kafka 메시지가 유실될 수 있습니다.

```yaml
server:
  shutdown: graceful                      # 새 요청 수신 중단
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s       # 진행 중 요청 완료 대기 (최대 30초)
```

```java
// RedisHealthIndicator — 로드밸런서가 인스턴스 상태 판단에 사용
public Health health() {
    String pong = redisConnectionFactory.getConnection().ping();
    if ("PONG".equals(pong)) {
        return Health.up().withDetail("redis", "연결 정상").build();
    }
    return Health.down().withDetail("redis", "PING 응답 비정상").build();
}
```

종료 신호가 오면 새 커넥션 수신을 먼저 중단하고, 진행 중인 요청은 최대 30초까지 완료를 기다립니다. `/actuator/health`가 DOWN을 반환하면 로드밸런서가 트래픽을 차단하고, 모든 요청이 끝나면 안전하게 종료됩니다.

---

## 4. 페이지네이션 진화

### Offset → Cursor 전환

Phase 1~4까지는 Spring Data의 기본 `Pageable`로 offset 기반 페이지네이션을 썼습니다.

```sql
SELECT * FROM orders WHERE user_id = ? LIMIT 20 OFFSET 10000;
```

offset이 커질수록 DB가 앞의 10,000개 row를 읽고 버리는 비효율이 생겼습니다. 페이지를 넘기는 도중에 데이터가 삽입되거나 삭제되면 같은 항목이 중복으로 보이거나 누락되는 문제도 있었습니다.

Phase 5에서 cursor 기반으로 바꿨습니다.

```java
// CursorPageResponse — size+1 패턴
public static <T, E> CursorPageResponse<T> of(
        List<E> entities, int size,
        Function<E, Long> idExtractor, Function<E, T> mapper) {
    boolean hasNext = entities.size() > size;           // size+1개를 요청해서 다음 페이지 존재 여부 판단
    List<E> content = hasNext ? entities.subList(0, size) : entities;
    Long nextCursor = hasNext ? idExtractor.apply(content.get(content.size() - 1)) : null;
    return new CursorPageResponse<>(mapped, nextCursor, hasNext);
}
```

```sql
-- Cursor 기반 쿼리: 인덱스를 타므로 항상 일정한 성능
SELECT * FROM orders WHERE user_id = ? AND id < ? ORDER BY id DESC LIMIT 21;
```

`size + 1`개를 조회해서 별도 COUNT 쿼리 없이 `hasNext`를 판단합니다. WHERE 조건으로 이전 페이지의 마지막 ID 이후만 가져오니까 데이터가 아무리 많아도 성능이 일정하고, 데이터 삽입/삭제에도 중복이나 누락이 없습니다.

---

## 5. 인증/인가 전환

### X-USER-ID 헤더 → Spring Security + JWT

Week 2 설계에서는 클라이언트가 보내는 `X-USER-ID` 헤더를 그대로 신뢰하는 방식이었습니다.

```java
// Week 2 방식: 클라이언트가 보내는 헤더를 그대로 신뢰
@RequestHeader(value = "X-USER-ID", required = false) String userId
```

문제가 심각했습니다. 아무 검증 없이 클라이언트가 보내는 userId를 그대로 쓰니까, 누구든 다른 사용자의 ID를 헤더에 넣어서 요청할 수 있었습니다. 사실상 인증이 없는 것과 같았습니다. 역할 구분도 없어서 모든 사용자가 관리자 API를 호출할 수 있었고, 토큰 만료 개념이 없어서 한번 알아낸 userId는 영구적으로 쓸 수 있었습니다.

Phase 6에서 Spring Security + JWT 기반으로 전면 교체했습니다.

**JWT 토큰 발급**

```java
// JwtTokenProvider
public String createAccessToken(String userId, Role role) {
    return Jwts.builder()
            .subject(userId)
            .claim("role", role.name())   // USER 또는 ADMIN
            .issuedAt(now)
            .expiration(new Date(now.getTime() + accessExpiration))  // 30분
            .signWith(secretKey)          // HMAC-SHA 서명
            .compact();
}
```

Access Token 30분, Refresh Token 7일로 설정했습니다. Refresh Token은 Redis에 저장하고, 갱신 시 이전 토큰을 무효화하는 Token Rotation을 적용했습니다. 비밀번호는 BCrypt로 해싱합니다.

**필터 체인**

```java
// JwtAuthenticationFilter — Bearer 토큰 파싱 → SecurityContext 설정
String token = resolveToken(request);  // "Bearer {token}"에서 추출
if (token != null && jwtTokenProvider.validateToken(token)) {
    String userId = claims.getSubject();
    Role role = Role.valueOf(claims.get("role", String.class));
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                    userId, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

**RBAC (역할 기반 접근 제어)**

```java
// SecurityConfig
.requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()    // 누구나 조회 가능
.requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN") // 상품 생성은 ADMIN만
.anyRequest().authenticated()                                           // 나머지는 로그인 필수
```

| 이전 | 이후 |
|------|------|
| `X-USER-ID` 헤더 (검증 없음) | JWT 서명 검증 |
| 역할 구분 없음 | USER/ADMIN RBAC |
| 만료 없음 | Access 30분 + Refresh 7일 |
| `@RequestHeader` 파라미터 | `SecurityContextHelper.getCurrentUserId()` |

**컨트롤러 전환**

이전에는 `@RequestHeader("X-USER-ID") String userId`가 8개 컨트롤러에 흩어져 있었는데, `SecurityContextHelper.getCurrentUserId()`로 전부 바꿨습니다. 선택적 인증이 필요한 곳(상품 상세의 isLiked 등)에는 `getCurrentUserIdOrNull()`을 씁니다.

```java
String userId = SecurityContextHelper.getCurrentUserId();      // 인증 필수 API
String userId = SecurityContextHelper.getCurrentUserIdOrNull(); // 선택적 인증 (isLiked 등)
```

E2E 테스트도 10개 전부 JWT 방식으로 전환했습니다. `TestAuthHelper`를 만들어서 테스트에서도 실제 인증 플로우를 거치게 했습니다.

---

## 6. 도메인 모델 진화

### 6-1. 주문 상태 머신

Week 2에는 `PENDING` → `PAID` 단방향만 있었습니다. Phase 2에서 `PAID` → `CANCELLED` 전이를 추가하면서, 상태 전이 규칙을 엔티티 안에 넣었습니다.

```java
public void cancel() {
    if (this.status != OrderStatus.PAID) {
        throw new CoreException(ErrorType.BAD_REQUEST, "결제 완료 상태의 주문만 취소할 수 있습니다.");
    }
    this.status = OrderStatus.CANCELLED;
    this.cancelledAt = ZonedDateTime.now();
}
```

어떤 서비스에서 `cancel()`을 호출하든 같은 비즈니스 규칙이 적용됩니다. 상태 전이 로직이 서비스 레이어에 흩어지면 규칙이 깨지기 쉬운데, 엔티티에 넣어서 그걸 막았습니다.

---

### 6-2. 포인트 이력 — Audit Trail

이전에는 Point 엔티티에 balance만 있었습니다. 충전/사용/환불 이력이 없어서 잔액이 어떻게 변했는지 추적할 방법이 없었습니다.

Phase 2에서 PointHistory 엔티티를 추가했습니다.

```java
// 모든 포인트 변동 시 자동으로 이력 생성
private Point updatePointAndLog(String userId, Long amount, PointHistoryType type, ...) {
    Point point = getPointWithLock(userId);
    operation.accept(point, amount);
    pointRepository.save(point);
    pointHistoryRepository.save(
            PointHistory.create(point, type, amount, point.getBalanceValue()));  // 변동 후 잔액 스냅샷
    return point;
}
```

| 필드 | 역할 |
|------|------|
| `type` | CHARGE / USE / REFUND |
| `amount` | 변동 금액 |
| `balanceAfter` | 변동 후 잔액 (스냅샷) → 이력만으로 현재 잔액 검증 가능 |

이력이 포인트 변동과 같은 트랜잭션 안에서 저장되기 때문에, 포인트는 바뀌었는데 이력이 안 남는 상황은 없습니다.

---

### 6-3. Soft Delete 패턴

`BaseEntity`에 `deletedAt` 필드를 두고, `@SQLRestriction("deleted_at IS NULL")`로 조회 시 자동 필터링하는 방식입니다.

```java
// BaseEntity
public void delete() {
    if (this.deletedAt == null) {
        this.deletedAt = ZonedDateTime.now();
    }
}
```

Soft Delete를 선택한 이유는 세 가지입니다. 실수로 삭제된 데이터를 복구할 수 있고, 삭제된 데이터도 외래 키 참조가 유지되고(삭제된 리뷰도 주문 이력에서 참조 가능), `@SQLRestriction` 덕분에 비즈니스 코드에서는 삭제 여부를 신경 쓸 필요가 없습니다.

---

### 6-4. Clean Architecture 레이어

```
Interface Layer  → Controller, DTO, ApiSpec
Application Layer → Facade(오케스트레이션), Command(입력), Info(출력)
Domain Layer     → Entity, ValueObject, Service, Repository(인터페이스)
Infrastructure   → RepositoryImpl, JpaRepository, Config
```

Facade를 도입한 건 여러 도메인 서비스를 조합하는 유스케이스가 늘어나면서입니다. 주문 하나 처리하려면 재고 + 포인트 + 쿠폰 + 이벤트를 다 건드려야 하는데, 이 조합 로직이 컨트롤러에 들어가면 컨트롤러가 뚱뚱해지고, 서비스끼리 직접 호출하면 순환 의존이 생겼습니다.

Facade가 트랜잭션 경계이자 오케스트레이션 지점 역할을 맡으면서, 컨트롤러는 HTTP 관심사만, 도메인 서비스는 단일 도메인 로직만 담당하게 됐습니다. 입력(Command)과 출력(Info)을 record로 분리한 것도, API 응답 형태를 바꿔도 도메인이 영향받지 않게 하기 위해서입니다.

---

## 개선 흐름 요약

```
Week 2: 기본 CRUD + 동시성(비관적 락)
  ↓
Phase 1: 미노출 API 연결 (isLiked, 페이징)
  ↓
Phase 2: 주문 취소(보상 트랜잭션) + 포인트 이력(Audit Trail) + 동적 쿼리(QueryDSL)
  ↓
Phase 3: Redis 활용 확대 (Hash 장바구니, Sorted Set 인기상품) + 캐시 무효화
  ↓
Phase 4: 신규 도메인(리뷰, 쿠폰, 카테고리) + Kafka 이벤트 + 선착순 동시성(SETNX)
  ↓
Phase 5: 운영 안정성 (분산 락, Rate Limiting, Circuit Breaker, Cursor Pagination, Graceful Shutdown)
  ↓
Phase 6: 인증/인가 (JWT + RBAC) — 기존 X-USER-ID 전면 교체
  ↓
Phase 7: Elasticsearch 검색 (예정)
```

각 Phase는 이전 Phase에서 발견된 한계를 해결하는 방향으로 진행했습니다. 특히 동시성 제어(비관적 락 → SETNX → 분산 락)와 캐시 전략(수동 redisTemplate → Spring Cache)은 코드 리뷰를 거치면서 단계적으로 강화됐습니다.
