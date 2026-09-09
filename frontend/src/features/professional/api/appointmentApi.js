import { professionalContext } from '../../../config/professional';
import { api } from '../../../services/api';

const base = `/api/profesionales/${encodeURIComponent(professionalContext.id)}`;
const actor = { headers: { 'X-Usuario': professionalContext.actor } };

export async function searchClients(query) {
  const value = query.trim();
  if (/^\d+$/.test(value)) {
    return api.get(`${base}/clientes?${new URLSearchParams({ size: '20', dni: value })}`);
  }
  const [byName, bySurname] = await Promise.all([
    api.get(`${base}/clientes?${new URLSearchParams({ size: '20', nombre: value })}`),
    api.get(`${base}/clientes?${new URLSearchParams({ size: '20', apellido: value })}`),
  ]);
  const rows = [...(byName?.content || []), ...(bySurname?.content || [])];
  return { content: [...new Map(rows.map((client) => [client.id, client])).values()] };
}

export function getAppointmentConfiguration() {
  return api.get(`${base}/configuracion`);
}

export function listAppointmentDays(desde, hasta) {
  return api.get(`${base}/dias-agenda/seleccionables?desde=${desde}&hasta=${hasta}`);
}

export function getAppointmentDay(diaAgendaId) {
  return api.get(`${base}/dias-agenda/${encodeURIComponent(diaAgendaId)}`);
}

export function listSuggestedTimes(fecha, tipoAtencionId = null) {
  const params = new URLSearchParams({ fecha });
  if (tipoAtencionId) params.set('tipoAtencionId', tipoAtencionId);
  return api.get(`${base}/turnos/horarios-sugeridos?${params}`);
}

export function validateManualAppointment(payload) {
  return api.post(`${base}/turnos/validar`, payload, actor);
}

export function createManualAppointment(payload) {
  return api.post(`${base}/turnos`, payload, actor);
}
