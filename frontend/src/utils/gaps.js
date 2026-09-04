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
 * Format a time string to 'HH:mm', safely handling 'HH:mm:ss'.
 * @param {string} time
 * @returns {string}
 */
export function formatTime(time) {
  if (!time || typeof time !== 'string') return '';
  const trimmed = time.trim();
  if (trimmed.length >= 5) {
    return trimmed.slice(0, 5);
  }
  return trimmed;
}

/**
 * Format a time range for display: "09:00 – 13:00"
 * @param {string} horaInicio
 * @param {string} horaFin
 * @returns {string}
 */
export function formatTimeRange(horaInicio, horaFin) {
  const start = formatTime(horaInicio);
  const end = formatTime(horaFin);
  return `${start || '—'} – ${end || '—'}`;
}

/**
 * Subtract blocked time intervals (bloqueos) from schedule gaps (brechas).
 * Returns the effective available intervals where appointments can actually be booked.
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @param {Array<{horaInicio: string, horaFin: string}>} [bloqueos=[]]
 * @returns {Array<{horaInicio: string, horaFin: string}>}
 */
export function subtractGaps(brechas = [], bloqueos = []) {
  if (!brechas || brechas.length === 0) return [];
  if (!bloqueos || bloqueos.length === 0) return sortGaps(brechas);

  // Filter and merge overlapping/adjacent bloqueos
  const validBloqueos = bloqueos
    .filter((b) => b && b.horaInicio && b.horaFin)
    .map((b) => ({
      start: timeToMinutes(b.horaInicio),
      end: timeToMinutes(b.horaFin),
    }))
    .filter((b) => b.end > b.start)
    .sort((a, b) => a.start - b.start);

  if (validBloqueos.length === 0) return sortGaps(brechas);

  const mergedBloqueos = [];
  for (const b of validBloqueos) {
    if (mergedBloqueos.length === 0) {
      mergedBloqueos.push({ ...b });
    } else {
      const prev = mergedBloqueos[mergedBloqueos.length - 1];
      if (b.start <= prev.end) {
        prev.end = Math.max(prev.end, b.end);
      } else {
        mergedBloqueos.push({ ...b });
      }
    }
  }

  const sortedBrechas = sortGaps(brechas);
  const result = [];

  for (const brecha of sortedBrechas) {
    const bStart = timeToMinutes(brecha.horaInicio);
    const bEnd = timeToMinutes(brecha.horaFin);
    if (bEnd <= bStart) continue;

    let cursor = bStart;

    for (const bloqueo of mergedBloqueos) {
      if (bloqueo.end <= cursor) continue;
      if (bloqueo.start >= bEnd) break;

      if (cursor < bloqueo.start) {
        const segEnd = Math.min(bloqueo.start, bEnd);
        if (segEnd > cursor) {
          result.push({
            horaInicio: minutesToTime(cursor),
            horaFin: minutesToTime(segEnd),
          });
        }
      }

      if (bloqueo.end > cursor) {
        cursor = bloqueo.end;
      }

      if (cursor >= bEnd) break;
    }

    if (cursor < bEnd) {
      result.push({
        horaInicio: minutesToTime(cursor),
        horaFin: minutesToTime(bEnd),
      });
    }
  }

  return result;
}

/**
 * Calculate total available minutes from gaps, optionally discounting blocks.
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @param {Array<{horaInicio: string, horaFin: string}>} [bloqueos=[]]
 * @returns {number}
 */
export function totalAvailableMinutes(brechas, bloqueos = []) {
  if (!brechas || brechas.length === 0) return 0;
  const effectiveGaps = bloqueos && bloqueos.length > 0 ? subtractGaps(brechas, bloqueos) : brechas;
  return effectiveGaps.reduce((total, gap) => {
    const start = timeToMinutes(gap.horaInicio);
    const end = timeToMinutes(gap.horaFin);
    return total + Math.max(0, end - start);
  }, 0);
}

/**
 * Convert gaps and schedule blocks to availability signal segments for the AvailabilitySignal component.
 * Returns array of { start, end, isAvailable, isBlocked, status } segments covering the full day range.
 * @param {Array<{horaInicio: string, horaFin: string}>} brechas
 * @param {string} [dayStart='07:00']
 * @param {string} [dayEnd='21:00']
 * @param {Array<{horaInicio: string, horaFin: string}>} [bloqueos=[]]
 * @returns {Array<{start: string, end: string, isAvailable: boolean, isBlocked: boolean, status: string}>}
 */
export function gapsToSignalSegments(brechas, dayStart = '07:00', dayEnd = '21:00', bloqueos = []) {
  const startMin = timeToMinutes(dayStart);
  const endMin = timeToMinutes(dayEnd);

  if (endMin <= startMin) return [];

  const effectiveGaps = subtractGaps(brechas, bloqueos);

  const validBloqueos = (bloqueos || [])
    .filter((b) => b && b.horaInicio && b.horaFin)
    .map((b) => ({
      start: Math.max(timeToMinutes(b.horaInicio), startMin),
      end: Math.min(timeToMinutes(b.horaFin), endMin),
    }))
    .filter((b) => b.end > b.start)
    .sort((a, b) => a.start - b.start);

  const mergedBloqueos = [];
  for (const b of validBloqueos) {
    if (mergedBloqueos.length === 0) {
      mergedBloqueos.push({ ...b });
    } else {
      const prev = mergedBloqueos[mergedBloqueos.length - 1];
      if (b.start <= prev.end) {
        prev.end = Math.max(prev.end, b.end);
      } else {
        mergedBloqueos.push({ ...b });
      }
    }
  }

  const markedIntervals = [];

  for (const gap of effectiveGaps) {
    const gStart = Math.max(timeToMinutes(gap.horaInicio), startMin);
    const gEnd = Math.min(timeToMinutes(gap.horaFin), endMin);
    if (gEnd > gStart) {
      markedIntervals.push({
        start: gStart,
        end: gEnd,
        status: 'available',
      });
    }
  }

  for (const b of mergedBloqueos) {
    if (b.end > b.start) {
      markedIntervals.push({
        start: b.start,
        end: b.end,
        status: 'blocked',
      });
    }
  }

  markedIntervals.sort((a, b) => a.start - b.start);

  const segments = [];
  let currentMin = startMin;

  for (const interval of markedIntervals) {
    if (interval.start > currentMin) {
      segments.push({
        start: minutesToTime(currentMin),
        end: minutesToTime(interval.start),
        isAvailable: false,
        isBlocked: false,
        status: 'inactive',
      });
    }

    if (interval.end > currentMin) {
      const effectiveStart = Math.max(currentMin, interval.start);
      if (interval.end > effectiveStart) {
        segments.push({
          start: minutesToTime(effectiveStart),
          end: minutesToTime(interval.end),
          isAvailable: interval.status === 'available',
          isBlocked: interval.status === 'blocked',
          status: interval.status,
        });
      }
      currentMin = Math.max(currentMin, interval.end);
    }
  }

  if (currentMin < endMin) {
    segments.push({
      start: minutesToTime(currentMin),
      end: minutesToTime(endMin),
      isAvailable: false,
      isBlocked: false,
      status: 'inactive',
    });
  }

  return segments;
}
