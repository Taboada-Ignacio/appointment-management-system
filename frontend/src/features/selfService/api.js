import { api } from '@/services/api';
import { professionalContext } from '@/config/professional';

const base = '/api/autogestion';
const options = (session) => ({ headers: { Authorization: `Bearer ${session}` } });
export const selfServiceApi = {
  open: (token) => api.post(`${base}/sesiones`, { token }),
  resume: (session) => api.get(`${base}/sesion`, options(session)),
  identify: (session, body) => api.post(`${base}/cliente/identificacion`, body, options(session)),
  register: (session, body) => api.post(`${base}/cliente`, body, options(session)),
  contact: (session, body) => api.put(`${base}/cliente/contacto`, body, options(session)),
  appointments: (session) => api.get(`${base}/turnos`, options(session)),
  config: (session) => api.get(`${base}/configuracion`, options(session)),
  months: (session) => api.get(`${base}/meses`, options(session)),
  days: (session, month) => api.get(`${base}/dias?${new URLSearchParams({ mes: month })}`, options(session)),
  dates: (session, from, to) => api.get(`${base}/fechas?${new URLSearchParams({ desde: from, hasta: to })}`, options(session)),
  gaps: (session, date) => api.get(`${base}/brechas?${new URLSearchParams({ fecha: date })}`, options(session)),
  choose: (session, body) => api.post(`${base}/intervalos`, body, options(session)),
  confirm: (session, key, body) => api.post(`${base}/turnos`, body, { headers: { Authorization: `Bearer ${session}`, 'Idempotency-Key': key } }),
};

const managementPath = () => `/api/profesionales/${professionalContext.id}/enlace-autogestion`;
const privateOptions = () => ({ credentials: 'include' });
export const selfServiceLinkApi = {
  get: () => api.get(managementPath(), privateOptions()),
  create: () => api.post(managementPath(), {}, privateOptions()),
  revoke: () => api.delete(managementPath(), privateOptions()),
};

export function bookingUrl(token) {
  return `${window.location.origin}${import.meta.env.BASE_URL}reservar#${token}`;
}
