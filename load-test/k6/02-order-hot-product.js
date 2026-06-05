import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, signupAndToken, authHeaders, makeHandleSummary } from './lib/common.js';

const orderLatency = new Trend('order_latency', true);

export const options = {
  scenarios: {
    // host.docker.internal NAT 한계로 50 VU. 핫상품 10개 집중으로 락 경합은 충분히 유발됨
    hot: { executor: 'constant-vus', vus: 50, duration: '2m' },
  },
  thresholds: {
    http_req_failed: ['rate<0.1'],      // Before는 락 대기 타임아웃 예상 → 결과로 실제값 기록
    order_latency: ['p(99)<5000'],
  },
};

// 쓰기 인증 필요: setup에서 사용자 풀(50명) 생성 + 포인트 충전.
export function setup() {
  const tokens = [];
  for (let i = 0; i < 50; i++) {
    const uid = `lt${i}`;               // 영숫자, 10자 이내
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
  const hotProductId = Math.floor(Math.random() * 10) + 1;   // 핫상품 10개 집중 → 락 경합 유발
  const body = JSON.stringify({ items: [{ productId: hotProductId, quantity: 1 }] });

  const res = http.post(`${BASE_URL}/api/v1/orders`, body, { headers: authHeaders(token) });
  orderLatency.add(res.timings.duration);
  check(res, { 'order 200': (r) => r.status === 200 });
}

export const handleSummary = makeHandleSummary('02-order-hot-product');
