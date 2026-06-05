# 부하/스트레스 테스트 (k6)

주문 핫상품 동시성 병목(비관적 락)을 측정하고 Redis 선차감으로 개선한 부하테스트.
결과: **TPS ×3.75, p99 -78%**. 상세: [`docs/load-test/2026-06-05-load-test-results.md`](../docs/load-test/2026-06-05-load-test-results.md)

## 구조
```
load-test/
├── k6/
│   ├── lib/common.js              # 로그인→JWT, 고유키, HTML 리포트(handleSummary)
│   ├── 01-product-read.js         # 캐시 효과(스모크)
│   ├── 02-order-hot-product.js    # 주문 부하(50 VU, 핫상품 10개)
│   ├── 03-order-stress.js         # 주문 스트레스(20→120 VU)
│   └── results/                   # *.json + report-*.html
├── charts/render.py               # before/after 비교 PNG 생성
├── seed/seed.sql                  # products 10k·brands 10
└── run-measure.sh                 # 측정 연속 실행기
```

## 사전 조건
1. 인프라 + 모니터링:
   ```bash
   docker compose -f docker/infra-compose.yml up -d        # MySQL/Redis/Kafka/ES (ready까지 ~1분 대기)
   docker compose -f docker/monitoring-compose.yml up -d    # Prometheus(9090)/Grafana(3000)
   ```
2. 앱 기동 (**8081**, 8080은 Oracle 점유). Before=선차감 OFF / After=ON:
   ```bash
   ./gradlew :apps:commerce-api:bootRun --args='--spring.profiles.active=local,loadtest [--stock.redis.pre-decrement.enabled=true]'
   ```
3. 시드: `docker exec -i docker-mysql-1 mysql -uapplication -papplication loopers < load-test/seed/seed.sql`
4. **After 측정 전 Redis 워밍업**: `for i in $(seq 1 10); do docker exec redis-master redis-cli SET stock:$i 100000; done`

## 실행
```bash
# 측정(docker k6). Windows Git Bash는 MSYS_NO_PATHCONV=1 필수
bash load-test/run-measure.sh baseline      # 01+02+03 → *-baseline.json
# (개선 적용·재기동·워밍업 후)
bash load-test/run-measure.sh optimized     # 또는 02/03 인라인 → *-optimized.json

# 비교 차트
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD/load-test:/lt" -w /lt python:3-slim \
  sh -c "pip install -q matplotlib && python charts/render.py"   # charts/out/*.png
```

## 환경 주의 (이 머신)
- **VU 50~120 상한**: Windows Docker Desktop의 `host.docker.internal` NAT가 고VU에서 dial timeout(500 VU=99% 실패). 단일 머신 공존 부하라 절대값보다 Before/After 상대 비교가 유효.
- k6 docker 볼륨/`-w`는 `MSYS_NO_PATHCONV=1` 없으면 경로 변환으로 깨짐.
- 결과 추출: `cd load-test/k6/results && py -3 -c "...d['metrics'][name]['values'][stat]..."`
