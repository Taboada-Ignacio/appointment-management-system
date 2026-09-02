// Empty by default so local development uses Vite's same-origin `/api` proxy.
// Deployments can still provide an absolute VITE_API_URL when the API lives elsewhere.
const BASE_URL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');

class ApiError extends Error {
  constructor(message, status, data, cause) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;

    if (cause) {
      this.cause = cause;
    }
  }
}

function hasHeader(headers, name) {
  return Object.keys(headers).some((header) => header.toLowerCase() === name.toLowerCase());
}

async function readResponseBody(response) {
  if (response.status === 204 || response.status === 205) {
    return null;
  }

  const body = await response.text();
  if (!body.trim()) {
    return null;
  }

  try {
    return JSON.parse(body);
  } catch {
    return body;
  }
}

/**
 * @param {string} endpoint
 * @param {RequestInit & { body?: unknown }} [options]
 */
async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
  const headers = {
    Accept: 'application/json',
    ...options.headers,
  };

  const config = /** @type {RequestInit & { body?: unknown }} */ ({
    ...options,
    headers,
  });

  if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
    if (!hasHeader(headers, 'Content-Type')) {
      headers['Content-Type'] = 'application/json';
    }
    config.body = JSON.stringify(config.body);
  }

  let response;
  try {
    response = await fetch(url, config);
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    throw new ApiError(
      'No se pudo conectar con el servidor.',
      0,
      null,
      error,
    );
  }

  const responseData = await readResponseBody(response);

  if (!response.ok) {
    const errorData = responseData && typeof responseData === 'object'
      ? responseData
      : { message: responseData || response.statusText };

    throw new ApiError(
      errorData.message || `Error en la petición: ${response.status}`,
      response.status,
      errorData,
    );
  }

  return responseData;
}

export const api = {
  get: (endpoint, options) => request(endpoint, { ...options, method: 'GET' }),
  post: (endpoint, body, options) => request(endpoint, { ...options, method: 'POST', body }),
  put: (endpoint, body, options) => request(endpoint, { ...options, method: 'PUT', body }),
  patch: (endpoint, body, options) => request(endpoint, { ...options, method: 'PATCH', body }),
  delete: (endpoint, options) => request(endpoint, { ...options, method: 'DELETE' }),
};

export { ApiError, BASE_URL, request };

