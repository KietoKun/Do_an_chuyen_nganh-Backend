import http from 'k6/http';
import { check } from 'k6';

export function getBaseUrl() {
    return (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
}

export function authHeaders(token) {
    const headers = {
        'Content-Type': 'application/json',
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    return headers;
}

export function login(baseUrl, username, password, label) {
    if (!username || !password) {
        throw new Error(`${label} credentials are required`);
    }

    const res = http.post(
        `${baseUrl}/api/auth/login`,
        JSON.stringify({ username, password }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    check(res, {
        [`${label} login status 200`]: (r) => r.status === 200,
    });

    if (res.status !== 200) {
        throw new Error(`${label} login failed with status ${res.status}`);
    }

    let token;
    try {
        token = res.json('accessToken');
    } catch (error) {
        throw new Error(`${label} login response was not valid JSON: ${error.message}`);
    }

    if (!token) {
        throw new Error(`${label} login did not return accessToken`);
    }

    return token;
}

export function fetchMenu(baseUrl) {
    const res = http.get(`${baseUrl}/api/dishes`);

    check(res, {
        'menu status 200': (r) => r.status === 200,
    });

    if (res.status !== 200) {
        throw new Error(`menu request failed with status ${res.status}`);
    }

    let data;
    try {
        data = res.json();
    } catch (error) {
        throw new Error(`menu response was not valid JSON: ${error.message}`);
    }

    if (!Array.isArray(data) || data.length === 0) {
        throw new Error('menu response is empty');
    }

    return data;
}

export function pickUsableVariantId(baseUrl) {
    const menu = fetchMenu(baseUrl);

    let fallbackVariantId = null;

    for (const dish of menu) {
        if (!dish || !Array.isArray(dish.variants)) {
            continue;
        }

        for (const variant of dish.variants) {
            if (!variant || variant.id == null) {
                continue;
            }

            if (fallbackVariantId == null) {
                fallbackVariantId = variant.id;
            }

            if ((variant.maxQuantity ?? 0) > 0) {
                return variant.id;
            }
        }
    }

    if (fallbackVariantId != null) {
        return fallbackVariantId;
    }

    throw new Error('no dish variant found in menu response');
}

export function pickFirstDishId(baseUrl) {
    const menu = fetchMenu(baseUrl);

    for (const dish of menu) {
        if (dish && dish.id != null) {
            return dish.id;
        }
    }

    throw new Error('no dish found in menu response');
}
