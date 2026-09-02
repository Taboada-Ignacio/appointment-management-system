import { professionalContext } from '../config/professional';

export const DEFAULT_TIMEZONE = professionalContext?.timezone || 'America/Argentina/Buenos_Aires';

export const DAY_NAMES = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
export const DAY_NAMES_SHORT = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
export const MONTH_NAMES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
];
export const BACKEND_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

function getTodayParts(timezone = DEFAULT_TIMEZONE) {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone || DEFAULT_TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());

  return {
    year: parts.find((p) => p.type === 'year')?.value || '2026',
    month: parts.find((p) => p.type === 'month')?.value || '01',
    day: parts.find((p) => p.type === 'day')?.value || '01',
  };
}

export function getTodayInTimezone(timezone = DEFAULT_TIMEZONE) {
  const { year, month, day } = getTodayParts(timezone);
  return `${year}-${month}-${day}`;
}

export function getCurrentYearMonth(timezone = DEFAULT_TIMEZONE) {
  const { year, month } = getTodayParts(timezone);
  return { year: parseInt(year, 10), month: parseInt(month, 10) };
}

export function toDateString(year, month, day) {
  if (year instanceof Date) {
    const y = year.getFullYear();
    const m = String(year.getMonth() + 1).padStart(2, '0');
    const d = String(year.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
  const y = String(year).padStart(4, '0');
  const m = String(month).padStart(2, '0');
  const d = String(day).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function parseDateString(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') {
    return { year: 2026, month: 1, day: 1 };
  }
  const [year, month, day] = dateStr.split('-');
  return {
    year: parseInt(year, 10) || 2026,
    month: parseInt(month, 10) || 1,
    day: parseInt(day, 10) || 1,
  };
}

export function isToday(dateStr, timezone = DEFAULT_TIMEZONE) {
  if (!dateStr) return false;
  return dateStr === getTodayInTimezone(timezone);
}

export function isPast(dateStr, timezone = DEFAULT_TIMEZONE) {
  if (!dateStr) return false;
  return dateStr < getTodayInTimezone(timezone);
}

export function isFuture(dateStr, timezone = DEFAULT_TIMEZONE) {
  if (!dateStr) return false;
  return dateStr > getTodayInTimezone(timezone);
}

export function getDaysInMonth(year, month) {
  return new Date(year, month, 0).getDate();
}

export function getFirstDayOfWeek(year, month) {
  const day = new Date(year, month - 1, 1).getDay();
  // Convert Sunday=0, Monday=1 to Monday=0, Sunday=6
  return day === 0 ? 6 : day - 1;
}

export function formatMonthYear(month, year) {
  const mIndex = Math.max(0, Math.min(11, (month || 1) - 1));
  return `${MONTH_NAMES[mIndex]} ${year}`;
}

export function formatDateLong(dateStr, _timezone = DEFAULT_TIMEZONE) {
  if (!dateStr) return 'Fecha no especificada';
  const { year, month, day } = parseDateString(dateStr);
  const dateObj = new Date(year, month - 1, day);
  const dayOfWeek = dateObj.getDay();
  const dayIndex = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  const mIndex = Math.max(0, Math.min(11, month - 1));
  return `${DAY_NAMES[dayIndex]} ${day} de ${MONTH_NAMES[mIndex].toLowerCase()} de ${year}`;
}

export function getMonthRange(year, month) {
  const firstDay = toDateString(year, month, 1);
  const lastDay = toDateString(year, month, getDaysInMonth(year, month));
  return { firstDay, lastDay, inicio: firstDay, fin: lastDay };
}

export function addDays(dateStr, days) {
  const { year, month, day } = parseDateString(dateStr);
  const dateObj = new Date(year, month - 1, day);
  dateObj.setDate(dateObj.getDate() + days);
  return toDateString(dateObj);
}
