import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CompleteRescheduleAppointmentPage } from '../features/professional/pages/CompleteRescheduleAppointmentPage';

const mutateAsync = vi.fn();
const resolveAffectedAsync = vi.fn();
const validateAppointmentReschedule = vi.fn();
const listSuggestedTimes = vi.fn();

vi.mock('../features/professional/hooks/useAgenda', () => ({
  useAssignedAppointments: () => ({ data: [], isLoading: false }),
  useSelectableDays: () => ({ data: [{ id: 22, diaAgendaId: 22, fecha: '2026-09-18', seleccionable: true, estado: 'ACTIVO' }], isLoading: false }),
  useDayDetail: () => ({ data: { id: 22, fecha: '2026-09-18', estadoActual: 'ACTIVO', brechas: [{ horaInicio: '09:00', horaFin: '12:00' }] } }),
  useRescheduleAppointment: () => ({ mutateAsync, isPending: false }),
}));
vi.mock('../features/professional/hooks/useAbsences', () => ({
  useAffectedAppointments: () => ({ data: [{
    afectacionId: 7, turnoId: 44, resolucion: 'PENDIENTE', estadoTurno: 'AFECTADO_POR_EXCEPCION',
    clienteId: 8, nombreCliente: 'Ana Paz', telefono: '1111', fechaActual: '2026-09-15',
    inicioActual: '2026-09-15T13:00:00Z', finActual: '2026-09-15T13:30:00Z',
  }], isLoading: false }),
  useResolveAffectedReschedule: () => ({ mutateAsync: resolveAffectedAsync, isPending: false }),
}));

vi.mock('../features/professional/api/agendaApi', () => ({ validateAppointmentReschedule: (...args) => validateAppointmentReschedule(...args) }));
vi.mock('../features/professional/api/appointmentApi', () => ({ listSuggestedTimes: (...args) => listSuggestedTimes(...args) }));
vi.mock('../components/ui/ToastProvider', () => ({ useToast: () => ({ success: vi.fn(), error: vi.fn() }) }));
vi.mock('../features/professional/components/MonthCalendar', () => ({
  MonthCalendar: ({ days, onSelectDay }) => <button onClick={() => onSelectDay(days[0])}>Elegir 18 de septiembre</button>,
}));
vi.mock('../features/professional/components/DailyTimeline', () => ({
  DailyTimeline: ({ candidateSlots, onSelectCandidate }) => candidateSlots[0]
    ? <button onClick={() => onSelectCandidate(candidateSlots[0])}>Elegir 10:00–10:30</button>
    : <span>Cargando horarios</span>,
}));

describe('CompleteRescheduleAppointmentPage', () => {
  beforeEach(() => {
    mutateAsync.mockReset().mockResolvedValue({});
    resolveAffectedAsync.mockReset().mockResolvedValue({});
    validateAppointmentReschedule.mockReset().mockResolvedValue({});
    listSuggestedTimes.mockReset().mockResolvedValue([{ horaInicio: '10:00', horaFin: '10:30', advertencias: [] }]);
  });

  it('mantiene el cliente, permite elegir día y horario y llega a confirmación', async () => {
    render(<MemoryRouter initialEntries={['/profesional/turnos/44/reprogramar?fecha=2026-09-15&afectacionId=7']}><Routes><Route path="/profesional/turnos/:appointmentId/reprogramar" element={<CompleteRescheduleAppointmentPage />} /></Routes></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Elegir 18 de septiembre' }));
    fireEvent.click(screen.getByRole('button', { name: 'Elegir 18 de septiembre' }));
    expect(await screen.findByText('Ana Paz')).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: /Nombre, apellido o DNI/i })).not.toBeInTheDocument();
    fireEvent.click(await screen.findByRole('button', { name: 'Elegir 10:00–10:30' }));
    fireEvent.click(screen.getByRole('button', { name: 'Continuar a confirmación' }));

    await waitFor(() => expect(validateAppointmentReschedule).toHaveBeenCalled());
    expect(await screen.findByRole('heading', { name: 'Confirmar reprogramación' })).toBeInTheDocument();
    expect(screen.getByText('2026-09-18 · 10:00–10:30')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar reprogramación' }));
    await waitFor(() => expect(resolveAffectedAsync).toHaveBeenCalledWith(expect.objectContaining({ id: '7', nuevoDiaAgendaId: 22 })));
    expect(mutateAsync).not.toHaveBeenCalled();
  });
});
