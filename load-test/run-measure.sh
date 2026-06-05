#!/usr/bin/env bash
# 부하 측정 연속 실행기. 인자: baseline | optimized | cacheoff
# 각 k6 측정 직후 Prometheus max_over_time으로 HikariCP active peak를 기록한다.
export MSYS_NO_PATHCONV=1
ROOT=/d/SQ/ecommerce-loadtest
R="$ROOT/load-test/k6/results"
PROM="http://localhost:9090/api/v1/query"
TREND="avg,min,med,p(90),p(95),p(99),max"
TAG="${1:-baseline}"

run() {
  docker run --rm -e BASE_URL=http://host.docker.internal:8081 \
    -v "$ROOT/load-test/k6:/scripts" grafana/k6 run \
    --summary-trend-stats="$TREND" "/scripts/$1" || true
}
hikari_peak() {  # $1=window e.g. 3m  $2=outfile
  curl -s "$PROM?query=max_over_time(hikaricp_connections_active%7Bpool%3D%22mysql-main-pool%22%7D%5B$1%5D)" > "$2"
}

if [ "$TAG" = "cacheoff" ]; then
  echo "### 01 cache OFF ###"; run 01-product-read.js; cp "$R/01-product-read.json" "$R/01-cacheoff.json"
  echo "### DONE cacheoff ###"; exit 0
fi

echo "### 01 cache ON ###"; run 01-product-read.js; cp "$R/01-product-read.json" "$R/01-cacheon.json"

echo "### 02 order ($TAG) ###"; run 02-order-hot-product.js; cp "$R/02-order-hot-product.json" "$R/02-$TAG.json"
hikari_peak 3m "$R/02-$TAG-hikari.json"

echo "### 03 stress ($TAG) ###"; run 03-order-stress.js; cp "$R/03-order-stress.json" "$R/03-$TAG.json"
hikari_peak 4m "$R/03-$TAG-hikari.json"

echo "### ALL DONE ($TAG) ###"
