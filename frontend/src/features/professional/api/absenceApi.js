import { professionalContext } from '@/config/professional';
import { api } from '@/services/api';

const base = `/api/profesionales/${encodeURIComponent(professionalContext.id)}/excepciones-agenda`;
const actor = { headers: { 'X-Usuario': professionalContext.actor } };
const affectedBase = `/api/profesionales/${encodeURIComponent(professionalContext.id)}/turnos-afectados`;

export const previewAbsence = (payload) => api.post(`${base}/preview`, payload, actor);
export const createAbsence = (payload) => api.post(base, payload, actor);
export const previewAbsenceUpdate = (id, payload) => api.post(`${base}/${encodeURIComponent(id)}/preview`, payload, actor);
export const updateAbsence = (id, payload) => api.put(`${base}/${encodeURIComponent(id)}`, payload, actor);
export const listAbsences = ({ desde, hasta, activa } = {}) => {
  const query = new URLSearchParams();
  if (desde) query.set('desde', desde);
  if (hasta) query.set('hasta', hasta);
  if (activa !== undefined) query.set('activa', String(activa));
  return api.get(`${base}${query.size ? `?${query}` : ''}`);
};
export const cancelAbsence = (id) => api.delete(`${base}/${encodeURIComponent(id)}`, actor);
export const listAffectedAppointments = (estado) => api.get(`${affectedBase}${estado ? `?estado=${encodeURIComponent(estado)}` : ''}`);
export const resolveAffectedCancellation = (id, observacion = '') => api.post(`${affectedBase}/${encodeURIComponent(id)}/baja`, { observacion }, actor);
export const resolveAffectedReschedule = (id, payload) => api.post(`${affectedBase}/${encodeURIComponent(id)}/reprogramar`, payload, actor);
export const resolveAffectedBulkCancellation = (afectacionIds, observacion = '') => api.post(`${affectedBase}/baja-masiva`, { afectacionIds, observacion }, actor);
