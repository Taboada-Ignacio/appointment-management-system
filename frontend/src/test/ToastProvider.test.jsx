import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { ToastProvider, useToast } from '../components/ui/ToastProvider';

function TestConsumer() {
  const { success, error, info } = useToast();
  return (
    <div>
      <button onClick={() => success('Éxito', 'Operación completada.')}>Trigger Success</button>
      <button onClick={() => error('Error', 'Falló la conexión.')}>Trigger Error</button>
      <button onClick={() => info('Aviso', 'Información relevante.')}>Trigger Info</button>
    </div>
  );
}

describe('ToastProvider & useToast', () => {
  it('renders and displays toasts on trigger', async () => {
    const user = userEvent.setup();
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    await user.click(screen.getByText('Trigger Success'));
    expect(screen.getByText('Éxito')).toBeInTheDocument();
    expect(screen.getByText('Operación completada.')).toBeInTheDocument();

    await user.click(screen.getByText('Trigger Error'));
    expect(screen.getByText('Error')).toBeInTheDocument();
    expect(screen.getByText('Falló la conexión.')).toBeInTheDocument();
  });

  it('allows dismissing a toast via close button', async () => {
    const user = userEvent.setup();
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    await user.click(screen.getByText('Trigger Info'));
    expect(screen.getByText('Aviso')).toBeInTheDocument();

    const closeBtn = screen.getByLabelText('Cerrar notificación');
    await user.click(closeBtn);
    expect(screen.queryByText('Aviso')).not.toBeInTheDocument();
  });
});

