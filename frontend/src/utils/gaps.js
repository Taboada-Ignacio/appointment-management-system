/**
 * Gap (brecha) utilities for appointment schedule management.
 * All functions are pure with no side effects.
 */

/**
 * Parse time string 'HH:mm' to minutes since midnight.
 * @param {string} time
 * @returns {number}
 */
export function timeToMinutes(time) {
  if (!time || typeof time !== 'string') return 0;
  const [hours, minutes] = time.split(':').map(Number);
  return (hours || 0) * 60 + (minutes || 0);
}

/**
 * Convert minutes since midnight to 'HH:mm'.
 * @param {number} minutes
 * @returns {string}
 */
export function minutesToTime(minutes) {
  const h = Math.floor(Math.max(0, minutes) / 60);
  const m = Math.max(0, minutes) % 60;
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
}

/**
 * Create an empty gap template.
 * @returns {{ horaInicio: string, horaFin: string }}
 */
export function emptyGap() {
  return { horaInicio: '', horaFin: '' };
}

/**
 * Sort gaps by start time ascending.
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @returns {Array}
 */
export function sortGaps(brechas) {
  return [...brechas].sort((a, b) => timeToMinutes(a.horaInicio) - timeToMinutes(b.horaInicio));
}

/**
 * Check if any gaps overlap.
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @returns {boolean}
 */
export function hasOverlap(brechas) {
  if (!brechas || brechas.length < 2) return false;
  const sorted = sortGaps(brechas);
  for (let i = 1; i < sorted.length; i++) {
    if (timeToMinutes(sorted[i].horaInicio) < timeToMinutes(sorted[i - 1].horaFin)) {
      return true;
    }
  }
  return false;
}

/**
 * Validate an array of gaps.
 * Returns { valid: boolean, errors: string[] }
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @returns {{ valid: boolean, errors: string[] }}
 */
export function validateGaps(brechas) {
  const errors = [];

  if (!brechas || brechas.length === 0) {
    return { valid: true, errors: [] };
  }

  for (let i = 0; i < brechas.length; i++) {
    const gap = brechas[i];
    const num = i + 1;

    if (!gap.horaInicio) {
      errors.push(`Brecha ${num}: falta la hora de inicio.`);
    }
    if (!gap.horaFin) {
      errors.push(`Brecha ${num}: falta la hora de fin.`);
    }

    if (gap.horaInicio && gap.horaFin) {
      if (timeToMinutes(gap.horaInicio) >= timeToMinutes(gap.horaFin)) {
        errors.push(`Brecha ${num}: el inicio (${gap.horaInicio}) debe ser anterior al fin (${gap.horaFin}).`);
      }
    }
  }

  if (hasOverlap(brechas)) {
    errors.push('Hay brechas que se superponen. Ajustá los horarios.');
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

/**
 * Format a time range for display: "09:00 – 13:00"
 * @param {string} horaInicio
 * @param {string} horaFin
 * @returns {string}
 */
export function formatTimeRange(horaInicio, horaFin) {
  return `${horaInicio || '—'} – ${horaFin || '—'}`;
}

/**
 * Calculate total available minutes from gaps.
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @returns {number}
 */
export function totalAvailableMinutes(brechas) {
  if (!brechas || brechas.length === 0) return 0;
  return brechas.reduce((total, gap) => {
    const start = timeToMinutes(gap.horaInicio);
    const end = timeToMinutes(gap.horaFin);
    return total + Math.max(0, end - start);
  }, 0);
}

/**
 * Convert gaps to availability signal segments for the AvailabilitySignal component.
 * Returns array of { start, end, isAvailable } segments covering the full day range.
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @param {string} [dayStart='07:00']
 * @param {string} [dayEnd='21:00']
 * @returns {Array<{start: string, end: string, isAvailable: boolean}>}
 */
export function gapsToSignalSegments(brechas, dayStart = '07:00', dayEnd = '21:00') {
  const startMin = timeToMinutes(dayStart);
  const endMin = timeToMinutes(dayEnd);

  if (!brechas || brechas.length === 0) {
    return [{ start: dayStart, end: dayEnd, isAvailable: false }];
  }

  const sorted = sortGaps(brechas);
  const segments = [];
  let currentMin = startMin;

  for (const gap of sorted) {
    const gapStart = Math.max(timeToMinutes(gap.horaInicio), startMin);
    const gapEnd = Math.min(timeToMinutes(gap.horaFin), endMin);

    if (gapStart > endMin || gapEnd < startMin) continue;

    if (gapStart > currentMin) {
      segments.push({
        start: minutesToTime(currentMin),
        end: minutesToTime(gapStart),
        isAvailable: false,
      });
    }

    if (gapEnd > currentMin) {
      segments.push({
        start: minutesToTime(Math.max(currentMin, gapStart)),
        end: minutesToTime(gapEnd),
        isAvailable: true,
      });
      currentMin = gapEnd;
    }
  }

  if (currentMin < endMin) {
    segments.push({
      start: minutesToTime(currentMin),
      end: minutesToTime(endMin),
      isAvailable: false,
    });
  }

  return segments;
}
