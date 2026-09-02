import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  useConfigureMonthDaily,
  useConfigureMonthWeekly,
  useMonths,
  useUpdateDayGaps,
} from '../features/professional/hooks/useAgenda';

const apiMocks = vi.hoisted(() => ({
  listAnnualAgendas: vi.fn(),
  createAnnualAgenda: vi.fn(),
  listMonths: vi.fn(),
  getMonth: vi.fn(),
  generateMonthDays: vi.fn(),
  configureMonthWeek: vi.fn(),
  configureMonthDates: vi.fn(),
  activateMonth: vi.fn(),
  deactivateMonth: vi.fn(),
  setRepeatMonth: vi.fn(),
  repeatMonth: vi.fn(),
  listSelectableDays: vi.fn(),
  getDay: vi.fn(),
  configureDay: vi.fn(),
}));

vi.mock('../features/professional/api/agendaApi', () => apiMocks);

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return function Wrapper({ children }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('agenda hooks safety boundaries', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does not request months while the annual agenda gate is closed', async () => {
    renderHook(() => useMonths(2026, { enabled: false }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(apiMocks.listMonths).not.toHaveBeenCalled();
    });
  });

  it('rejects overlapping weekly gaps before calling the API', async () => {
    const { result } = renderHook(() => useConfigureMonthWeekly(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await expect(
        result.current.mutateAsync({
          mesAgendaId: 9,
          diasSemana: [
            {
              diaSemana: 'MONDAY',
              brechas: [
                { horaInicio: '09:00', horaFin: '12:00' },
                { horaInicio: '11:00', horaFin: '13:00' },
              ],
            },
          ],
        })
      ).rejects.toThrow('superponen');
    });

    expect(apiMocks.configureMonthWeek).not.toHaveBeenCalled();
  });

  it('rejects invalid date gaps before calling the API', async () => {
    const { result } = renderHook(() => useConfigureMonthDaily(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await expect(
        result.current.mutateAsync({
          mesAgendaId: 9,
          dias: [
            {
              fecha: '2026-09-10',
              brechas: [{ horaInicio: '14:00', horaFin: '13:00' }],
            },
          ],
        })
      ).rejects.toThrow('no son válidas');
    });

    expect(apiMocks.configureMonthDates).not.toHaveBeenCalled();
  });

  it('rejects incomplete day gaps before calling the API', async () => {
    const { result } = renderHook(() => useUpdateDayGaps(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await expect(
        result.current.mutateAsync({
          diaAgendaId: 101,
          brechas: [{ horaInicio: '09:00', horaFin: '' }],
        })
      ).rejects.toThrow('no son válidas');
    });

    expect(apiMocks.configureDay).not.toHaveBeenCalled();
  });
});
