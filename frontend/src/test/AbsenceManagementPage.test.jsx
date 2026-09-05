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

  it('renderiza KPIs superiores, dropdown de creación y panel colapsable en ExceptionsPanel', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url) => {
      if (url.includes('/turnos-afectados')) {
        return [
          { turnoId: 101, excepcionId: 1, resolucion: 'PENDIENTE', nombreCliente: 'Ana Gomez' },
        ];
      }
      return [
        {
          id: 1,
          tipo: 'VACACIONES',
          motivo: 'Vacaciones de invierno',
          fechaInicio: '2026-07-01',
          fechaFin: '2026-07-15',
          activa: true,
          brechas: [],
          fechasExcluidas: [],
          fechaCreacion: '2026-06-01T10:00:00Z',
        },
        {
          id: 2,
          tipo: 'BLOQUEO_HORARIO',
          motivo: 'Trámite particular',
          fechaInicio: '2026-09-10',
          fechaFin: '2026-09-10',
          activa: true,
          brechas: [{ horaInicio: '10:00', horaFin: '12:00' }],
          fechasExcluidas: [],
          fechaCreacion: '2026-09-01T08:00:00Z',
        },
      ];
    });

    renderPage('exceptions');

    // 1c: Verifica renderizado de KPIs superiores
    expect(await screen.findByText('Vigentes hoy')).toBeInTheDocument();
    expect(screen.getByText('Próximos 30 días')).toBeInTheDocument();
    expect(screen.getByText('Con turnos pendientes')).toBeInTheDocument();
    expect(screen.getByText('Total registradas')).toBeInTheDocument();

    // 2c: Verifica dropdown de Nueva excepción
    const newButton = screen.getByRole('button', { name: /Nueva excepción/i });
    expect(newButton).toBeInTheDocument();
    fireEvent.pointerDown(newButton, { button: 0, pointerType: 'mouse' });
    fireEvent.keyDown(newButton, { key: 'ArrowDown' });
    expect(await screen.findByText('Ausencia / Vacaciones')).toBeInTheDocument();
    expect(screen.getByText('Habilitación Extraordinaria')).toBeInTheDocument();
    expect(screen.getByText('Modificación de Horario')).toBeInTheDocument();

    // Cierra el dropdown para remover el bloqueo de aria-hidden del resto de la página
    fireEvent.keyDown(document.activeElement || document.body, { key: 'Escape' });
    await waitFor(() => {
      expect(screen.queryByText('Ausencia / Vacaciones')).not.toBeInTheDocument();
    });

    // 3c: Verifica toggle y despliegue del panel colapsable de filtros avanzados
    const advancedToggle = screen.getByRole('button', { name: /Filtros avanzados/i });
    fireEvent.click(advancedToggle);
    expect(await screen.findByText('Estado de la excepción')).toBeInTheDocument();
    expect(screen.getByText('Período temporal rápido')).toBeInTheDocument();
    expect(screen.getByText('Solo con turnos pendientes')).toBeInTheDocument();

    // 4b y 7b: Verifica datos operativos en la tabla (franjas, duración, botón de turnos afectados)
    expect(screen.getByText('10:00–12:00')).toBeInTheDocument();
    expect(screen.getByText('15 días')).toBeInTheDocument();
    expect(screen.getByText(/1 turnos \(1 pend\.\)/)).toBeInTheDocument();

    // 6b: Abre el Sheet modular y verifica sus bloques
    const verButtons = screen.getAllByRole('button', { name: /Ver/ });
    fireEvent.click(verButtons[0]);

    expect(await screen.findByText(/Detalle de la excepción/)).toBeInTheDocument();
    expect(screen.getByText('Período y Horarios')).toBeInTheDocument();
    expect(screen.getByText('Motivo registrado')).toBeInTheDocument();
    expect(screen.getByText('Trazabilidad')).toBeInTheDocument();
    expect(screen.getByText('Ver en Mi mes')).toBeInTheDocument();
  });

  it('permite filtrar interactivamente desde las tarjetas KPI y limpiar filtros', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url) => {
      if (url.includes('/turnos-afectados')) {
        return [{ turnoId: 101, excepcionId: 1, resolucion: 'PENDIENTE', nombreCliente: 'Ana' }];
      }
      return [
        { id: 1, tipo: 'VACACIONES', motivo: 'Vacaciones de invierno', fechaInicio: '2026-07-01', fechaFin: '2026-07-15', activa: true },
        { id: 2, tipo: 'BLOQUEO_HORARIO', motivo: 'Capacitación interna', fechaInicio: '2026-09-10', fechaFin: '2026-09-10', activa: true },
      ];
    });

    renderPage('exceptions');

    await screen.findAllByText('Vacaciones de invierno');
    expect(screen.getAllByText('Capacitación interna').length).toBeGreaterThan(0);

    // Click KPI "Con turnos pendientes"
    const kpiPendientes = screen.getByText('Con turnos pendientes');
    fireEvent.click(kpiPendientes);

    // Solo debe verse Vacaciones de invierno (excepción 1)
    expect(screen.getAllByText('Vacaciones de invierno').length).toBeGreaterThan(0);
    expect(screen.queryAllByText('Capacitación interna')).toHaveLength(0);

    // Click en botón Limpiar
    const limpiarButton = screen.getByRole('button', { name: /Limpiar/i });
    fireEvent.click(limpiarButton);

    // Ambos motivos deben estar presentes de nuevo
    expect(screen.getAllByText('Vacaciones de invierno').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Capacitación interna').length).toBeGreaterThan(0);
  });

  it('filtra solo excepciones en estado futura en Próximos 30 días y ordena por fecha de registro (fechaCreacion desc)', async () => {
    const today = new Date();
    const pastDate = new Date(today.getTime() - 5 * 86400000).toISOString().slice(0, 10);
    const futureDate1 = new Date(today.getTime() + 5 * 86400000).toISOString().slice(0, 10);
    const futureDate2 = new Date(today.getTime() + 10 * 86400000).toISOString().slice(0, 10);
    const futureDate3 = new Date(today.getTime() + 60 * 86400000).toISOString().slice(0, 10);

    vi.spyOn(api, 'get').mockResolvedValue([
      // Registrada primera (más antigua), pero para dentro de 60 días
      { id: 1, tipo: 'VACACIONES', motivo: 'Registro antiguo para diciembre', fechaInicio: futureDate3, fechaFin: futureDate3, activa: true, fechaCreacion: '2026-01-01T10:00:00Z' },
      // Registrada segunda, vigente hoy (inició en el pasado y termina en 5 días)
      { id: 2, tipo: 'BLOQUEO_HORARIO', motivo: 'Vigente hoy', fechaInicio: pastDate, fechaFin: futureDate1, activa: true, fechaCreacion: '2026-02-01T10:00:00Z' },
      // Registrada tercera, futura en 10 días
      { id: 3, tipo: 'HABILITACION_EXTRAORDINARIA', motivo: 'Futura próxima A', fechaInicio: futureDate2, fechaFin: futureDate2, activa: true, fechaCreacion: '2026-03-01T10:00:00Z' },
      // Registrada cuarta (la más reciente), futura en 5 días
      { id: 4, tipo: 'MODIFICACION_HORARIO', motivo: 'Futura próxima B', fechaInicio: futureDate1, fechaFin: futureDate1, activa: true, fechaCreacion: '2026-04-01T10:00:00Z' },
    ]);

    renderPage('exceptions');

    await screen.findAllByText('Futura próxima B');

    // 1. Verifica orden por fechaCreacion descendente: 4, 3, 2, 1
    const table = document.querySelector('tbody');
    const rows = [...table.querySelectorAll('tr')];
    const motives = rows.map((r) => r.querySelectorAll('td')[2]?.textContent);
    expect(motives).toEqual(['Futura próxima B', 'Futura próxima A', 'Vigente hoy', 'Registro antiguo para diciembre']);

    // 2. Clic en KPI "Próximos 30 días"
    const kpi30 = screen.getByText('Próximos 30 días');
    fireEvent.click(kpi30);

    // Solo deben figurar en estado FUTURA dentro de los 30 días ('Futura próxima B' y 'Futura próxima A')
    expect(screen.getAllByText('Futura próxima B').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Futura próxima A').length).toBeGreaterThan(0);
    expect(screen.queryAllByText('Vigente hoy')).toHaveLength(0);
    expect(screen.queryAllByText('Registro antiguo para diciembre')).toHaveLength(0);
  });
});


