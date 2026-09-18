import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, expect, it, vi } from 'vitest';
import { ProfessionalDirectoryPage } from '@/features/professional/pages/ProfessionalDirectoryPage';
import { api } from '@/services/api';
import userEvent from '@testing-library/user-event';

// Pointer capture and scrolling are implemented by browsers but absent in jsdom.
Element.prototype.hasPointerCapture = () => false;
Element.prototype.setPointerCapture = () => {};
Element.prototype.releasePointerCapture = () => {};
Element.prototype.scrollIntoView = () => {};
afterEach(() => vi.restoreAllMocks());
function show(clients = false, pendingClients = false) {
  render(<MemoryRouter><QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><ProfessionalDirectoryPage clients={clients} pendingClients={pendingClients} /></QueryClientProvider></MemoryRouter>);
}
it('selecciona todos los pendientes de otras páginas y verifica la selección', async () => {
  const get = vi.spyOn(api, 'get').mockImplementation(url => Promise.resolve(url.endsWith('/ids') ? [7, 8] : { content: [{ id: 7, inicioEstimado: '2026-09-18T11:00:00Z', finEstimado: '2026-09-18T11:30:00Z', cliente: { nombre: 'Ana' } }], totalElements: 2, totalPages: 2 }));
  const post = vi.spyOn(api, 'post').mockResolvedValue(null);
  show(); await screen.findByText('Ana');
  expect(screen.getByRole('button', { name: 'Verificar turno' })).toBeDisabled();
  await userEvent.click(screen.getByRole('checkbox', { name: 'Seleccionar turno #7' }));
  expect(screen.getByText('1 seleccionados')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Seleccionar todos los turnos' }));
  await screen.findByText('2 seleccionados');
  expect(get).toHaveBeenCalledWith(expect.stringMatching(/pendientes-verificacion\/ids$/));
  await userEvent.click(screen.getByRole('button', { name: 'Verificar turno' }));
  await waitFor(() => expect(post).toHaveBeenCalledWith(expect.stringMatching(/\/verificar$/), { turnoIds: [7, 8] }, expect.anything()));
  await screen.findByText('Los turnos seleccionados fueron verificados y quedaron asignados.');
});
it('solicita un motivo obligatorio para dar de baja la selección', async () => {
  vi.spyOn(api, 'get').mockResolvedValue({ content: [{ id: 7, inicioEstimado: '2026-09-18T11:00:00Z', finEstimado: '2026-09-18T11:30:00Z', cliente: { nombre: 'Ana' } }], totalElements: 1, totalPages: 1 });
  const post = vi.spyOn(api, 'post').mockResolvedValue(null);
  show(); await screen.findByText('Ana');
  await userEvent.click(screen.getByRole('checkbox', { name: 'Seleccionar turno #7' }));
  await userEvent.click(screen.getByRole('button', { name: 'Dar de baja turno' }));
  expect(screen.getByRole('button', { name: 'Confirmar baja' })).toBeDisabled();
  fireEvent.change(screen.getByLabelText('Motivo de baja'), { target: { value: 'Solicitud del cliente' } });
  await userEvent.click(screen.getByRole('button', { name: 'Confirmar baja' }));
  await waitFor(() => expect(post).toHaveBeenCalledWith(expect.stringMatching(/\/baja$/), { turnoIds: [7], motivo: 'Solicitud del cliente' }, expect.anything()));
});
it('consulta turnos pendientes sin limitar por fecha y permite consultar otra página', async () => {
  const get = vi.spyOn(api, 'get').mockResolvedValue({ content: [{ id: 7, inicioEstimado: '2026-09-18T11:00:00Z', finEstimado: '2026-09-18T11:30:00Z', cliente: { nombre: 'Ana', apellido: 'Paz', numeroDocumento: '30111222' } }], totalElements: 21, totalPages: 2 });
  show();
  await screen.findByText('Ana Paz');
  expect(get).toHaveBeenCalledWith(expect.stringMatching(/turnos\/pendientes-verificacion\?page=0&size=20$/));
  fireEvent.click(screen.getByRole('button', { name: 'Página siguiente' }));
  await waitFor(() => expect(get).toHaveBeenCalledWith(expect.stringMatching(/page=1&size=20$/)));
});
it('consulta todos los clientes y envía los filtros al backend', async () => {
  const get = vi.spyOn(api, 'get').mockResolvedValue({ content: [{ id: 1, nombre: 'Ana', apellido: 'Paz', numeroDocumento: '30111222', estadoActual: 'HABILITADO' }], totalElements: 1, totalPages: 1 });
  show(true);
  await screen.findByText('Verificado (habilitado)');
  expect(get).toHaveBeenCalledWith(expect.stringMatching(/clientes\?page=0&size=20$/));
  fireEvent.change(screen.getByLabelText('DNI'), { target: { value: '30111222' } });
  await waitFor(() => expect(get).toHaveBeenCalledWith(expect.stringContaining('dni=30111222')));
  await userEvent.click(screen.getByRole('combobox', { name: 'Estado del cliente' }));
  await userEvent.click(await screen.findByRole('option', { name: 'Pendiente de verificación' }));
  await waitFor(() => expect(get).toHaveBeenCalledWith(expect.stringContaining('estado=PENDIENTE_DE_VERIFICACION')));
});

it('cambia el estado de un cliente pendiente a habilitado', async () => {
  vi.spyOn(api, 'get').mockResolvedValue({ content: [{ id: 9, nombre: 'Ana', apellido: 'López', estadoActual: 'PENDIENTE_DE_VERIFICACION' }], totalElements: 1, totalPages: 1 });
  const put = vi.spyOn(api, 'put').mockResolvedValue(null);
  show(true);
  await userEvent.click(await screen.findByRole('button', { name: 'Cambiar estado de Ana López' }));
  await userEvent.click(screen.getByRole('button', { name: 'Guardar estado' }));
  await waitFor(() => expect(put).toHaveBeenCalledWith('/api/profesionales/1/clientes/9/estado', { estado: 'HABILITADO' }, expect.any(Object)));
  expect(await screen.findByText('El estado del cliente fue actualizado.')).toBeInTheDocument();
});

it('selecciona todos los clientes pendientes de otras páginas y los verifica', async () => {
  const get = vi.spyOn(api, 'get').mockImplementation(url => Promise.resolve(url.endsWith('/ids') ? [9, 10] : { content: [{ id: 9, nombre: 'Ana', apellido: 'López', estadoActual: 'PENDIENTE_DE_VERIFICACION' }], totalElements: 2, totalPages: 2 }));
  const post = vi.spyOn(api, 'post').mockResolvedValue(null);
  show(true, true);
  await screen.findByText('Ana López');
  expect(screen.queryByRole('button', { name: /Cambiar estado/ })).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Verificar' })).toBeDisabled();
  await userEvent.click(screen.getByRole('button', { name: 'Seleccionar todos los clientes' }));
  expect(get).toHaveBeenCalledWith('/api/profesionales/1/clientes/pendientes-verificacion/ids');
  await userEvent.click(screen.getByRole('button', { name: 'Verificar' }));
  await waitFor(() => expect(post).toHaveBeenCalledWith('/api/profesionales/1/clientes/pendientes-verificacion/estado', { clienteIds: [9, 10], estado: 'HABILITADO' }, expect.any(Object)));
});
