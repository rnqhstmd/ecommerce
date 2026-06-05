# 부하/스트레스 테스트 결과 — 주문 핫상품 동시성 개선

> 측정 도구: **k6**(grafana/k6 docker) · 대상: `ecommerce` develop(Spring Boot 3.4.4 / Java 21) · 측정일: 2026-06-05
> 스크립트: `load-test/k6/*.js` · 실행 가이드: `load-test/README.md`
> 핵심 결과: **주문 TPS 3.75배(21.5→80.5), p99 78%↓(5.37s→1.19s)** — 비관적 락 → Redis 선차감 + 트랜잭션 축소

---

## 1. 측정 개요

주문 핫패스(`POST /api/v1/orders`)의 비관적 락 경합을 측정하고, 재고·포인트를 Redis 원자 연산으로 전환하여 개선했다. 인기 상품 10개에 주문을 집중시켜 락 경합을 유발한다.

| 시나리오 | 대상 | 부하(VU/지속) | 목적 |
|----------|------|--------------|------|
| 01 product-read | `GET /products/{id}` | 10→50 ramp / 2분 | 캐시 효과(스모크) |
| 02 order-hot | `POST /orders` (핫상품 10개) | 50 VU / 2분 | 부하: 락 경합 |
| 03 order-stress | `POST /orders` | 20→120 ramp / 3.3분 | 스트레스: 한계 처리량 |

## 2. 측정 환경

| 항목 | 값 |
|------|-----|
| OS / HW | Windows 11, 단일 머신(앱·MySQL·Redis·Kafka·ES·k6 공존) |
| JVM | OpenJDK 21 (Gradle toolchain, foojay 자동 프로비저닝) |
| 앱 포트 | **8081** (8080은 Oracle TNS Listener 점유) |
| DB / Cache | MySQL 8.0(`docker-mysql-1`), Redis 7 Master/Replica |
| k6 | `grafana/k6` docker, BASE_URL=`host.docker.internal:8081` |
| HikariCP | max-pool=40, connection-timeout=3s |
| VU 상한 | **50~120** (Windows Docker Desktop의 host.docker.internal NAT가 고VU에서 dial timeout. 500 VU에서 99% 실패 → 50으로 축소) |

> **측정 환경 노트**: 단일 머신에 모든 구성요소가 공존하므로 절대 latency는 전용 부하 환경 대비 보수적(높게)이다. VU 규모도 NAT 한계로 낮다. 따라서 **절대값보다 동일 조건 Before/After 상대 비교가 유효**하다.

## 3. 결과 — 비관적 락(Before) vs Redis 선차감+트랜잭션 축소(After)

### 3.1 시나리오 02 — 주문 핫상품 (50 VU, 2분)

| 지표 | Before(비관적 락) | After(Redis) | 개선 |
|------|------|------|------|
| **TPS (req/s)** | 21.5 | **80.5** | **×3.75** |
| med | 1,622 ms | 429 ms | -74% |
| p95 | 4,381 ms | 927 ms | -79% |
| **p99** | 5,370 ms | **1,190 ms** | **-78%** |
| max | 6,752 ms | 1,886 ms | -72% |
| HikariCP active peak | 40/40 | 40/40 | (peak 동일, 점유시간 단축) |
| 에러율 | 0.00% | 0.41% | |

### 3.2 시나리오 03 — 스트레스 (20→120 VU, 3.3분)

| 지표 | Before | After | 개선 |
|------|------|------|------|
| **TPS** | 38.6 | **98.1** | **×2.54** |
| med | 1,099 ms | 464 ms | -58% |
| p95 | 2,965 ms | 951 ms | -68% |
| **p99** | 4,024 ms | **1,337 ms** | **-67%** |
| max | 5,601 ms | 2,098 ms | -63% |
| 에러율 | 0.01% | 0.23% | |

### 3.3 시나리오 01 — 상품 단건 조회 (캐시)

캐시 ON에서 326 req/s, p95 180ms, 에러율 0%. (아래 §5 캐시 버그 수정 후 정상화)

## 4. 원인 → 개선 (2단계 병목 추적)

### Before: 비관적 락이 HikariCP를 포화시킴
주문 트랜잭션(`OrderFacade.placeOrder`)이 단일 `@Transactional` 안에서:
1. 재고 `SELECT ... FOR UPDATE` (`StockDeductionService`, 핫상품 10개에 집중)
2. 포인트 `SELECT ... FOR UPDATE` + UPDATE + 이력 INSERT (`PointService.usePoint`)
3. 주문 저장(INSERT orders/order_items)

을 모두 수행 → 커넥션을 길게 점유 → **HikariCP 40/40 포화, TPS 21.5, p99 5.4s**.

