import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import { createRoutes } from '../app/router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider } from '../components/ui/ToastProvider';

const mockConfig = {
  id: 1,
  profesionalId: 1,
  cantidadMaxTurnosALaVez: 1,
  duracionAproximadaPorTurno: 30,
  agendaSoloManejadaPorProfesional: false,
  umbralCancelacionHoras: 24,
};

const mockAgendas = [{ id: 1, anio: 2026, fechaCreacion: '2026-01-01' }];

function renderWithRouter(initialEntries = ['/'], config = mockConfig, agendas = mockAgendas) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  if (config !== undefined) {
    queryClient.setQueryData(['professional', 1, 'config'], config);
  }

  if (agendas !== undefined) {
    queryClient.setQueryData(['professional', 1, 'agendas'], agendas);
  }

  const router = createMemoryRouter(createRoutes(), { initialEntries });

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <RouterProvider router={router} />
      </ToastProvider>
    </QueryClientProvider>
  );
}

describe('Routing & Shell Navigation', () => {
  it('redirects root "/" to "/profesional/mi-dia"', async () => {
    renderWithRouter(['/']);
    expect(screen.getByRole('heading', { name: /Mi día/i })).toBeInTheDocument();
  });

  it('renders Sidebar navigation links when config and annual agenda exist', () => {
    renderWithRouter(['/profesional/mi-dia']);
    expect(screen.getByRole('link', { name: /Mi día/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Mi mes/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Mi año/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Configuración/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Cambiar mi semana/i })).toBeInTheDocument();
  });

  it('renders blocking OnboardingWizard at Step 1 when professional has no configuration', () => {
    renderWithRouter(['/profesional/mi-dia'], null, []);
    expect(screen.getByText('Puesta en marcha del consultorio')).toBeInTheDocument();
    expect(screen.getByText('Parámetros de Atención y Turnos')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Mi día/i })).not.toBeInTheDocument();
  });

  it('renders blocking OnboardingWizard at Step 2 when config exists but annual agenda is missing', () => {
    renderWithRouter(['/profesional/mi-dia'], mockConfig, []);
    expect(screen.getByText('Puesta en marcha del consultorio')).toBeInTheDocument();
    expect(screen.getByText('Días de Atención y Franjas Horarias')).toBeInTheDocument();
    expect(screen.getByText(/Tus parámetros ya están guardados/i)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Mi día/i })).not.toBeInTheDocument();
  });

  it('renders 404 NotFoundPage on unknown routes', () => {
    renderWithRouter(['/ruta-inexistente']);
    expect(screen.getByRole('heading', { name: /Página no encontrada/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Volver a Mi Día/i })).toBeInTheDocument();
  });
});
