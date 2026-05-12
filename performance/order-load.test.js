import http from 'k6/http';
import { check, sleep } from 'k6';
import { authHeaders, getBaseUrl, login, pickUsableVariantId } from './_common.js';

const TOKEN_REAUTH_MS = Number(__ENV.TOKEN_REAUTH_MS || 4 * 60 * 1000);
let customerSession = null;
const customerAccountsRaw = __ENV.CUSTOMER_ACCOUNTS_PATH
    ? open(__ENV.CUSTOMER_ACCOUNTS_PATH)
    : __ENV.CUSTOMER_ACCOUNTS_JSON || '';

export const options = {
    stages: [
        { duration: '1m', target: 20 },
        { duration: '2m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<3000'],
    },
};

export function setup() {
    const baseUrl = getBaseUrl();
    const customerAccounts = parseCustomerAccounts();
    const variantId = __ENV.ORDER_VARIANT_ID ? Number(__ENV.ORDER_VARIANT_ID) : pickUsableVariantId(baseUrl);

    return {
        baseUrl,
        variantId,
        deliveryMethod: __ENV.DELIVERY_METHOD || 'TAKEAWAY',
        customerAccounts,
    };
}

export default function (data) {
    const account = selectCustomerAccount(data.customerAccounts);
    const session = getCustomerSession(data.baseUrl, account);

    const body = {
        note: 'k6 load test',
        items: [
            {
                variantId: data.variantId,
                quantity: 1,
                toppingIds: [],
            },
        ],
        deliveryMethod: data.deliveryMethod,
    };

    const res = http.post(
        `${data.baseUrl}/api/orders`,
        JSON.stringify(body),
        { headers: authHeaders(session.token) }
    );

    if ((res.status === 401 || res.status === 403) && account.username && account.password) {
        customerSession = authenticate(data.baseUrl, account);
        const retryRes = http.post(
            `${data.baseUrl}/api/orders`,
            JSON.stringify(body),
            { headers: authHeaders(customerSession.token) }
        );

        if (retryRes.status !== 200) {
            console.error(`order failed: status=${retryRes.status}, body=${retryRes.body}`);
            console.error(`request body: ${JSON.stringify(body)}`);
        }

        check(retryRes, {
            'order status 200': (r) => r.status === 200,
        });
        sleep(1);
        return;
    }

    if (res.status !== 200) {
        console.error(`order failed: status=${res.status}, body=${res.body}`);
        console.error(`request body: ${JSON.stringify(body)}`);
    }

    check(res, {
        'order status 200': (r) => r.status === 200,
    });

    sleep(1);
}

function parseCustomerAccounts() {
    if (customerAccountsRaw) {
        const accounts = JSON.parse(customerAccountsRaw);
        if (!Array.isArray(accounts) || accounts.length === 0) {
            throw new Error('CUSTOMER_ACCOUNTS_PATH or CUSTOMER_ACCOUNTS_JSON must be a non-empty JSON array');
        }
        return accounts;
    }

    if (!__ENV.CUSTOMER_USERNAME || !__ENV.CUSTOMER_PASSWORD) {
        throw new Error('CUSTOMER_USERNAME and CUSTOMER_PASSWORD are required when CUSTOMER_ACCOUNTS_JSON is not set');
    }

    return [
        {
            username: __ENV.CUSTOMER_USERNAME,
            password: __ENV.CUSTOMER_PASSWORD,
        },
    ];
}

function selectCustomerAccount(accounts) {
    const index = ((__VU || 1) - 1) % accounts.length;
    return accounts[index];
}

function getCustomerSession(baseUrl, account) {
    if (!customerSession || customerSession.username !== account.username || Date.now() - customerSession.loggedInAt > TOKEN_REAUTH_MS) {
        customerSession = authenticate(baseUrl, account);
    }

    return customerSession;
}

function authenticate(baseUrl, account) {
    const token = login(baseUrl, account.username, account.password, 'customer');
    return {
        username: account.username,
        token,
        loggedInAt: Date.now(),
    };
}
