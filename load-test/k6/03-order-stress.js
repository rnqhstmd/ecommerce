import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, signupAndToken, authHeaders, makeHandleSummary } from './lib/common.js';

const orderLatency = new Trend('order_stress_latency', true);

export const options = {
  // VU를 점증시켜 breaking point 관찰. host.docker.internal NAT 한계로 상한 120.
  stages: [
    { duration: '40s', target: 20 },
    { duration: '40s', target: 50 },
    { duration: '40s', target: 80 },
    { duration: '40s', target: 120 },
    { duration: '40s', target: 0 },
  ],
  thresholds: {
    // 임계 초과해도 중단하지 않고 끝까지 측정
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: false }],
  },
};

export function setup() {
  const tokens = [];
  for (let i = 0; i < 50; i++) {
    const uid = `st${i}`;               // 영숫자, 10자 이내
    const t = signupAndToken(uid);
    if (t) {
      http.post(`${BASE_URL}/api/v1/points/charge`,
        JSON.stringify({ amount: 100000000 }), { headers: authHeaders(t) });
      tokens.push(t);
    }
  }
  return { tokens };
}

export default function (data) {
  const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
  const hotProductId = Math.floor(Math.random() * 10) + 1;
  const body = JSON.stringify({ items: [{ productId: hotProductId, quantity: 1 }] });

  const res = http.post(`${BASE_URL}/api/v1/orders`, body, { headers: authHeaders(token) });
  orderLatency.add(res.timings.duration);
  check(res, { 'order 200': (r) => r.status === 200 });
}

export const handleSummary = makeHandleSummary('03-order-stress');
