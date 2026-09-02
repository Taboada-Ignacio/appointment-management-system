import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, it, expect, vi } from 'vitest';
import { App } from '../App';
import { api } from '../services/api';

const mockConfig = {
  id: 1,
  profesionalId: 1,
  cantidadMaxTurnosALaVez: 1,
  duracionAproximadaPorTurno: 30,
  agendaSoloManejadaPorProfesional: false,
  umbralCancelacionHoras: 24,
};

const mockAgendas = [{ id: 1, anio: 2026, fechaCreacion: '2026-01-01' }];

describe('App Root Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders professional panel layout and redirects to Mi día when configured and annual agenda exists', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url) => {
      if (url.includes('/configuracion')) {
        return mockConfig;
      }
      if (url.includes('/agendas')) {
        return mockAgendas;
      }
      return [];
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Centro de agenda')).toBeInTheDocument();
      expect(screen.getByRole('heading', { level: 1, name: /Mi día/i })).toBeInTheDocument();
    });
  });

  it('renders professional sidebar navigation links when fully configured', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url) => {
      if (url.includes('/configuracion')) {
        return mockConfig;
      }
      if (url.includes('/agendas')) {
        return mockAgendas;
      }
      return [];
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /Mi día/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Mi mes/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Mi año/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Configuración/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Cambiar mi semana/i })).toBeInTheDocument();
    });
  });

  it('renders OnboardingWizard at Step 1 when professional has no configuration', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url) => {
      if (url.includes('/configuracion')) {
        const error = new Error('No encontrado');
        error.status = 404;
        throw error;
      }
      return [];
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Puesta en marcha del consultorio')).toBeInTheDocument();
      expect(screen.getByText('Parámetros de Atención y Turnos')).toBeInTheDocument();
    });
  });

  it('renders OnboardingWizard at Step 2 when config exists but annual agenda is not created', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url) => {
      if (url.includes('/configuracion')) {
        return mockConfig;
      }
      if (url.includes('/agendas')) {
        return [];
      }
      return [];
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Puesta en marcha del consultorio')).toBeInTheDocument();
      expect(screen.getByText('Días de Atención y Franjas Horarias')).toBeInTheDocument();
      expect(screen.getByText(/Tus parámetros ya están guardados/i)).toBeInTheDocument();
    });
  });
});
