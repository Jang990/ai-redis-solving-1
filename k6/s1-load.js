// S1 부하 게이트 — 상황·목표: .learning/redis/brief.md
// 계약: ① 부하 속 모든 응답 정확  ② p99 < 100ms
// 실행: 서버를 호스트에서 띄우고(./gradlew bootRun)
//       docker run --rm -i grafana/k6 run - < k6/s1-load.js
import http from 'k6/http';
import { check } from 'k6';

// k6가 도커 안에서 돌 때 호스트의 서버를 가리키는 주소.
// 로컬 k6 바이너리로 돌리면: k6 run -e BASE_URL=http://localhost:8080 k6/s1-load.js
const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
  scenarios: {
    open_rush: {
      executor: 'constant-vus',
      vus: 100,        // 부하 knob — green 후 코치가 올린다
      duration: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<100'], // 계약 ②: p99 < 100ms
    checks: ['rate==1'],              // 계약 ①: 응답 전부 정확
  },
};

export function setup() {
  const res = http.post(`${BASE}/products`,
    JSON.stringify({ name: '오픈런 굿즈', price: 10000, stock: 777 }),
    { headers: { 'Content-Type': 'application/json' } });
  if (res.status !== 201) throw new Error(`상품 생성 실패: status=${res.status}`);
  return { id: res.json('id') };
}

export default function (data) {
  const res = http.get(`${BASE}/products/${data.id}`);
  check(res, {
    'status 200': (r) => r.status === 200,
    'stock 정확': (r) => r.status === 200 && r.json('stock') === 777,
  });
}
