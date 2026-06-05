import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, makeHandleSummary } from './lib/common.js';

const readLatency = new Trend('product_read_latency', true);

export const options = {
  // Windows Docker Desktop의 host.docker.internal NAT 한계로 VU를 낮춰 연결 폭주 방지
  stages: [
    { duration: '20s', target: 10 },
    { duration: '1m',  target: 50 },
    { duration: '20s', target: 50 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    product_read_latency: ['p(95)<150', 'p(99)<400'],
  },
};

export default function () {
  const id = Math.floor(Math.random() * 10) + 1;   // 핫상품 1~10 (캐시 hit 유도)
  const res = http.get(`${BASE_URL}/api/v1/products/${id}`);   // permitAll, 토큰 불필요
  readLatency.add(res.timings.duration);
  check(res, { 'status 200': (r) => r.status === 200 });
}

export const handleSummary = makeHandleSummary('01-product-read');
