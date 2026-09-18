import { beforeEach, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SelfServicePage, SelfServiceShortcut } from '@/features/professional/pages/SelfServicePage';
import { selfServiceLinkApi } from '@/features/selfService/api';

vi.mock('@/features/selfService/api', () => ({ selfServiceLinkApi: { get: vi.fn(), create: vi.fn(), revoke: vi.fn() }, bookingUrl: token => `http://localhost/reservar#${token}` }));
beforeEach(() => { vi.resetAllMocks(); selfServiceLinkApi.get.mockResolvedValue({ activo: true, recuperable: true, token: 'saved-token' }); });

it('recupera el enlace y permite copiar, abrir y mostrar el QR', async () => {
  const writeText = vi.fn().mockResolvedValue(); Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } });
  render(<MemoryRouter initialEntries={["/profesional/autogestion"]}><SelfServicePage /></MemoryRouter>); await screen.findByDisplayValue('http://localhost/reservar#saved-token');
  fireEvent.click(screen.getByRole('button', { name: 'Copiar enlace' }));
  await waitFor(() => expect(writeText).toHaveBeenCalledWith('http://localhost/reservar#saved-token'));
  expect(screen.getByRole('link', { name: /Abrir portal/ })).toHaveAttribute('rel', 'noopener noreferrer');
  fireEvent.click(screen.getByRole('button', { name: 'Mostrar QR' })); expect(screen.getByText('Código QR del enlace de reserva')).toBeInTheDocument();
});
it('confirma la regeneración antes de invalidar el enlace anterior', async () => {
  selfServiceLinkApi.create.mockResolvedValue({ token: 'new-token' }); render(<MemoryRouter initialEntries={["/profesional/autogestion"]}><SelfServicePage /></MemoryRouter>);
  fireEvent.click(await screen.findByRole('button', { name: 'Generar un enlace nuevo' }));
  expect(selfServiceLinkApi.create).not.toHaveBeenCalled(); expect(screen.getByText(/sesiones abiertas desde él dejarán de funcionar/)).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Generar enlace' }));
  await screen.findByDisplayValue('http://localhost/reservar#new-token');
});
it('revoca solo después de confirmar y retira el enlace de la pantalla', async () => {
  selfServiceLinkApi.revoke.mockResolvedValue(null); render(<MemoryRouter initialEntries={["/profesional/autogestion"]}><SelfServicePage /></MemoryRouter>);
  fireEvent.click(await screen.findByRole('button', { name: 'Revocar enlace' }));
  expect(selfServiceLinkApi.revoke).not.toHaveBeenCalled();
  fireEvent.click(screen.getAllByRole('button', { name: 'Revocar enlace' }).at(-1));
  await screen.findByText('Sin enlace activo'); expect(screen.queryByDisplayValue('http://localhost/reservar#saved-token')).not.toBeInTheDocument();
});
it('ofrece generar un enlace si no hay uno activo', async () => {
  selfServiceLinkApi.get.mockResolvedValue({ activo: false }); render(<MemoryRouter initialEntries={["/profesional/autogestion"]}><SelfServicePage /></MemoryRouter>);
  expect(await screen.findByRole('button', { name: 'Generar enlace' })).toBeInTheDocument();
});
it('explica que hace falta autenticación y no simula un enlace', async () => {
  selfServiceLinkApi.get.mockRejectedValue(Object.assign(new Error('No autorizado'), { status: 401 })); render(<MemoryRouter initialEntries={["/profesional/autogestion"]}><SelfServicePage /></MemoryRouter>);
  await screen.findByText(/Iniciá sesión como profesional/); expect(screen.queryByRole('button', { name: 'Generar enlace' })).not.toBeInTheDocument();
});
it('incluye acceso desde la tarjeta del dashboard', () => {
  render(<MemoryRouter><SelfServiceShortcut /></MemoryRouter>);
  expect(screen.getByRole('link', { name: /Administrar autogestión/ })).toHaveAttribute('href', '/profesional/autogestion');
});
