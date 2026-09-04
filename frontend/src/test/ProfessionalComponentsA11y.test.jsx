import { useRef, useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { AffectedAppointmentsDialog } from '../features/professional/components/AffectedAppointmentsDialog';
import { DailyTimeline } from '../features/professional/components/DailyTimeline';
import { MonthCalendar } from '../features/professional/components/MonthCalendar';
import { ProfessionalSidebar } from '../features/professional/components/ProfessionalSidebar';
import { WeeklyEditor } from '../features/professional/components/WeeklyEditor';
import { YearMonthCard } from '../features/professional/components/YearMonthCard';
import { agendaKeys } from '../features/professional/hooks/useAgenda';

function renderYearMonthCard(monthData, onViewMonth = vi.fn()) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(agendaKeys.monthDetail(monthData.id), monthData);

  return render(
    <QueryClientProvider client={queryClient}>
      <YearMonthCard year={2027} monthData={monthData} onViewMonth={onViewMonth} />
    </QueryClientProvider>
  );
}

describe('Professional component accessibility and contract honesty', () => {
  it('renders custom dialog content and requires a reason before confirming affected appointments', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();

    render(
      <AffectedAppointmentsDialog
        open
        onOpenChange={vi.fn()}
        onConfirm={onConfirm}
      />
    );

    expect(screen.getByText('No se puede consultar el impacto')).toBeInTheDocument();
    expect(screen.getByText(/no supone cantidades ni muestra turnos ficticios/i)).toBeInTheDocument();

    const confirmButton = screen.getByRole('button', { name: 'Registrar bajas con motivo' });
    expect(confirmButton).toBeDisabled();

    await user.type(screen.getByLabelText(/Motivo de baja administrativa/i), 'Cambio de guardia');
    expect(confirmButton).toBeEnabled();
    await user.click(confirmButton);

    expect(onConfirm).toHaveBeenCalledWith('Cambio de guardia');
  });

  it('allows ConfirmDialog consumers to render children', () => {
    render(
      <ConfirmDialog
        open
        onOpenChange={vi.fn()}
        title="Confirmar acción"
        description="Descripción"
        confirmLabel="Confirmar"
        onConfirm={vi.fn()}
      >
        <p>Contenido específico</p>
      </ConfirmDialog>
    );

    expect(screen.getByText('Contenido específico')).toBeInTheDocument();
  });

  it('matches calendar data only by date, normalizes null and uses roving arrow navigation', async () => {
    const user = userEvent.setup();
    const onSelectDay = vi.fn();

    render(
      <MonthCalendar
        year={2027}
        month={2}
        selectedDayId={81}
        days={[
          {
            id: 81,
            fecha: '2027-02-15',
            estadoActual: null,
            cantidadBrechas: 2,
            cantidadTurnosAsignados: 3,
            tiposExcepcion: ['VACACIONES'],
          },
        ]}
        onSelectDay={onSelectDay}
      />
    );

    expect(screen.getAllByText('Inactivo')).toHaveLength(1);
    expect(screen.getAllByText('2 brechas')).toHaveLength(1);
    expect(screen.getAllByText('3 turnos')).toHaveLength(1);
    expect(screen.getByText('Vacaciones')).toBeInTheDocument();

    const day15 = screen.getByRole('gridcell', { name: /15 de Febrero de 2027/i });
    const day16 = screen.getByRole('gridcell', { name: /16 de Febrero de 2027/i });
    const dayButtons = screen
      .getAllByRole('gridcell')
      .filter((cell) => cell.tagName === 'BUTTON');

    expect(dayButtons.filter((button) => button.tabIndex === 0)).toEqual([day15]);

    await user.click(day15);
    onSelectDay.mockClear();
    await user.keyboard('{ArrowRight}');
    expect(day16).toHaveFocus();
    expect(day16).toHaveAttribute('tabindex', '0');

    await user.keyboard('{Enter}');
    expect(onSelectDay).toHaveBeenCalledWith({
      id: '2027-02-16',
      fecha: '2027-02-16',
      empty: true,
    });
  });

  it('normalizes a raw null backend day status to INACTIVO', () => {
    render(
      <DailyTimeline
        day={{ fecha: '2020-01-01', estadoActual: null, brechas: [] }}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    expect(screen.getByLabelText('Estado: Inactivo')).toBeInTheDocument();
  });

  it('keeps explicit backend states even when the date is in the past', () => {
    render(
      <DailyTimeline
        day={{ fecha: '2020-01-01', estadoActual: 'ACTIVO', brechas: [] }}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    expect(screen.getByLabelText('Estado: Activo')).toBeInTheDocument();
  });

  it('does not invent annual availability or appointment counts', () => {
    renderYearMonthCard({ id: 9, nroMes: 2, estadoActual: null });

    expect(
      screen.getByRole('img', { name: /Configuración diaria no disponible en el resumen anual/i })
    ).toBeInTheDocument();
    expect(screen.getAllByText('—')).toHaveLength(3);
    expect(screen.queryByText(/Sin configurar/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/0 turnos/i)).not.toBeInTheDocument();
  });

  it('summarizes only the daily data actually present', () => {
    renderYearMonthCard({
      id: 9,
      nroMes: 2,
      estadoActual: 'INACTIVO',
      dias: [
        { fecha: '2027-02-01', cantidadBrechas: 1 },
        { fecha: '2027-02-02', cantidadBrechas: 0 },
        { fecha: '2027-02-03', cantidadBrechas: 0 },
      ],
    });

    expect(
      screen.getByRole('img', { name: /1 de 3 días consultados tienen brechas/i })
    ).toBeInTheDocument();
    expect(screen.getByText('1 de 3 consultados')).toBeInTheDocument();
    expect(screen.getByText('33%')).toBeInTheDocument();
  });

  it('unmounts the closed mobile drawer and restores focus after Escape', async () => {
    const user = userEvent.setup();

    function SidebarHarness() {
      const [open, setOpen] = useState(false);
      const triggerRef = useRef(null);
      return (
        <MemoryRouter initialEntries={['/profesional/mi-dia']}>
          <button ref={triggerRef} type="button" onClick={() => setOpen(true)}>
            Abrir navegación
          </button>
          <ProfessionalSidebar
            open={open}
            onClose={() => setOpen(false)}
            returnFocusRef={triggerRef}
          />
        </MemoryRouter>
      );
    }

    render(<SidebarHarness />);
    const trigger = screen.getByRole('button', { name: 'Abrir navegación' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await user.click(trigger);
    const drawer = screen.getByRole('dialog', { name: 'Navegación profesional' });
    expect(within(drawer).getByRole('button', { name: 'Cerrar menú' })).toHaveFocus();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('provides a visible focus treatment for weekly switches', () => {
    render(<WeeklyEditor initialDraft={{ diasSemana: [] }} />);

    const mondaySwitch = screen.getByRole('switch', {
      name: 'Habilitar atención los días Lunes',
    });
    expect(mondaySwitch).toHaveClass(
      'focus-visible:ring-3',
      'focus-visible:ring-ring/50'
    );
  });
});
