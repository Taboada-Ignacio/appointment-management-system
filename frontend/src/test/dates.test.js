import { describe, it, expect } from 'vitest';
import {
  getTodayInTimezone,
  getCurrentYearMonth,
  toDateString,
  parseDateString,
  isToday,
  isPast,
  isFuture,
  getDaysInMonth,
  getFirstDayOfWeek,
  formatMonthYear,
  formatDateLong,
  getMonthRange,
  addDays,
} from '../utils/dates';

describe('Dates Utilities (dates.js)', () => {
  const timezone = 'America/Argentina/Buenos_Aires';

  it('returns ISO date string for getTodayInTimezone', () => {
    const today = getTodayInTimezone(timezone);
    expect(today).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('returns year and month numbers for getCurrentYearMonth', () => {
    const ym = getCurrentYearMonth(timezone);
    expect(ym.year).toBeGreaterThanOrEqual(2025);
    expect(ym.month).toBeGreaterThanOrEqual(1);
    expect(ym.month).toBeLessThanOrEqual(12);
  });

  it('formats year, month, day to YYYY-MM-DD', () => {
    expect(toDateString(2026, 9, 15)).toBe('2026-09-15');
    expect(toDateString(2026, 1, 5)).toBe('2026-01-05');
  });

  it('parses YYYY-MM-DD to components', () => {
    expect(parseDateString('2026-09-15')).toEqual({ year: 2026, month: 9, day: 15 });
  });

  it('calculates isToday, isPast, isFuture accurately', () => {
    const today = getTodayInTimezone(timezone);
    expect(isToday(today, timezone)).toBe(true);
    expect(isToday('2020-01-01', timezone)).toBe(false);

    expect(isPast('2020-01-01', timezone)).toBe(true);
    expect(isPast('2099-01-01', timezone)).toBe(false);

    expect(isFuture('2099-01-01', timezone)).toBe(true);
    expect(isFuture('2020-01-01', timezone)).toBe(false);
  });

  it('computes days in month accurately including leap years', () => {
    expect(getDaysInMonth(2026, 1)).toBe(31);
    expect(getDaysInMonth(2026, 2)).toBe(28);
    expect(getDaysInMonth(2024, 2)).toBe(29); // Leap year
    expect(getDaysInMonth(2026, 4)).toBe(30);
  });

  it('computes Monday-based first day of week (0=Mon, 6=Sun)', () => {
    // 2026-09-01 was a Tuesday -> index 1
    expect(getFirstDayOfWeek(2026, 9)).toBe(1);
  });

  it('formats month and year in Spanish', () => {
    expect(formatMonthYear(9, 2026)).toBe('Septiembre 2026');
    expect(formatMonthYear(1, 2026)).toBe('Enero 2026');
  });

  it('formats long date with Spanish day and month name', () => {
    expect(formatDateLong('2026-09-15')).toBe('Martes 15 de septiembre de 2026');
  });

  it('computes month start and end dates', () => {
    const range = getMonthRange(2026, 2);
    expect(range.firstDay).toBe('2026-02-01');
    expect(range.lastDay).toBe('2026-02-28');
  });

  it('adds and subtracts days', () => {
    expect(addDays('2026-09-15', 1)).toBe('2026-09-16');
    expect(addDays('2026-09-15', -1)).toBe('2026-09-14');
  });
});

