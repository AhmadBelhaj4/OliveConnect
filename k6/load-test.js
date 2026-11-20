import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '15s', target: 50 },
    { duration: '45s', target: 100 },   // 100 connexions simultanées (exigence sujet)
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],     // < 1 % d'erreur
    http_req_duration: ['p(95)<2000'],  // objectif interne < 2 s (sujet < 10 s)
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export function setup() {
  const res = http.post(`${BASE}/auth/login`, JSON.stringify({
    email: 'collection@example.test',
    password: 'Secret#123',
  }), { headers: { 'Content-Type': 'application/json' } });
  return { token: res.json('accessToken') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  // 1. Carte publique (bbox Sfax)
  const rMap = http.get(`${BASE}/orchards/map?bbox=10.5,34.5,11.0,35.0`);
  check(rMap, { 'map 200': (r) => r.status === 200 });

  // 2. File des demandes paginée
  const rQueue = http.get(`${BASE}/harvest-requests?status=SUBMITTED&page=0&size=20`, { headers });
  check(rQueue, { 'queue 200': (r) => r.status === 200 });

  // 3. Dashboard summary (caché 90 s dans Redis)
  const rDash = http.get(`${BASE}/dashboard/summary?from=2026-01-01&to=2026-12-31`, { headers });
  check(rDash, { 'dashboard 200': (r) => r.status === 200 });

  sleep(0.5);
}
