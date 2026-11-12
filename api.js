/**
 * Thin Fetch API wrapper. Every call returns parsed JSON or throws ApiError.
 * The JWT (or opaque session token) returned by POST /auth/login is sent as a Bearer token.
 */
class ApiError extends Error {
  constructor(message, status = 0, details = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

const Api = (() => {
  const cfg = window.APP_CONFIG;
  const TOKEN_KEY = 'ch_token';
  let token = sessionStorage.getItem(TOKEN_KEY);
  let unauthorizedHandler = () => {};

  async function request(method, path, body) {
    if (cfg.USE_MOCK) {
      try {
        return await MockServer.handle(method, path, body, token);
      } catch (e) {
        if (e.status === 401) unauthorizedHandler();
        throw e;
      }
    }

    let res;
    try {
      res = await fetch(cfg.API_BASE + path, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          ...(token ? { Authorization: 'Bearer ' + token } : {})
        },
        body: body === undefined ? undefined : JSON.stringify(body)
      });
    } catch (err) {
      throw new ApiError('Cannot reach the server. Check your connection and that the backend is running.');
    }

    let data = null;
    const text = await res.text();
    if (text) { try { data = JSON.parse(text); } catch (_) { data = { message: text }; } }

    if (!res.ok) {
      if (res.status === 401 && path !== '/auth/login') unauthorizedHandler();
      throw new ApiError(
        (data && (data.message || data.error)) || `Request failed (${res.status})`,
        res.status,
        data
      );
    }
    return data;
  }

  return {
    get: (p) => request('GET', p),
    post: (p, b) => request('POST', p, b ?? {}),
    put: (p, b) => request('PUT', p, b ?? {}),
    patch: (p, b) => request('PATCH', p, b ?? {}),
    del: (p) => request('DELETE', p),
    setToken(t) {
      token = t;
      if (t) sessionStorage.setItem(TOKEN_KEY, t); else sessionStorage.removeItem(TOKEN_KEY);
    },
    hasToken: () => !!token,
    onUnauthorized(fn) { unauthorizedHandler = fn; }
  };
})();