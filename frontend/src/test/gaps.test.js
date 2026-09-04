import { describe, it, expect } from 'vitest';
import {
  timeToMinutes,
  minutesToTime,
  emptyGap,
  sortGaps,
  hasOverlap,
  validateGaps,
  formatTimeRange,
  totalAvailableMinutes,
  gapsToSignalSegments,
  subtractGaps,
} from '../utils/gaps';

describe('Gaps Utilities (gaps.js)', () => {
  it('converts HH:mm to minutes and back', () => {
    expect(timeToMinutes('00:00')).toBe(0);
    expect(timeToMinutes('09:30')).toBe(570);
    expect(timeToMinutes('13:45')).toBe(825);
    expect(timeToMinutes('23:59')).toBe(1439);

    expect(minutesToTime(0)).toBe('00:00');
    expect(minutesToTime(570)).toBe('09:30');
    expect(minutesToTime(825)).toBe('13:45');
    expect(minutesToTime(1439)).toBe('23:59');
  });

  it('creates empty gap structure', () => {
    expect(emptyGap()).toEqual({ horaInicio: '', horaFin: '' });
  });

  it('sorts gaps chronologically by horaInicio', () => {
    const unsorted = [
      { horaInicio: '14:00', horaFin: '18:00' },
      { horaInicio: '08:00', horaFin: '12:00' },
      { horaInicio: '12:30', horaFin: '13:30' },
    ];
    const sorted = sortGaps(unsorted);
    expect(sorted[0].horaInicio).toBe('08:00');
    expect(sorted[1].horaInicio).toBe('12:30');
    expect(sorted[2].horaInicio).toBe('14:00');
  });

  it('detects overlapping time ranges', () => {
    const nonOverlapping = [
      { horaInicio: '09:00', horaFin: '13:00' },
      { horaInicio: '14:00', horaFin: '18:00' },
    ];
    expect(hasOverlap(nonOverlapping)).toBe(false);

    const overlapping = [
      { horaInicio: '09:00', horaFin: '13:00' },
      { horaInicio: '12:30', horaFin: '16:00' },
    ];
    expect(hasOverlap(overlapping)).toBe(true);

    const exactTouch = [
      { horaInicio: '09:00', horaFin: '13:00' },
      { horaInicio: '13:00', horaFin: '17:00' },
    ];
    expect(hasOverlap(exactTouch)).toBe(false);
  });

  it('validates gaps array with Spanish messages', () => {
    // Valid gaps
    const valid = [
      { horaInicio: '09:00', horaFin: '13:00' },
      { horaInicio: '14:00', horaFin: '18:00' },
    ];
    const validRes = validateGaps(valid);
    expect(validRes.valid).toBe(true);
    expect(validRes.errors).toHaveLength(0);

    // Inverted start/end
    const inverted = [{ horaInicio: '14:00', horaFin: '10:00' }];
    const invertedRes = validateGaps(inverted);
    expect(invertedRes.valid).toBe(false);
    expect(invertedRes.errors[0]).toContain('debe ser anterior al fin');

    // Overlapping
    const overlap = [
      { horaInicio: '09:00', horaFin: '13:00' },
      { horaInicio: '11:00', horaFin: '15:00' },
    ];
    const overlapRes = validateGaps(overlap);
    expect(overlapRes.valid).toBe(false);
    expect(overlapRes.errors).toContain('Hay brechas que se superponen. Ajustá los horarios.');
  });

  it('formats time range strings for display', () => {
    expect(formatTimeRange('09:00', '13:00')).toBe('09:00 – 13:00');
    expect(formatTimeRange('', '13:00')).toBe('— – 13:00');
  });

  it('computes total available minutes', () => {
    const gaps = [
      { horaInicio: '09:00', horaFin: '13:00' }, // 240 min
      { horaInicio: '14:00', horaFin: '16:30' }, // 150 min
    ];
    expect(totalAvailableMinutes(gaps)).toBe(390);
    expect(totalAvailableMinutes([])).toBe(0);
  });

  it('generates signal segments covering day bounds', () => {
    const gaps = [{ horaInicio: '09:00', horaFin: '12:00' }];
    const segments = gapsToSignalSegments(gaps, '08:00', '14:00');

    expect(segments).toEqual([
      { start: '08:00', end: '09:00', isAvailable: false, isBlocked: false, status: 'inactive' },
      { start: '09:00', end: '12:00', isAvailable: true, isBlocked: false, status: 'available' },
      { start: '12:00', end: '14:00', isAvailable: false, isBlocked: false, status: 'inactive' },
    ]);
  });

  it('subtracts blocked intervals from schedule gaps (subtractGaps)', () => {
    const brechas = [
      { horaInicio: '09:00', horaFin: '13:00' },
      { horaInicio: '14:00', horaFin: '18:00' },
    ];
    const bloqueos = [
      { horaInicio: '10:00', horaFin: '11:00' }, // middle of first gap
      { horaInicio: '17:00', horaFin: '19:00' }, // overlaps end of second gap
    ];

    const result = subtractGaps(brechas, bloqueos);
    expect(result).toEqual([
      { horaInicio: '09:00', horaFin: '10:00' },
      { horaInicio: '11:00', horaFin: '13:00' },
      { horaInicio: '14:00', horaFin: '17:00' },
    ]);
  });

  it('handles complete gap cancellation when block covers entire gap', () => {
    const brechas = [{ horaInicio: '09:00', horaFin: '12:00' }];
    const bloqueos = [{ horaInicio: '08:00', horaFin: '13:00' }];

    expect(subtractGaps(brechas, bloqueos)).toEqual([]);
  });

  it('computes total available minutes discounting blocked intervals', () => {
    const brechas = [{ horaInicio: '09:00', horaFin: '13:00' }]; // 240 min
    const bloqueos = [{ horaInicio: '10:00', horaFin: '11:00' }]; // 60 min blocked
    expect(totalAvailableMinutes(brechas, bloqueos)).toBe(180);
  });

  it('generates availability signal segments reflecting blocked slots and feasible gaps', () => {
    const brechas = [{ horaInicio: '09:00', horaFin: '13:00' }];
    const bloqueos = [{ horaInicio: '10:00', horaFin: '11:00' }];

    const segments = gapsToSignalSegments(brechas, '08:00', '14:00', bloqueos);
    expect(segments).toEqual([
      { start: '08:00', end: '09:00', isAvailable: false, isBlocked: false, status: 'inactive' },
      { start: '09:00', end: '10:00', isAvailable: true, isBlocked: false, status: 'available' },
      { start: '10:00', end: '11:00', isAvailable: false, isBlocked: true, status: 'blocked' },
      { start: '11:00', end: '13:00', isAvailable: true, isBlocked: false, status: 'available' },
      { start: '13:00', end: '14:00', isAvailable: false, isBlocked: false, status: 'inactive' },
    ]);
  });
});

