import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '1m', target: 50  },
        { duration: '2m', target: 100 },
        { duration: '3m', target: 100 },
        { duration: '1m', target: 0   },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed:   ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080';
const JOKE_ID  = '1d32febf-02cf-4c55-9795-753c454f8d2f';

export default function () {

    const searchResponse = http.get(`${BASE_URL}/api/jokes?page=0&size=10`);
    check(searchResponse, {
        'search status 200':   (r) => r.status === 200,
        'search under 200ms':  (r) => r.timings.duration < 200,
    });

    const jokeResponse = http.get(`${BASE_URL}/api/jokes/${JOKE_ID}`);
    check(jokeResponse, {
        'joke fetch status 200': (r) => r.status === 200,
        'joke fetch under 50ms': (r) => r.timings.duration < 50,
    });

    const categoryResponse = http.get(
        `${BASE_URL}/api/jokes/category/Animal%20Jokes?page=0&size=10`
    );
    check(categoryResponse, {
        'category status 200':  (r) => r.status === 200,
        'category under 300ms': (r) => r.timings.duration < 300,
    });

    const randomResponse = http.get(`${BASE_URL}/api/jokes/random`);
    check(randomResponse, {
        'random status 200': (r) => r.status === 200,
    });

    sleep(1);
}