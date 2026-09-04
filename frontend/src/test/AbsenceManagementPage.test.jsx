import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../components/ui/ToastProvider';
import { AbsenceManagementPage } from '../features/professional/pages/AbsenceManagementPage';
import { api } from '../services/api';

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter initialEntries={['/profesional/ausencias']}>
          <AbsenceManagementPage />
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
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /Desde:/ }));

    await waitFor(() => {
      const weekdays = [...document.querySelectorAll('.rdp-weekday')];
      expect(weekdays.map((day) => day.textContent)).toEqual(['lu', 'ma', 'mi', 'ju', 'vi', 'sá', 'do']);
    });
  });

  it('muestra el resumen al confirmar una ausencia con el contrato REST en español', async () => {
    vi.spyOn(api, 'post').mockImplementation(async (url) => {
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
        excepcion: { id: 10 },
        impacto: {
          cantidadTurnosAfectados: 0,
          cantidadNotificacionesWhatsApp: 0,
          cantidadSinNotificacion: 0,
          turnosAfectados: [],
        },
      };
    });

    renderPage();
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
  });
});
