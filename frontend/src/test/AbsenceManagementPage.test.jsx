import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../components/ui/ToastProvider';
import { AbsenceManagementPage } from '../features/professional/pages/AbsenceManagementPage';
import { api } from '../services/api';

function renderPage(section = 'register') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter initialEntries={[`/profesional/ausencias/${section}`]}>
          <AbsenceManagementPage section={section} />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

describe('AbsenceManagementPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(api, 'get').mockResolvedValue([]);
  });

  it('ordena el calendario de lunes a domingo', async () => {
    renderPage('register');
    fireEvent.click(screen.getByRole('button', { name: /Desde:/ }));

    await waitFor(() => {
      const weekdays = [...document.querySelectorAll('.rdp-weekday')];
      expect(weekdays.map((day) => day.textContent)).toEqual(['lu', 'ma', 'mi', 'ju', 'vi', 'sá', 'do']);
    });
  });

  it('muestra el resumen al confirmar una ausencia con el contrato REST en español', async () => {
    const postSpy = vi.spyOn(api, 'post').mockImplementation(async (url) => {
      if (url.endsWith('/preview')) {
        return {
          previewToken: 'token-vigente',
          cantidadTurnosAfectados: 0,
          cantidadNotificacionesWhatsApp: 0,
          cantidadSinNotificacion: 0,
          turnosAfectados: [],
        };
      }

      return {
        excepcion: { id: 10, tipo: 'VACACIONES' },
        impacto: {
          cantidadTurnosAfectados: 0,
          cantidadNotificacionesWhatsApp: 0,
          cantidadSinNotificacion: 0,
          turnosAfectados: [],
        },
      };
    });

    renderPage('register');
    fireEvent.click(screen.getByRole('button', { name: /Desde:/ }));
    fireEvent.click(document.querySelector('[data-day="10/9/2026"]'));
    fireEvent.click(document.querySelector('[data-day="12/9/2026"]'));
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Vacaciones programadas' } });
    fireEvent.click(screen.getByRole('button', { name: 'Revisar impacto' }));

    await screen.findByText('Turnos afectados');
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar ausencia' }));

    await waitFor(() => {
      expect(screen.getByText('La excepción ha sido cargada con éxito')).toBeInTheDocument();
      expect(screen.getByText('Registrar otra excepción')).toBeInTheDocument();
      expect(screen.getByText('Ver todas las excepciones')).toBeInTheDocument();
    });

    expect(postSpy).toHaveBeenCalledWith(
      expect.stringContaining('/excepciones-agenda'),
      expect.objectContaining({
        tipo: 'VACACIONES',
        motivo: 'Vacaciones programadas',
      }),
      expect.anything(),
    );
  });

  it('permite registrar una habilitación extraordinaria con franjas y confirmación directa informativa', async () => {
    let capturedPayload = null;
    vi.spyOn(api, 'post').mockImplementation(async (url, payload) => {
      capturedPayload = payload;
      if (url.endsWith('/preview')) {
        return {
          previewToken: 'token-habilitacion',
          cantidadTurnosAfectados: 0,
          cantidadNotificacionesWhatsApp: 0,
          cantidadSinNotificacion: 0,
          turnosAfectados: [],
        };
      }

      return {
        excepcion: { id: 20, tipo: 'HABILITACION_EXTRAORDINARIA' },
        impacto: {
          cantidadTurnosAfectados: 0,
          cantidadNotificacionesWhatsApp: 0,
          cantidadSinNotificacion: 0,
          turnosAfectados: [],
        },
      };
    });

    renderPage('habilitaciones');

    expect(screen.getByRole('heading', { name: 'Registrar Habilitaciones Extraordinarias' })).toBeInTheDocument();
    expect(screen.getByText('Habilitación extraordinaria de disponibilidad')).toBeInTheDocument();
    expect(screen.getByText('Franjas horarias a habilitar')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Desde:/ }));
    fireEvent.click(document.querySelector('[data-day="15/9/2026"]'));
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Guardia especial' } });

    // Step 1 button for habilitaciones is "Continuar"
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }));

    // Step 2 is informative without affected appointments list
    await screen.findByText('Confirmar habilitación extraordinaria');
    expect(screen.getByText(/Esta habilitación añade disponibilidad y no cancela turnos previos/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar habilitación' }));

    await waitFor(() => {
      expect(screen.getByText('Habilitación extraordinaria cargada con éxito')).toBeInTheDocument();
      expect(screen.getByText('Registrar otra habilitación')).toBeInTheDocument();
    });

    expect(capturedPayload).toMatchObject({
      tipo: 'HABILITACION_EXTRAORDINARIA',
      motivo: 'Guardia especial',
      brechas: expect.arrayContaining([expect.objectContaining({ horaInicio: '09:00', horaFin: '10:00' })]),
    });
  });

  it('permite registrar una modificación extraordinaria y revisar turnos alcanzados', async () => {
    let capturedPayload = null;
    vi.spyOn(api, 'post').mockImplementation(async (url, payload) => {
      capturedPayload = payload;
      if (url.endsWith('/preview')) {
        return {
          previewToken: 'token-modificacion',
          cantidadTurnosAfectados: 1,
          cantidadNotificacionesWhatsApp: 1,
          cantidadSinNotificacion: 0,
          turnosAfectados: [
            {
              turnoId: 101,
              nombreCliente: 'Juan Pérez',
              estado: 'CONFIRMADO',
              fecha: '2026-09-20',
              inicioEstimado: '2026-09-20T11:00:00Z',
              finEstimado: '2026-09-20T11:30:00Z',
              telefono: '+5491100001111',
              notificacionWhatsAppHabilitada: true,
            },
          ],
        };
      }

      return {
        excepcion: { id: 30, tipo: 'MODIFICACION_HORARIO' },
        impacto: {
          cantidadTurnosAfectados: 1,
          cantidadNotificacionesWhatsApp: 1,
          cantidadSinNotificacion: 0,
          turnosAfectados: [],
        },
      };
    });

    renderPage('modificaciones');

    expect(screen.getByRole('heading', { name: 'Registrar Modificaciones Extraordinarias' })).toBeInTheDocument();
    expect(screen.getByText('Modificación excepcional de horario')).toBeInTheDocument();
    expect(screen.getByText('Nuevas franjas horarias de atención')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Desde:/ }));
    fireEvent.click(document.querySelector('[data-day="20/9/2026"]'));
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Horario corrido especial' } });

    fireEvent.click(screen.getByRole('button', { name: 'Revisar impacto' }));

    await screen.findByText('Turnos alcanzados por el cambio de horario');
    expect(screen.getByText('Juan Pérez')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar modificación' }));

    await waitFor(() => {
      expect(screen.getByText('Modificación de horario cargada con éxito')).toBeInTheDocument();
      expect(screen.getByText('Registrar otra modificación')).toBeInTheDocument();
    });

    expect(capturedPayload).toMatchObject({
      tipo: 'MODIFICACION_HORARIO',
      motivo: 'Horario corrido especial',
      brechas: expect.arrayContaining([expect.objectContaining({ horaInicio: '09:00', horaFin: '10:00' })]),
    });
  });

  it('permite filtrar excepciones por categoría en ExceptionsPanel', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([
      { id: 1, tipo: 'VACACIONES', motivo: 'Vacaciones invierno', fechaInicio: '2026-07-01', fechaFin: '2026-07-15', activa: true },
      { id: 2, tipo: 'HABILITACION_EXTRAORDINARIA', motivo: 'Guardia fin de semana', fechaInicio: '2026-08-01', fechaFin: '2026-08-01', activa: true },
      { id: 3, tipo: 'MODIFICACION_HORARIO', motivo: 'Jornada reducida', fechaInicio: '2026-09-01', fechaFin: '2026-09-01', activa: true },
    ]);

    renderPage('exceptions');

    await screen.findAllByText('Vacaciones invierno');
    expect(screen.getAllByText('Guardia fin de semana').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Jornada reducida').length).toBeGreaterThan(0);

    // Filter by Habilitaciones Extraordinarias
    fireEvent.click(screen.getByRole('button', { name: 'Habilitaciones Extraordinarias' }));
    expect(screen.getAllByText('Guardia fin de semana').length).toBeGreaterThan(0);
    expect(screen.queryAllByText('Vacaciones invierno')).toHaveLength(0);
    expect(screen.queryAllByText('Jornada reducida')).toHaveLength(0);

    // Filter by Modificaciones de Horario
    fireEvent.click(screen.getByRole('button', { name: 'Modificaciones de Horario' }));
    expect(screen.getAllByText('Jornada reducida').length).toBeGreaterThan(0);
    expect(screen.queryAllByText('Vacaciones invierno')).toHaveLength(0);
    expect(screen.queryAllByText('Guardia fin de semana')).toHaveLength(0);

    // Filter by Ausencias
    fireEvent.click(screen.getByRole('button', { name: 'Ausencias' }));
    expect(screen.getAllByText('Vacaciones invierno').length).toBeGreaterThan(0);
    expect(screen.queryAllByText('Guardia fin de semana')).toHaveLength(0);
    expect(screen.queryAllByText('Jornada reducida')).toHaveLength(0);

    // Filter by Todas
    fireEvent.click(screen.getByRole('button', { name: 'Todas' }));
    expect(screen.getAllByText('Vacaciones invierno').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Guardia fin de semana').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Jornada reducida').length).toBeGreaterThan(0);
  });

  it('ordena las excepciones de la más nueva a la más vieja en ExceptionsPanel', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([
      { id: 1, tipo: 'VACACIONES', motivo: 'Vacaciones invierno', fechaInicio: '2026-07-01', fechaFin: '2026-07-15', activa: true },
      { id: 2, tipo: 'HABILITACION_EXTRAORDINARIA', motivo: 'Guardia fin de semana', fechaInicio: '2026-08-01', fechaFin: '2026-08-01', activa: true },
      { id: 3, tipo: 'MODIFICACION_HORARIO', motivo: 'Jornada reducida', fechaInicio: '2026-09-01', fechaFin: '2026-09-01', activa: true },
    ]);

    renderPage('exceptions');

    await screen.findAllByText('Vacaciones invierno');
    const table = document.querySelector('tbody');
    const rows = [...table.querySelectorAll('tr')];
    const motives = rows.map((r) => r.querySelectorAll('td')[2]?.textContent);
    expect(motives).toEqual(['Jornada reducida', 'Guardia fin de semana', 'Vacaciones invierno']);
  });
});
