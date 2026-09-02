import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, it, expect } from 'vitest';
import { MonthConfigurator } from '../features/professional/components/MonthConfigurator';
import { WEEKLY_TEMPLATE_KEY } from '../features/professional/components/WeeklyEditor';
import { ToastProvider } from '../components/ui/ToastProvider';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';

function renderConfigurator(monthData, year = 2026) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const buildUi = (nextMonthData, nextYear = year) => (
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <BrowserRouter>
            <MonthConfigurator monthData={nextMonthData} year={nextYear} />
          </BrowserRouter>
        </ToastProvider>
      </QueryClientProvider>
    );

  const result = render(buildUi(monthData, year));
  return {
    ...result,
    rerenderConfigurator(nextMonthData, nextYear = year) {
      result.rerender(buildUi(nextMonthData, nextYear));
    },
  };
}

describe('MonthConfigurator Component', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders mode tabs and activation button state', () => {
    const emptyMonth = {
      id: 9,
      nroMes: 9,
      anio: 2026,
      estadoActual: 'INACTIVO',
      repetirConfiguracion: false,
      dias: [],
    };

    renderConfigurator(emptyMonth);

    expect(screen.getByText('Aplicar plantilla semanal')).toBeInTheDocument();
    expect(screen.getByText('Configurar por fecha puntual')).toBeInTheDocument();

    const activateBtn = screen.getByRole('button', { name: /Activar mes/i });
    expect(activateBtn).toBeDisabled(); // Disabled because month has no gaps configured
  });

  it('enables activation button when month has configured days', () => {
    const configuredMonth = {
      id: 9,
      nroMes: 9,
      anio: 2026,
      estadoActual: 'INACTIVO',
      repetirConfiguracion: false,
      dias: [{ id: 101, fecha: '2026-09-15', cantidadBrechas: 2 }],
    };

    renderConfigurator(configuredMonth);

    const activateBtn = screen.getByRole('button', { name: /Activar mes/i });
    expect(activateBtn).toBeEnabled();
  });

  it('only enables immediate replication when the backend repeat flag is active', () => {
    const configuredMonth = {
      id: 9,
      nroMes: 9,
      anio: 2026,
      estadoActual: 'INACTIVO',
      repetirConfiguracion: false,
      dias: [{ id: 101, fecha: '2026-09-15', cantidadBrechas: 2 }],
    };

    const { rerenderConfigurator } = renderConfigurator(configuredMonth);
    const repeatButton = screen.getByRole('button', { name: /Replicar ahora/i });
    expect(repeatButton).toBeDisabled();

    rerenderConfigurator({ ...configuredMonth, repetirConfiguracion: true });
    expect(screen.getByRole('button', { name: /Replicar ahora/i })).toBeEnabled();
  });

  it('explains and blocks the unsupported December-to-January replication', () => {
    renderConfigurator({
      id: 12,
      nroMes: 12,
      anio: 2026,
      estadoActual: 'INACTIVO',
      repetirConfiguracion: true,
      dias: [{ id: 1201, fecha: '2026-12-15', cantidadBrechas: 1 }],
    });

    expect(screen.getByText('Límite del contrato anual')).toBeInTheDocument();
    expect(screen.getByRole('checkbox')).toBeDisabled();
    expect(screen.getByRole('button', { name: /Replicar ahora/i })).toBeDisabled();
  });

  it('blocks weekly mode completely when the real month detail has configured gaps', () => {
    localStorage.setItem(
      WEEKLY_TEMPLATE_KEY,
      JSON.stringify({
        diasSemana: [
          {
            diaSemana: 'MONDAY',
            brechas: [{ horaInicio: '09:00', horaFin: '13:00' }],
          },
        ],
      })
    );

    renderConfigurator({
      id: 9,
      nroMes: 9,
      anio: 2026,
      estadoActual: 'INACTIVO',
      dias: [{ id: 101, fecha: '2026-09-15', cantidadBrechas: 2 }],
    });

    expect(screen.getByText('Aplicación semanal bloqueada')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Aplicar plantilla al mes/i })).toBeDisabled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('uses the real last day of the month and blocks overwriting a configured date', () => {
    renderConfigurator({
      id: 9,
      nroMes: 9,
      anio: 2026,
      estadoActual: 'INACTIVO',
      dias: [{ id: 101, fecha: '2026-09-15', cantidadBrechas: 1 }],
    });

    fireEvent.click(screen.getByRole('tab', { name: /Configurar por fecha puntual/i }));
    const dateInput = screen.getByLabelText('Fecha a configurar');
    expect(dateInput).toHaveAttribute('max', '2026-09-30');

    fireEvent.change(dateInput, { target: { value: '2026-09-15' } });

    expect(screen.getByText('Fecha protegida contra sobreescritura')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Guardar brechas para esta fecha/i })).toBeDisabled();
  });

  it('resets the selected date when the configured month changes', async () => {
    const september = {
      id: 9,
      nroMes: 9,
      anio: 2026,
      estadoActual: 'INACTIVO',
      dias: [],
    };
    const february = {
      id: 2,
      nroMes: 2,
      anio: 2026,
      estadoActual: 'INACTIVO',
      dias: [],
    };
    const { rerenderConfigurator } = renderConfigurator(september);

    fireEvent.click(screen.getByRole('tab', { name: /Configurar por fecha puntual/i }));
    fireEvent.change(screen.getByLabelText('Fecha a configurar'), {
      target: { value: '2026-09-20' },
    });

    rerenderConfigurator(february);

    await waitFor(() => {
      expect(screen.getByLabelText('Fecha a configurar')).toHaveValue('2026-02-01');
    });
    expect(screen.getByLabelText('Fecha a configurar')).toHaveAttribute('max', '2026-02-28');
  });

  it('keeps the date mutation disabled while gaps are invalid', () => {
    renderConfigurator({
      id: 9,
      nroMes: 9,
      anio: 2026,
      estadoActual: 'INACTIVO',
      dias: [],
    });

    fireEvent.click(screen.getByRole('tab', { name: /Configurar por fecha puntual/i }));
    fireEvent.change(screen.getByLabelText('Brecha 1: Hora de inicio'), {
      target: { value: '14:00' },
    });

    expect(screen.getByRole('button', { name: /Guardar brechas para esta fecha/i })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('debe ser anterior');
  });
});
