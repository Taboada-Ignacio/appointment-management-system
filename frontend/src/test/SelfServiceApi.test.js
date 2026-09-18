import { afterEach, expect, it, vi } from 'vitest';
import { api } from '@/services/api';
import { bookingUrl, selfServiceApi, selfServiceLinkApi } from '@/features/selfService/api';

afterEach(() => { vi.restoreAllMocks(); vi.unstubAllEnvs(); });
it('consulta configuración y brechas sin enviar un tipo de atención', async () => {
  const get = vi.spyOn(api, 'get').mockResolvedValue({});
  await selfServiceApi.config('session');
  await selfServiceApi.gaps('session', '2026-09-18');
  expect(get).toHaveBeenCalledWith('/api/autogestion/configuracion', { headers: { Authorization: 'Bearer session' } });
  expect(get).toHaveBeenCalledWith('/api/autogestion/brechas?fecha=2026-09-18', { headers: { Authorization: 'Bearer session' } });
});
it('envía la sesión y la misma clave de idempotencia en la confirmación', async () => {
  const post = vi.spyOn(api, 'post').mockResolvedValue({});
  await selfServiceApi.confirm('session', 'attempt-1', { referencia: 'slot', observaciones: 'Consulta' });
  expect(post).toHaveBeenCalledWith('/api/autogestion/turnos', { referencia: 'slot', observaciones: 'Consulta' }, { headers: { Authorization: 'Bearer session', 'Idempotency-Key': 'attempt-1' } });
});
it('consulta el enlace usando la autenticación del profesional', async () => {
  vi.stubEnv('DEV', false);
  vi.stubEnv('VITE_PROFESSIONAL_DEV_AUTH', 'false');
  const get = vi.spyOn(api, 'get').mockResolvedValue({});
  await selfServiceLinkApi.get();
  expect(get).toHaveBeenCalledWith(expect.stringMatching(/\/api\/profesionales\/\d+\/enlace-autogestion$/), { credentials: 'include' });
});
it('comparte la credencial en el fragmento, sin enviarla como parámetro de URL', () => {
  const url = new URL(bookingUrl('token'));
  expect(url.pathname).toBe('/reservar'); expect(url.hash).toBe('#token'); expect(url.search).toBe('');
});
it('consulta exclusivamente el mes indicado usando la sesión del cliente', async () => {
  const get = vi.spyOn(api, 'get').mockResolvedValue([]);
  await selfServiceApi.days('session', '2026-09');
  expect(get).toHaveBeenCalledWith('/api/autogestion/dias?mes=2026-09', { headers: { Authorization: 'Bearer session' } });
});
