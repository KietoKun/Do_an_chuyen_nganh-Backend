import http from 'k6/http';
import { check, sleep } from 'k6';
import { getBaseUrl, pickFirstDishId } from './_common.js';

export const options = {
    stages: [
        { duration: '1m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 500 },
        { duration: '2m', target: 1000 },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<1000'],
    },
};

export function setup() {
    const baseUrl = getBaseUrl();
    const dishId = pickFirstDishId(baseUrl);

    return {
        baseUrl,
        dishId,
    };
}

export default function (data) {
    const menuRes = http.get(`${data.baseUrl}/api/dishes`);
    check(menuRes, {
        'menu request 200': (r) => r.status === 200,
    });

    const detailRes = http.get(`${data.baseUrl}/api/dishes/${data.dishId}`);
    check(detailRes, {
        'dish detail 200': (r) => r.status === 200,
    });

    sleep(1);
}
