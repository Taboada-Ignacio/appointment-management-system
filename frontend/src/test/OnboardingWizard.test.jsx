import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, it, expect, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { OnboardingWizard } from '../features/professional/components/OnboardingWizard';
import { ToastProvider } from '../components/ui/ToastProvider';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { api } from '../services/api';

function renderWizard(props = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <BrowserRouter>
          <OnboardingWizard onComplete={vi.fn()} {...props} />
        </BrowserRouter>
      </ToastProvider>
    </QueryClientProvider>
  );
}

describe('OnboardingWizard Component', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders Step 1 with explanatory texts for each parameter when initialStep is 1', () => {
    renderWizard({ initialStep: 1 });

    expect(screen.getByText('Parámetros de Atención y Turnos')).toBeInTheDocument();
    expect(screen.getByText('Duración estimada por turno')).toBeInTheDocument();
    expect(screen.getByText('Capacidad simultánea de atención')).toBeInTheDocument();
    expect(screen.getByText('Tiempo mínimo de cancelación anticipada')).toBeInTheDocument();
    expect(screen.getByText('Modalidad de gestión de turnos')).toBeInTheDocument();

    // Check explanatory text sections
    expect(screen.getByText(/Define el bloque de tiempo estándar asignado a cada cita/i)).toBeInTheDocument();
    expect(screen.getByText(/Indica cuántos pacientes o clientes podés recibir al mismo tiempo/i)).toBeInTheDocument();
    expect(screen.getByText(/Establece el plazo mínimo previo al inicio del turno/i)).toBeInTheDocument();
    expect(screen.getByText(/Si está activado, los pacientes no podrán solicitar turnos/i)).toBeInTheDocument();
  });

  it('starts at Step 2 and allows returning to apply parameter changes with PUT', async () => {
    const mockSavedConfig = {
      id: 1,
      profesionalId: 1,
      cantidadMaxTurnosALaVez: 2,
      duracionAproximadaPorTurno: 45,
      agendaSoloManejadaPorProfesional: true,
      umbralCancelacionHoras: 12,
    };

    const putSpy = vi.spyOn(api, 'put').mockResolvedValue(mockSavedConfig);
    renderWizard({ initialStep: 2, savedConfig: mockSavedConfig });

    expect(screen.getByText('Días de Atención y Franjas Horarias')).toBeInTheDocument();
    expect(screen.getByText(/Tus parámetros ya están guardados/i)).toBeInTheDocument();
    expect(screen.queryByText('Parámetros de Atención y Turnos')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Volver a parámetros/i }));
    expect(screen.getByText('Parámetros de Atención y Turnos')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Aplicar cambios y continuar/i }));
    await waitFor(() => expect(putSpy).toHaveBeenCalledWith(
      '/api/profesionales/1/configuracion',
      expect.objectContaining({ duracionAproximadaPorTurno: 45 }),
      expect.anything(),
    ));

    // Has "Repetir esta configuración por mes" switch checked by default and can be deselected
    const repeatSwitch = screen.getByLabelText(/Repetir configuración al mes siguiente/i);
    expect(repeatSwitch).toBeInTheDocument();
    expect(repeatSwitch).toBeChecked();

    // Deselect it
    fireEvent.click(repeatSwitch);
    expect(repeatSwitch).not.toBeChecked();
  });

  it('requires confirmation before triggering generation, then shows success and "Empezar a trabajar"', async () => {
    const postSpy = vi.spyOn(api, 'post').mockImplementation(async (url) => {
      if (url.includes('/configuracion')) {
        return {
          id: 1,
          profesionalId: 1,
          cantidadMaxTurnosALaVez: 1,
          duracionAproximadaPorTurno: 30,
          agendaSoloManejadaPorProfesional: false,
          umbralCancelacionHoras: 24,
        };
      }
      if (url.includes('/agendas/inicializacion')) {
        return {
          completado: true,
          agendasAnuales: [2026],
          mesesConfigurados: [
            { id: 9, anio: 2026, nroMes: 9, estado: 'ACTIVO', diasActivos: 22, diasInactivos: 8 },
            { id: 10, anio: 2026, nroMes: 10, estado: 'ACTIVO', diasActivos: 23, diasInactivos: 8 },
          ],
          diasLaborablesPorSemana: 5,
          repetidoAlMesSiguiente: true,
        };
      }
      return {};
    });

    const onCompleteMock = vi.fn();
    renderWizard({ initialStep: 1, onComplete: onCompleteMock });

    // 1. Submit Step 1
    const submitBtn1 = screen.getByRole('button', { name: /Guardar y continuar a horarios/i });
    fireEvent.click(submitBtn1);

    // 2. Wait for Step 2
    await waitFor(() => {
      expect(screen.getByText('Días de Atención y Franjas Horarias')).toBeInTheDocument();
    });

    // Check repeat switch is present and checked by default
    const repeatSwitch = screen.getByLabelText(/Repetir configuración al mes siguiente/i);
    expect(repeatSwitch).toBeInTheDocument();
    expect(repeatSwitch).toBeChecked();

    // 3. Click "Confirmar horarios y generar agenda" in Step 2 -> opens AlertDialog
    const promptConfirmBtn = screen.getByRole('button', { name: /Confirmar horarios y generar agenda/i });
    fireEvent.click(promptConfirmBtn);

    // 4. Verify confirmation dialog is visible with repeat notice
    await waitFor(() => {
      expect(screen.getByText('¿Confirmar horarios y generar agenda?')).toBeInTheDocument();
      expect(screen.getByText(/Repetición mensual:/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Confirmar e iniciar/i })).toBeInTheDocument();
    });

    // 5. Click "Confirmar e iniciar" in dialog
    const confirmActionBtn = screen.getByRole('button', { name: /Confirmar e iniciar/i });
    fireEvent.click(confirmActionBtn);

    // 6. Verify Final Success Screen appears
    await waitFor(() => {
      expect(screen.getByText('¡Todo listo! Tu consultorio está preparado para trabajar')).toBeInTheDocument();
      expect(screen.getByText('Repetir configuración al mes siguiente')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Empezar a trabajar \(Ir a Mi Día\)/i })).toBeInTheDocument();
    });

    expect(postSpy).toHaveBeenCalledWith(
      '/api/profesionales/1/agendas/inicializacion',
      expect.objectContaining({ repetirAlMesSiguiente: true }),
      expect.anything(),
    );

    // 7. Click "Empezar a trabajar"
    const startBtn = screen.getByRole('button', { name: /Empezar a trabajar \(Ir a Mi Día\)/i });
    fireEvent.click(startBtn);
    expect(onCompleteMock).toHaveBeenCalled();
  });

  it('returns to Step 2 and does not show success when atomic initialization fails', async () => {
    vi.spyOn(api, 'post').mockRejectedValue(new Error('No se pudo inicializar el calendario'));
    renderWizard({
      initialStep: 2,
      savedConfig: {
        id: 1,
        profesionalId: 1,
        cantidadMaxTurnosALaVez: 1,
        duracionAproximadaPorTurno: 30,
        agendaSoloManejadaPorProfesional: false,
        umbralCancelacionHoras: 24,
      },
    });

    fireEvent.click(screen.getByRole('button', { name: /Confirmar horarios y generar agenda/i }));
    fireEvent.click(await screen.findByRole('button', { name: /Confirmar e iniciar/i }));

    await waitFor(() => {
      expect(screen.getByText('Días de Atención y Franjas Horarias')).toBeInTheDocument();
      expect(screen.queryByText(/Todo listo/i)).not.toBeInTheDocument();
    });
  });
});
