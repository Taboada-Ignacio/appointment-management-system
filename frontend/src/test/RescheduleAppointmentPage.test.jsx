import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { RescheduleAppointmentPage } from '../features/professional/pages/RescheduleAppointmentPage';

const fixtures = vi.hoisted(() => ({
  days: [{ id: 22, diaAgendaId: 22, fecha: '2026-09-18', seleccionable: true, estado: 'ACTIVO' }],
  affected: [{
    afectacionId: 7, turnoId: 44, resolucion: 'PENDIENTE', clienteId: 8,
    nombreCliente: 'Ana Paz', fechaActual: '2026-09-15', estadoTurno: 'AFECTADO_POR_EXCEPCION',
    inicioActual: '2026-09-15T13:00:00Z', finActual: '2026-09-15T13:30:00Z',
  }],
}));

vi.mock('../features/professional/hooks/useAgenda', () => ({
  useAssignedAppointments: () => ({ data: [], isLoading: false }),
  useSelectableDays: () => ({ data: fixtures.days, isLoading: false }),
  useDayDetail: () => ({ data: null }),
  useRescheduleAppointment: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));
vi.mock('../features/professional/hooks/useAbsences', () => ({
  useAffectedAppointments: () => ({ data: fixtures.affected, isLoading: false }),
  useResolveAffectedReschedule: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));
vi.mock('../features/professional/api/agendaApi', () => ({ validateAppointmentReschedule: vi.fn().mockResolvedValue({}) }));
vi.mock('../features/professional/api/appointmentApi', () => ({ listSuggestedTimes: vi.fn().mockResolvedValue([]) }));
vi.mock('../components/ui/ToastProvider', () => ({ useToast: () => ({ success: vi.fn() }) }));
vi.mock('../features/professional/components/MonthCalendar', () => ({
  MonthCalendar: ({ days, onSelectDay }) => <button disabled={!days[0]?.seleccionable} onClick={() => onSelectDay(days[0])}>Elegir día disponible</button>,
}));
vi.mock('../features/professional/components/DailyTimeline', () => ({ DailyTimeline: () => null }));

describe('RescheduleAppointmentPage', () => {
  it('el segundo clic en el día seleccionado continúa a la confirmación', async () => {
    render(<MemoryRouter initialEntries={['/profesional/turnos/44/cambiar-dia?fecha=2026-09-15&afectacionId=7']}><Routes><Route path="/profesional/turnos/:appointmentId/cambiar-dia" element={<RescheduleAppointmentPage />} /></Routes></MemoryRouter>);

    const day = screen.getByRole('button', { name: 'Elegir día disponible' });
    await waitFor(() => expect(day).toBeEnabled());
    fireEvent.click(day);
    expect(await screen.findByText('Horario disponible')).toBeInTheDocument();
    fireEvent.click(day);

    expect(await screen.findByText('2. Revisar y confirmar')).toBeInTheDocument();
  });
});
