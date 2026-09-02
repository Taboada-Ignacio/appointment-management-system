import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { OnboardingStepper } from '../features/professional/components/OnboardingStepper';
import { ToastProvider } from '../components/ui/ToastProvider';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';

function renderStepper(agendaState) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <BrowserRouter>
          <OnboardingStepper agendaState={agendaState} />
        </BrowserRouter>
      </ToastProvider>
    </QueryClientProvider>
  );
}

describe('OnboardingStepper Component', () => {
  it('renders all 4 onboarding steps', () => {
    renderStepper({
      agenda: null,
      month: null,
      year: 2026,
    });

    expect(screen.getByText('Configurar mi semana')).toBeInTheDocument();
    expect(screen.getByText('Generar agenda anual')).toBeInTheDocument();
    expect(screen.getByText('Configurar un mes')).toBeInTheDocument();
    expect(screen.getByText('Activar el mes')).toBeInTheDocument();
  });

  it('allows dismissing the stepper when agenda exists', async () => {
    const user = userEvent.setup();
    renderStepper({
      agenda: { id: 1, anio: 2026 },
      month: null,
      year: 2026,
    });

    const dismissBtn = screen.getByLabelText('Continuar más tarde');
    expect(dismissBtn).toBeInTheDocument();
    await user.click(dismissBtn);

    expect(screen.queryByText('Prepará tu agenda para recibir turnos')).not.toBeInTheDocument();
  });
});
