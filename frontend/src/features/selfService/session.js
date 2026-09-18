export const BOOKING_STORAGE_KEY = 'appointment-management.booking.v1';

export function readBooking() {
  try {
    const data = JSON.parse(sessionStorage.getItem(BOOKING_STORAGE_KEY));
    return data && typeof data === 'object' && typeof data.session === 'string' ? data : null;
  } catch { return null; }
}
export function saveBooking(data) {
  try { sessionStorage.setItem(BOOKING_STORAGE_KEY, JSON.stringify(data)); } catch { /* Booking also works without storage. */ }
}
export function clearBooking() {
  try { sessionStorage.removeItem(BOOKING_STORAGE_KEY); } catch { /* Storage may be unavailable. */ }
}

export function dateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
export function localDate(value) { return new Date(`${value}T12:00:00`); }
export function professionalToday(timezone) {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
  return `${parts.find(p => p.type === 'year').value}-${parts.find(p => p.type === 'month').value}-${parts.find(p => p.type === 'day').value}`;
}
export function friendlyDate(value) {
  return localDate(value).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });
}
