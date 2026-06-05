import http from 'k6/http';
import { check } from 'k6';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// 8080은 Oracle TNS Listener가 점유 → 앱은 8081. (k6 docker는 host.docker.internal로 호스트 접근)
export const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8081';

// 모듈 스코프 카운터로 setup 컨텍스트(VU 정보 없음)에서도 고유성 보장
let _c = 0;
export function uniqueSuffix(vu, iter) {
  _c += 1;
  return `${vu || 0}-${iter || 0}-${_c}`;
}

// 회원가입 → accessToken. userId는 영문/숫자 10자 이내(하이픈 금지).
// 이미 가입된 userId면 로그인으로 토큰을 획득해 재실행에도 견고하게 동작한다.
export function signupAndToken(userId) {
  const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };
  let res = http.post(`${BASE_URL}/api/v1/auth/signup`, JSON.stringify({
    userId: userId,
    email: `${userId}@lt.com`,
    birthDate: '1990-01-01',
    gender: 'MALE',              // Gender enum: MALE, FEMALE, OTHER
    password: 'password123',
  }), jsonHeaders);

  if (res.status !== 200) {
    // 중복 가입 등으로 실패 → 로그인 fallback
    res = http.post(`${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({ userId: userId, password: 'password123' }), jsonHeaders);
  }
  check(res, { 'auth ok': (r) => r.status === 200 });
  return res.json('data.accessToken');   // ApiResponse{ meta, data }
}

export function authHeaders(token) {
  return { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };
}

// 시나리오별 HTML + JSON 리포트 동시 출력
export function makeHandleSummary(name) {
  return function (data) {
    return {
      [`/scripts/results/report-${name}.html`]: htmlReport(data),
      [`/scripts/results/${name}.json`]: JSON.stringify(data),
      stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
  };
}