### 1차 개선(실패로 배운 것): 재고 선차감만
재고를 Redis DECRBY로 선차감(SELECT FOR UPDATE 제거)했으나 **효과 없음**(TPS 21.5→21.7, HikariCP 40 유지). 측정으로 **진짜 병목은 포인트 비관적 락 + DB UPDATE row lock**임을 규명.

### 2차 개선(성공): 재고+포인트 Redis화 + 트랜잭션 축소
- 재고: Redis DECRBY 선차감, **DB 차감 제거**(주문 트랜잭션에서 재고 UPDATE row lock 제거)
- 포인트: Redis DECRBY 선차감(`RedisPointService`), **비관적 락+UPDATE+이력 INSERT 제거**
- 주문 트랜잭션의 DB 쓰기 = **INSERT orders/order_items만** 잔존

→ **TPS ×3.75, p99 -78%** 달성. 재고/포인트 정합성의 진실 원천은 Redis(원자 연산), DB는 주문 기록.

> 한계(문서화): 이번 PoC는 재고/포인트 DB 동기화를 생략했다. 실서비스는 Outbox/배치로 Redis→DB 최종 일관성을 맞추고, DLQ·재처리를 더해야 한다.

## 5. 부수 발견 — develop 실제 버그 수정

부하 측정 과정에서 develop의 결함 3건을 발견·수정했다 (모두 test 프로파일에선 드러나지 않던 것):

1. **Redis 캐시 역직렬화 2겹 버그** (cache ON에서 앱 다운): `RedisCacheConfig`의 `BasicPolymorphicTypeValidator`에 `allowIfSubType` 누락 + `FAIL_ON_UNKNOWN_PROPERTIES` 미설정. 파생 getter(`getPriceValue` 등)가 직렬화되어 역직렬화 실패. test는 캐시 OFF라 131개 테스트가 못 잡음.
2. **ddl-auto:update 스키마 드리프트**: `users.birth_date`, `points.amount` 가 NOT NULL인데 엔티티 미매핑(이전 스키마 잔재) → 회원가입/주문 500. orphan 컬럼 제거로 해결.
3. **JDK 21 toolchain**: `settings.gradle.kts`에 foojay-resolver 플러그인 추가로 자동 프로비저닝.

## 6. 시각 자료

- k6 HTML 리포트: `load-test/k6/results/report-{01,02,03}-*.html`
- Before/After 비교 차트: `load-test/charts/out/02-before-after.png`, `03-before-after.png`
- Grafana 대시보드: `docker/grafana/provisioning/dashboards/ecommerce-loadtest.json` (HikariCP/HTTP/주문메트릭/JVM 6패널)
- 커스텀 메트릭: `ecommerce_order_{success,fail,stock_shortage}_total` (`/actuator/prometheus`)

## 7. 재현 절차

```bash
# 1) worktree (develop 기반)
git worktree add -b feat/load-test-infra ../ecommerce-loadtest origin/develop
# 2) 인프라 + 모니터링
docker compose -f docker/infra-compose.yml up -d
docker compose -f docker/monitoring-compose.yml up -d
# 3) 앱 (8081, local+loadtest). Before=선차감 OFF, After=ON
./gradlew :apps:commerce-api:bootRun --args='--spring.profiles.active=local,loadtest [--stock.redis.pre-decrement.enabled=true]'
# 4) 시드: load-test/seed/seed.sql (products 10k)
# 5) After는 측정 전 Redis 워밍업: redis-cli SET stock:1..10 100000
# 6) 측정: bash load-test/run-measure.sh baseline   (또는 02/03 인라인)
# 7) 차트: docker run --rm -v "$PWD/load-test:/lt" -w /lt python:3-slim sh -c "pip install -q matplotlib && python charts/render.py"
```

## 8. 이력서 블록

> 주문 API k6 부하테스트(50 VU, 핫상품 10개 집중)에서 재고·포인트 **비관적 락이 HikariCP 풀(40)을 포화**시켜 TPS 21.5, p99 5.4s에 그침을 측정. 1차로 재고만 Redis 선차감했으나 효과가 없자 **측정으로 포인트 락+DB UPDATE row lock이 진짜 병목임을 규명**, 재고·포인트를 Redis 원자 연산으로 전환하고 주문 트랜잭션의 DB 쓰기를 주문 INSERT만 남기도록 축소 → **TPS 3.75배(21.5→80.5), p99 78%↓(5.4s→1.19s)** 달성. 더불어 cache-ON에서 앱이 다운되던 **Redis 직렬화 2겹 버그**(allowIfSubType/FAIL_ON_UNKNOWN 누락)와 ddl-auto 스키마 드리프트를 발견·수정. k6 HTML·Grafana 대시보드·비교 차트로 시각화.
