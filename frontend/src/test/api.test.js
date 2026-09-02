import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { api, ApiError } from '../services/api';

describe('HTTP API Client (api.js)', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('handles standard JSON 200 response', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      text: () => Promise.resolve(JSON.stringify({ id: 1, anio: 2026 })),
    });

    const result = await api.get('/profesionales/1/agendas');
    expect(result).toEqual({ id: 1, anio: 2026 });
    expect(global.fetch).toHaveBeenCalledWith(
      '/profesionales/1/agendas',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ Accept: 'application/json' }),
      })
    );
  });

  it('handles 204 No Content with null return', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      headers: new Headers(),
      text: () => Promise.resolve(''),
    });

    const result = await api.post('/profesionales/1/meses-agenda/5/activar');
    expect(result).toBeNull();
  });

  it('handles 200 with empty body as null', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      text: () => Promise.resolve(''),
    });

    const result = await api.post('/profesionales/1/meses-agenda/5/activar');
    expect(result).toBeNull();
  });

  it('converts network connection errors to ApiError with status 0', async () => {
    global.fetch = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(api.get('/test')).rejects.toThrow(ApiError);
    await expect(api.get('/test')).rejects.toMatchObject({
      name: 'ApiError',
      status: 0,
      message: 'No se pudo conectar con el servidor.',
    });
  });

  it('converts HTTP error responses with JSON body to ApiError', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      headers: new Headers({ 'content-type': 'application/json' }),
      text: () => Promise.resolve(JSON.stringify({ message: 'Agenda duplicada' })),
    });

    await expect(api.post('/profesionales/1/agendas', { anio: 2026 })).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      message: 'Agenda duplicada',
      data: { message: 'Agenda duplicada' },
    });
  });

  it('converts HTTP error responses with plain text body to ApiError', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
      headers: new Headers({ 'content-type': 'text/plain' }),
      text: () => Promise.resolve('Internal server exception occurred'),
    });

    await expect(api.get('/error')).rejects.toMatchObject({
      name: 'ApiError',
      status: 500,
      message: 'Internal server exception occurred',
    });
  });

  it('does NOT attach Content-Type header on GET requests without body', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      text: () => Promise.resolve('[]'),
    });

    await api.get('/profesionales/1/agendas');
    const calledHeaders = global.fetch.mock.calls[0][1].headers;
    expect(calledHeaders['Content-Type']).toBeUndefined();
  });

  it('attaches Content-Type: application/json when sending JSON body', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      headers: new Headers({ 'content-type': 'application/json' }),
      text: () => Promise.resolve('{"id": 1}'),
    });

    await api.post('/profesionales/1/agendas', { anio: 2026 });
    const calledHeaders = global.fetch.mock.calls[0][1].headers;
    expect(calledHeaders['Content-Type']).toBe('application/json');
  });

  it('passes custom X-Usuario headers correctly', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      text: () => Promise.resolve('{}'),
    });

    await api.post(
      '/profesionales/1/agendas',
      { anio: 2026 },
      { headers: { 'X-Usuario': 'Dr. Pérez' } }
    );
    const calledHeaders = global.fetch.mock.calls[0][1].headers;
    expect(calledHeaders['X-Usuario']).toBe('Dr. Pérez');
  });
});
