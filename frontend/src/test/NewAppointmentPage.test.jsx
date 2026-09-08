import { fireEvent, render, screen, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../components/ui/ToastProvider';
import { NewAppointmentPage } from '../features/professional/pages/NewAppointmentPage';
import { api } from '../services/api';

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter initialEntries={['/profesional/turnos/nuevo?fecha=2030-09-10']}><Routes><Route path="/profesional/turnos/nuevo" element={<NewAppointmentPage/>}/><Route path="/profesional/mi-dia" element={<p>Agenda actualizada</p>}/></Routes></MemoryRouter></ToastProvider></QueryClientProvider>);
}

const patient = { id: 7, nombre: 'Ana', apellido: 'Pérez', tipoDocumento: 'DNI', numeroDocumento: '30111222' };

describe('alta manual de turnos', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(api, 'get').mockImplementation(async (url) => {
      if (url.endsWith('/configuracion')) return { duracionAproximadaPorTurno: 30, cantidadMaxTurnosALaVez: 2 };
      if (url.endsWith('/dias-agenda/5')) return { diaAgendaId: 5, fecha: '2030-09-10', estadoActual: 'ACTIVO', brechas: [{ horaInicio: '08:00', horaFin: '12:00' }] };
      if (url.includes('dias-agenda/seleccionables')) return [{ diaAgendaId: 5, fecha: '2030-09-10', estado: 'ACTIVO', seleccionable: true, cantidadBrechas: 2, cantidadTurnosAsignados: 3 }];
      if (url.includes('horarios-sugeridos')) return [
        { horaInicio: '09:00:00', horaFin: '09:30:00', turnosConcurrentes: 1, capacidadSimultanea: 2, advertencias: [] },
        { horaInicio: '11:45:00', horaFin: '12:15:00', turnosConcurrentes: 0, capacidadSimultanea: 2, advertencias: ['HORARIO_FUERA_DE_BRECHA'] },
        { horaInicio: '10:00:00', horaFin: '10:30:00', turnosConcurrentes: 2, capacidadSimultanea: 2, advertencias: ['CAPACIDAD_SUPERADA'] },
      ];
      if (url.includes('/clientes')) return { content: [patient] };
      return [];
    });
  });

  it('usa calendario mensual y combina atención, brechas, capacidad y horario en el paso 3', async () => {
    const previewResult = {
      creado: false, requiereConfirmacion: true,
      tokenConfirmacion: 'confirmacion-firmada',
      advertencias: ['HORARIO_FUERA_DE_BRECHA', 'CAPACIDAD_SUPERADA'],
      cliente: patient, tipoAtencion: { id: 3, nombre: 'Consulta' },
    };
    const post = vi.spyOn(api, 'post').mockResolvedValueOnce(previewResult)
      .mockResolvedValueOnce(previewResult).mockResolvedValueOnce({ creado: true, turnoId: 99 });
    renderPage();

    fireEvent.change(screen.getByLabelText('Nombre, apellido o DNI'), { target: { value: 'Ana' } });
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
    fireEvent.click(await screen.findByRole('button', { name: /Ana Pérez/ }));
    expect(screen.getByLabelText('Progreso del alta').children).toHaveLength(4);
    expect(screen.queryByRole('listbox', { name: 'Seleccionar mes del turno' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Seleccionar mes del turno' }));
    expect(screen.getByRole('listbox', { name: 'Seleccionar mes del turno' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Septiembre' })).toHaveAttribute('aria-selected', 'true');
    fireEvent.keyDown(screen.getByRole('listbox', { name: 'Seleccionar mes del turno' }), { key: 'Escape' });
    expect(await screen.findByRole('gridcell', { name: /2 brechas horarias.*3 turnos activos/ })).toBeInTheDocument();
    fireEvent.click(await screen.findByRole('gridcell', { name: /10 de Septiembre de 2030/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Seleccionar día' }));
    expect(await screen.findByText('Cronograma del día')).toBeInTheDocument();
    expect(screen.queryByText('Brechas horarias')).not.toBeInTheDocument();
    expect(screen.getAllByText(/08:00.*12:00/).length).toBeGreaterThan(0);
    expect(await screen.findByText(/Duración aproximada: 30 min/)).toBeInTheDocument();
    expect((await screen.findAllByText(/Capacidad simultánea: 2/)).length).toBeGreaterThan(0);
    const timeline = await screen.findByTestId('daily-timeline-grid');
    expect(within(timeline).queryByRole('button', { name: /10:00.*10:30/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('switch', { name: 'Mostrar turnos con superposición' }));
    expect(within(timeline).getByRole('button', { name: /10:00.*10:30.*capacidad simultánea/ })).toBeInTheDocument();
    const initialHeight = timeline.style.height;
    fireEvent.click(screen.getByRole('button', { name: 'Aumentar zoom' }));
    expect(timeline.style.height).not.toBe(initialHeight);
    fireEvent.click(screen.getByRole('button', { name: 'Restablecer zoom' }));
    expect(timeline.style.height).toBe(initialHeight);
    fireEvent.wheel(timeline, { ctrlKey: true, deltaY: -100 });
    expect(timeline.style.height).not.toBe(initialHeight);
    fireEvent.wheel(timeline, { ctrlKey: true, deltaY: 100 });
    expect(timeline.style.height).toBe(initialHeight);
    const ordinaryCandidate = within(timeline).getByRole('button', { name: /09:00.*09:30/ });
    const overflowCandidate = within(timeline).getByRole('button', { name: /11:45.*12:15.*fuera de las franjas/ });
    fireEvent.click(overflowCandidate);
    expect(overflowCandidate).toHaveAttribute('aria-pressed', 'true');
    expect(ordinaryCandidate).toHaveAttribute('aria-pressed', 'false');
    fireEvent.click(ordinaryCandidate);
    expect(ordinaryCandidate).toHaveAttribute('aria-pressed', 'true');
    expect(overflowCandidate).toHaveAttribute('aria-pressed', 'false');
    expect(screen.queryByText('Consulta de turnos no disponible')).not.toBeInTheDocument();
    expect(screen.getByTestId('appointment-availability-column')).toHaveClass('lg:col-span-1');
    fireEvent.click(screen.getByRole('button', { name: 'Volver al día' }));
    expect(await screen.findByRole('button', { name: 'Seleccionar día' })).toBeInTheDocument();
    fireEvent.doubleClick(await screen.findByRole('gridcell', { name: /10 de Septiembre de 2030/ }));
    fireEvent.click(await screen.findByRole('button', { name: /09:00.*09:30/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Revisar avisos y confirmar' }));

    expect(await screen.findByText('Ana Pérez')).toBeInTheDocument();
    expect(screen.getByText('DNI 30111222')).toBeInTheDocument();
    expect(screen.getByText(/fuera de las franjas/)).toBeInTheDocument();
    expect(screen.getByText(/capacidad simultánea/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Volver al horario' }));
    expect(screen.getByRole('button', { name: /Hora de inicio: 09:00/ })).toHaveTextContent('09:00');
    fireEvent.click(screen.getByRole('button', { name: 'Revisar avisos y confirmar' }));
    fireEvent.click(await screen.findByRole('button', { name: /Confirmar y crear/ }));

    await screen.findByText('Agenda actualizada');
    expect(post).toHaveBeenLastCalledWith(expect.stringMatching(/\/turnos$/), expect.objectContaining({ tokenConfirmacion: 'confirmacion-firmada' }), expect.anything());
  }, 10000);

  it('muestra un día inactivo pero impide continuar', async () => {
    api.get.mockImplementation(async (url) => {
      if (url.endsWith('/configuracion')) return { duracionAproximadaPorTurno: 30, cantidadMaxTurnosALaVez: 2 };
      if (url.includes('dias-agenda/seleccionables')) return [{ diaAgendaId: 5, fecha: '2030-09-10', estado: 'INACTIVO', seleccionable: false, mensaje: 'Día inactivo' }];
      if (url.includes('/clientes')) return { content: [patient] };
      return [];
    });
    renderPage();
    fireEvent.change(screen.getByLabelText('Nombre, apellido o DNI'), { target: { value: '30111222' } });
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
    fireEvent.click(await screen.findByRole('button', { name: /Ana Pérez/ }));
    fireEvent.click(await screen.findByRole('gridcell', { name: /10 de Septiembre de 2030/ }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Día inactivo');
    expect(screen.getByRole('button', { name: 'Seleccionar día' })).toBeDisabled();
  });

  it('explica por qué no hay candidatos ni alta manual sin tipos activos', async () => {
    api.get.mockImplementation(async (url) => {
      if (url.endsWith('/configuracion')) return null;
      if (url.endsWith('/dias-agenda/5')) return { id: 5, fecha: '2030-09-10', estadoActual: 'ACTIVO', brechas: [{ horaInicio: '08:00', horaFin: '12:00' }] };
      if (url.includes('dias-agenda/seleccionables')) return [{ diaAgendaId: 5, fecha: '2030-09-10', estado: 'ACTIVO', seleccionable: true }];
      if (url.includes('/clientes')) return { content: [patient] };
      return [];
    });
    renderPage();
    fireEvent.change(screen.getByLabelText('Nombre, apellido o DNI'), { target: { value: 'Ana' } });
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
    fireEvent.click(await screen.findByRole('button', { name: /Ana Pérez/ }));
    fireEvent.doubleClick(await screen.findByRole('gridcell', { name: /10 de Septiembre de 2030/ }));

    expect(await screen.findByText('No hay configuración profesional')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Revisar avisos y confirmar' })).toBeDisabled();
  });

  it('al modificar el horario invalida la prevalidación anterior', async () => {
    vi.spyOn(api, 'post').mockResolvedValue({ creado: false, tokenConfirmacion: 'token', advertencias: [], cliente: patient, tipoAtencion: { nombre: 'Consulta' } });
    renderPage();
    fireEvent.change(screen.getByLabelText('Nombre, apellido o DNI'), { target: { value: 'Ana' } });
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
    fireEvent.click(await screen.findByRole('button', { name: /Ana Pérez/ }));
    fireEvent.doubleClick(await screen.findByRole('gridcell', { name: /10 de Septiembre de 2030/ }));
    fireEvent.click(await screen.findByRole('button', { name: /09:00.*09:30/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Revisar avisos y confirmar' }));
    await screen.findByText('Confirmar turno');
    fireEvent.click(screen.getByRole('button', { name: 'Volver al horario' }));
    expect(screen.queryByRole('button', { name: /Confirmar y crear/ })).not.toBeInTheDocument();
  });
});
