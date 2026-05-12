import http from 'k6/http';
import { check, sleep } from 'k6';
import { authHeaders, getBaseUrl, login } from './_common.js';

const TOKEN_REAUTH_MS = Number(__ENV.TOKEN_REAUTH_MS || 4 * 60 * 1000);
let adminSession = null;

export const options = {
    stages: [
        { duration: '1m', target: 10 },
        { duration: '2m', target: 20 },
        { duration: '2m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<2000'],
    },
};

export function setup() {
    const baseUrl = getBaseUrl();
    const adminUsername = __ENV.ADMIN_USERNAME || '0900000000';
    const adminPassword = __ENV.ADMIN_PASSWORD || '123456';

    try {
        login(baseUrl, adminUsername, adminPassword, 'admin');
    } catch (error) {
        if (adminUsername === '0900000000' && adminPassword === '123456') {
            http.post(`${baseUrl}/api/auth/init-admin`, '');
            login(baseUrl, adminUsername, adminPassword, 'admin');
        } else {
            throw error;
        }
    }

    return {
        baseUrl,
        adminUsername,
        adminPassword,
    };
}

export default function (data) {
    const session = getAdminSession(data.baseUrl, data.adminUsername, data.adminPassword);
    let responses = requestStatistics(data.baseUrl, session.token);

    if (hasAuthFailure(responses)) {
        adminSession = authenticate(data.baseUrl, data.adminUsername, data.adminPassword);
        responses = requestStatistics(data.baseUrl, adminSession.token);
    }

    check(responses.topSellingRes, {
        'top selling 200': (r) => r.status === 200,
    });
    check(responses.revenueRes, {
        'revenue 200': (r) => r.status === 200,
    });
    check(responses.dailyRevenueRes, {
        'daily revenue 200': (r) => r.status === 200,
    });

    sleep(1);
}

function requestStatistics(baseUrl, token) {
    const headers = authHeaders(token);

    const topSellingRes = http.get(
        `${baseUrl}/api/statistics/top-selling-dishes?limit=10`,
        { headers }
    );

    const revenueRes = http.get(`${baseUrl}/api/statistics/revenue`, { headers });

    const dailyRevenueRes = http.get(`${baseUrl}/api/statistics/revenue/daily`, { headers });

    return {
        topSellingRes,
        revenueRes,
        dailyRevenueRes,
    };
}

function hasAuthFailure(responses) {
    return [responses.topSellingRes, responses.revenueRes, responses.dailyRevenueRes]
        .some((response) => response.status === 401 || response.status === 403);
}

function getAdminSession(baseUrl, username, password) {
    if (!adminSession || Date.now() - adminSession.loggedInAt > TOKEN_REAUTH_MS) {
        adminSession = authenticate(baseUrl, username, password);
    }

    return adminSession;
}

function authenticate(baseUrl, username, password) {
    return {
        token: login(baseUrl, username, password, 'admin'),
        loggedInAt: Date.now(),
    };
}
