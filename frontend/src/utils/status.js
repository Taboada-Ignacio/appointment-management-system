/**
 * Status utilities for agenda entities.
 * Backend states: ACTIVO, INACTIVO, EN_TRANSCURSO, FINALIZADO.
 * null is treated as INACTIVO (backend may send null for newly created entities).
 */

export const STATUS = Object.freeze({
  ACTIVO: 'ACTIVO',
  INACTIVO: 'INACTIVO',
  EN_TRANSCURSO: 'EN_TRANSCURSO',
  FINALIZADO: 'FINALIZADO',
});

export const STATUS_LABELS = Object.freeze({
  ACTIVO: 'Activo',
  INACTIVO: 'Inactivo',
  EN_TRANSCURSO: 'En transcurso',
  FINALIZADO: 'Finalizado',
});

/**
 * Normalize a backend status value. null → INACTIVO.
 * @param {string|null|undefined} status
 * @returns {string}
 */
export function normalizeStatus(status) {
  if (!status) return STATUS.INACTIVO;
  const upper = String(status).toUpperCase();
  return STATUS[upper] ?? STATUS.INACTIVO;
}

/**
 * Resolve a status without replacing backend semantics with a browser-clock
 * interpretation. The current contract uses null for new entities, and the UI
 * must always present that value as INACTIVO.
 *
 * The date and timezone arguments remain for API compatibility with callers;
 * temporal transitions require a future backend-backed contract.
 * @param {string|null|undefined} status - Raw backend status
 * @param {string} _fecha - ISO date string
 * @param {string} _timezone - IANA timezone
 * @returns {string}
 */
export function deriveTemporalStatus(status, _fecha, _timezone) {
  return normalizeStatus(status);
}

/**
 * Check if a month has any configured gaps (brechas).
 * This function deliberately requires the detail DTO (`dias` array). A month
 * summary does not contain enough information to make this decision safely.
 * @param {object} monthDetail - Month detail object with dias array
 * @returns {boolean}
 */
export function isMonthConfigured(monthDetail) {
  if (!monthDetail?.dias || !Array.isArray(monthDetail.dias)) return false;
  return monthDetail.dias.some(day => Number(day.cantidadBrechas ?? 0) > 0);
}

/**
 * Whether a month/day can be activated.
 * Must be INACTIVO and have configuration.
 */
export function canActivate(status, hasConfig) {
  const normalized = normalizeStatus(status);
  return normalized === STATUS.INACTIVO && hasConfig;
}

/**
 * Whether a month/day can be inactivated.
 * Must be ACTIVO.
 */
export function canInactivate(status) {
  return normalizeStatus(status) === STATUS.ACTIVO;
}

/**
 * Whether a status represents a terminal/completed state.
 */
export function isTerminal(status) {
  const normalized = normalizeStatus(status);
  return normalized === STATUS.FINALIZADO;
}
