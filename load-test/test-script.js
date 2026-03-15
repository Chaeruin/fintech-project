import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 500 }, // 30초 동안 500명까지 증가
    { duration: '1m', target: 1000 }, // 1분 동안 1000명 유지
    { duration: '10s', target: 0 },    // 10초 동안 종료
  ],
};

export default function () {
  const url = 'http://localhost:8080/api/v1/payments/confirm';
  const payload = JSON.stringify({
    orderId: `ORD-${Math.random()}`,
    amount: 50000,
    userId: Math.floor(Math.random() * 10000)
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  let res = http.post(url, payload, params);
  check(res, {
      'is status 201 or 200': (r) => r.status === 201 || r.status === 200,
      'transaction time < 500ms': (r) => r.timings.duration < 500,
    });
  sleep(0.1); // 0.1초 대기 후 재요청
}