import { professionalContext } from '../../../config/professional';
import { api } from '../../../services/api';

const professionalPath = `/api/profesionales/${encodeURIComponent(professionalContext.id)}`;
const actorOptions = Object.freeze({
  headers: Object.freeze({ 'X-Usuario': professionalContext.actor }),
});

function monthPath(monthAgendaId) {
  return `${professionalPath}/meses-agenda/${encodeURIComponent(monthAgendaId)}`;
}

function dayPath(dayAgendaId) {
  return `${professionalPath}/dias-agenda/${encodeURIComponent(dayAgendaId)}`;
}

function appointmentPath(appointmentId) {
  return `${professionalPath}/turnos/${encodeURIComponent(appointmentId)}`;
}

export function listAnnualAgendas() {
  return api.get(`${professionalPath}/agendas`);
}

export function createAnnualAgenda(anio) {
  return api.post(`${professionalPath}/agendas`, { anio }, actorOptions);
}

export function listMonths(anio) {
  return api.get(`${professionalPath}/agendas/${encodeURIComponent(anio)}/meses`);
}

export function getMonth(monthAgendaId) {
  return api.get(monthPath(monthAgendaId));
}

export function generateMonthDays(monthAgendaId) {
  return api.post(`${monthPath(monthAgendaId)}/dias`);
}

export function configureMonthWeek(monthAgendaId, diasSemana) {
  return api.post(`${monthPath(monthAgendaId)}/modo-semana`, { diasSemana }, actorOptions);
}

export function configureMonthDates(monthAgendaId, dias) {
  return api.post(`${monthPath(monthAgendaId)}/modo-mes`, { dias }, actorOptions);
}

export function activateMonth(monthAgendaId) {
  return api.post(`${monthPath(monthAgendaId)}/activar`, undefined, actorOptions);
}

export function deactivateMonth(monthAgendaId) {
  return api.post(`${monthPath(monthAgendaId)}/inactivar`, undefined, actorOptions);
}

export function setRepeatMonth(monthAgendaId, repetirConfiguracion) {
  return api.put(`${monthPath(monthAgendaId)}/repetir-configuracion`, { repetirConfiguracion });
}

export function repeatMonth(monthAgendaId) {
  return api.post(`${monthPath(monthAgendaId)}/repetir`);
}

/** @param {{ desde?: string, hasta?: string }} [range] */
export function listSelectableDays(range = {}) {
  const { desde, hasta } = range;
  const search = new URLSearchParams();
  if (desde) search.set('desde', desde);
  if (hasta) search.set('hasta', hasta);

  const query = search.toString();
  return api.get(`${professionalPath}/dias-agenda/seleccionables${query ? `?${query}` : ''}`);
}

export function getDay(dayAgendaId) {
  return api.get(dayPath(dayAgendaId));
}

export function configureDay(dayAgendaId, brechas) {
  return api.put(`${dayPath(dayAgendaId)}/brechas`, { brechas }, actorOptions);
}

export function cancelAppointment(appointmentId, motivo) {
  const body = motivo === undefined ? undefined : { motivo };
  return api.post(`${appointmentPath(appointmentId)}/cancelacion`, body, actorOptions);
}

export function deactivateAppointment(appointmentId, motivo) {
  return api.post(`${appointmentPath(appointmentId)}/baja`, { motivo }, actorOptions);
}

export const agendaApi = Object.freeze({
  listAnnualAgendas,
  createAnnualAgenda,
  listMonths,
  getMonth,
  generateMonthDays,
  configureMonthWeek,
  configureMonthDates,
  activateMonth,
  deactivateMonth,
  setRepeatMonth,
  repeatMonth,
  listSelectableDays,
  getDay,
  configureDay,
  cancelAppointment,
  deactivateAppointment,
});

export default agendaApi;
