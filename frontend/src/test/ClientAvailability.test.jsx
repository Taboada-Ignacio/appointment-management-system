import { beforeEach, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BookingPage } from '@/features/selfService/BookingPage';
import { selfServiceApi } from '@/features/selfService/api';
import { dateKey, localDate, professionalToday, saveBooking } from '@/features/selfService/session';

vi.mock('@/features/selfService/api', () => ({ selfServiceApi: Object.fromEntries(['resume', 'config', 'months', 'days', 'dates', 'gaps'].map(key => [key, vi.fn()])) }));

const timezone = 'America/Argentina/Buenos_Aires';
const today = professionalToday(timezone), current = localDate(today);
const nextMonth = new Date(current.getFullYear(), current.getMonth() + 1, 1);
const availableDate = dateKey(nextMonth);

beforeEach(() => {
  vi.resetAllMocks(); sessionStorage.clear();
  const link = 'L'.repeat(43), session = 'S'.repeat(43);
  window.history.replaceState({}, '', `/reservar#${link}`);
  saveBooking({ link, session, step: 'fecha', draft: {} });
  selfServiceApi.resume.mockResolvedValue({ profesional: { nombre: 'María', apellido: 'Pérez', zonaHoraria: timezone }, cliente: { nombre: 'Ana', estado: 'HABILITADO', puedeReservar: true }, dni: '30111222' });
  selfServiceApi.config.mockResolvedValue({ duracionMinutos: 30, capacidadSimultanea: 1 });
  selfServiceApi.months.mockResolvedValue([today.slice(0, 7), availableDate.slice(0, 7)]);
  selfServiceApi.days.mockImplementation((_session, month) => Promise.resolve(availableDate.startsWith(month) ? [{ id: 1, fecha: availableDate, estadoActual: 'ACTIVO', seleccionable: true }] : []));
  selfServiceApi.gaps.mockResolvedValue([{ inicio: '08:00:00', fin: '10:00:00', intervalos: [{ inicio: '08:00:00', fin: '08:30:00' }] }]);
});

function renderPortal() {
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })}><BookingPage /></QueryClientProvider>);
}

it('abre el mes actual completo aunque la primera fecha disponible sea del mes siguiente', async () => {
  renderPortal();
  const grid = await screen.findByRole('grid', { name: 'Calendario mensual de disponibilidad' });
  const cells = within(grid).getAllByRole('gridcell');
  expect(cells).toHaveLength(new Date(current.getFullYear(), current.getMonth() + 1, 0).getDate());
  expect(screen.getByRole('heading', { name: new RegExp(`${current.toLocaleDateString('es-AR', { month: 'long' })} de ${current.getFullYear()}`, 'i') })).toBeInTheDocument();
  expect(cells.every(cell => cell.disabled)).toBe(true);
  expect(selfServiceApi.days).toHaveBeenCalledWith('S'.repeat(43), today.slice(0, 7));
  expect(selfServiceApi.dates).not.toHaveBeenCalled();
  expect(screen.getByRole('button', { name: 'Mes anterior' })).toBeDisabled();
});

it('al seleccionar un día disponible muestra solo brechas e intervalos seleccionables', async () => {
  renderPortal();
  await screen.findByRole('grid', { name: 'Calendario mensual de disponibilidad' });
  await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Mes siguiente' })); });
  const cell = await screen.findByRole('gridcell', { name: /1 de .* Disponible\./ });
  await waitFor(() => expect(cell).toBeEnabled());
  await act(async () => { fireEvent.click(cell); });
  const interval = await screen.findByRole('button', { name: /Seleccionar.*08:00.*08:30/ });
  expect(screen.queryByText(/Ocupación/)).not.toBeInTheDocument();
  expect(screen.queryByText('Señal de disponibilidad')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /Editar brechas/ })).not.toBeInTheDocument();
  await act(async () => { fireEvent.click(interval); });
  expect(interval).toHaveAttribute('aria-pressed', 'true');
  expect(screen.getByRole('button', { name: /Revisar solicitud/ })).toBeEnabled();
});

 it('muestra y permite seleccionar días activos aunque el profesional no tenga tipos de atención', async () => {
  selfServiceApi.config.mockResolvedValue({ duracionMinutos: 15, capacidadSimultanea: 1 });
  selfServiceApi.days.mockResolvedValue([{ id: 1, fecha: today, estadoActual: 'ACTIVO', seleccionable: true }]);
  renderPortal();
  const grid = await screen.findByRole('grid', { name: 'Calendario mensual de disponibilidad' });
  const day = within(grid).getByRole('gridcell', { name: / Disponible\./ });
  await act(async () => { fireEvent.click(day); });
  await screen.findByRole('heading', { name: 'Encontrá tu horario' });
  expect(await screen.findByRole('button', { name: /Seleccionar.*08:00/ })).toBeInTheDocument();
  expect(screen.queryByLabelText('Tipo de atención')).not.toBeInTheDocument();
  expect(selfServiceApi.gaps).toHaveBeenCalledWith('S'.repeat(43), today);
});

it('no permite ver meses inactivos ni restaura un mes que ya dejó de estar activo', async () => {
  selfServiceApi.months.mockResolvedValue([availableDate.slice(0, 7)]);
  renderPortal();
  await screen.findByRole('grid', { name: 'Calendario mensual de disponibilidad' });
  expect(screen.getByRole('button', { name: 'Mes anterior' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Mes siguiente' })).toBeDisabled();
  expect(selfServiceApi.days).toHaveBeenCalledWith('S'.repeat(43), availableDate.slice(0, 7));
  expect(selfServiceApi.days).not.toHaveBeenCalledWith('S'.repeat(43), today.slice(0, 7));
});
