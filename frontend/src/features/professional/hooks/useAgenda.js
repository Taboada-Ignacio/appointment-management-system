import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listAnnualAgendas,
  createAnnualAgenda,
  listMonths,
  getMonth,
  generateMonthDays,
  configureMonthWeek,
  configureMonthDates,
  activateMonth,
  deactivateMonth,
  setRepeatMonth,
  repeatMonth,
  listSelectableDays,
  getDay,
  configureDay
} from '../api/agendaApi';
import { professionalContext } from '../../../config/professional';
import { validateGaps } from '../../../utils/gaps';

export const agendaKeys = {
  all: () => ['professional', professionalContext.id],
  agendas: () => [...agendaKeys.all(), 'agendas'],
  yearAgenda: (anio) => [...agendaKeys.agendas(), anio],
  months: (anio) => [...agendaKeys.all(), 'months', anio],
  monthDetail: (mesAgendaId) => [...agendaKeys.all(), 'month', mesAgendaId],
  selectableDays: (desde, hasta) => [...agendaKeys.all(), 'selectableDays', desde, hasta],
  dayDetail: (diaAgendaId) => [...agendaKeys.all(), 'day', diaAgendaId],
};

export function useAnnualAgendas() {
  return useQuery({
    queryKey: agendaKeys.agendas(),
    queryFn: () => listAnnualAgendas(),
  });
}

export function useMonths(anio, { enabled = true } = {}) {
  return useQuery({
    queryKey: agendaKeys.months(anio),
    queryFn: () => listMonths(anio),
    enabled: Boolean(anio) && enabled,
  });
}

export function useMonthDetail(mesAgendaId) {
  return useQuery({
    queryKey: agendaKeys.monthDetail(mesAgendaId),
    queryFn: () => getMonth(mesAgendaId),
    enabled: Boolean(mesAgendaId),
  });
}

export function useSelectableDays(desde, hasta) {
  return useQuery({
    queryKey: agendaKeys.selectableDays(desde, hasta),
    queryFn: () => listSelectableDays({ desde, hasta }),
    enabled: Boolean(desde && hasta),
  });
}

export function useDayDetail(diaAgendaId) {
  return useQuery({
    queryKey: agendaKeys.dayDetail(diaAgendaId),
    queryFn: () => getDay(diaAgendaId),
    enabled: Boolean(diaAgendaId),
  });
}

export function useCreateAnnualAgenda() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {number} */ anio) => createAnnualAgenda(anio),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.agendas() });
    },
  });
}

export function useConfigureMonthWeekly() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {{mesAgendaId: number|string, diasSemana: Array<{diaSemana: string, brechas: Array<{horaInicio: string, horaFin: string}>}>}} */ { mesAgendaId, diasSemana }) => {
      const invalidDay = diasSemana?.find(({ brechas }) => !validateGaps(brechas).valid);
      if (invalidDay) {
        const validation = validateGaps(invalidDay.brechas);
        throw new Error(`La configuración de ${invalidDay.diaSemana || 'un día'} no es válida. ${validation.errors.join(' ')}`);
      }
      return configureMonthWeek(mesAgendaId, diasSemana);
    },
    onSuccess: (_, { mesAgendaId }) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.monthDetail(mesAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'months'] });
    },
  });
}

export function useConfigureMonthDaily() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {{mesAgendaId: number|string, dias: Array<{fecha: string, brechas: Array<{horaInicio: string, horaFin: string}>}>}} */ { mesAgendaId, dias }) => {
      const invalidDay = dias?.find(({ brechas }) => !validateGaps(brechas).valid);
      if (invalidDay) {
        const validation = validateGaps(invalidDay.brechas);
        throw new Error(`Las brechas de ${invalidDay.fecha || 'la fecha seleccionada'} no son válidas. ${validation.errors.join(' ')}`);
      }
      return configureMonthDates(mesAgendaId, dias);
    },
    onSuccess: (_, { mesAgendaId }) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.monthDetail(mesAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'months'] });
    },
  });
}

export function useActivateMonth() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {number|string} */ mesAgendaId) => activateMonth(mesAgendaId),
    onSuccess: (_, mesAgendaId) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.monthDetail(mesAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'months'] });
    },
  });
}

export function useInactivateMonth() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {number|string} */ mesAgendaId) => deactivateMonth(mesAgendaId),
    onSuccess: (_, mesAgendaId) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.monthDetail(mesAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'months'] });
    },
  });
}

export function useUpdateDayGaps() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {{diaAgendaId: number|string, brechas: Array<{horaInicio: string, horaFin: string}>}} */ { diaAgendaId, brechas }) => {
      const validation = validateGaps(brechas);
      if (!validation.valid) {
        throw new Error(`Las brechas del día no son válidas. ${validation.errors.join(' ')}`);
      }
      return configureDay(diaAgendaId, brechas);
    },
    onSuccess: (_, { diaAgendaId }) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.dayDetail(diaAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'selectableDays'] });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'month'] });
    },
  });
}

export function useRepeatMonth() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {number|string} */ mesAgendaId) => repeatMonth(mesAgendaId),
    onSuccess: (_, mesAgendaId) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.monthDetail(mesAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'months'] });
    },
  });
}

export function useSetRepeatConfig() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {{mesAgendaId: number|string, repetirConfiguracion: boolean}} */ { mesAgendaId, repetirConfiguracion }) => setRepeatMonth(mesAgendaId, repetirConfiguracion),
    onSuccess: (_, { mesAgendaId }) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.monthDetail(mesAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'months'] });
    },
  });
}

export function useGenerateMonthDays() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (/** @type {number|string} */ mesAgendaId) => generateMonthDays(mesAgendaId),
    onSuccess: (_, mesAgendaId) => {
      queryClient.invalidateQueries({ queryKey: agendaKeys.monthDetail(mesAgendaId) });
      queryClient.invalidateQueries({ queryKey: [...agendaKeys.all(), 'months'] });
    },
  });
}

export function useAgendaMonth(year, month) {
  const agendasQuery = useAnnualAgendas();
  const agendas = agendasQuery.data;
  
  const agenda = agendas?.find(a => a.anio === year) ?? null;
  
  const monthsQuery = useMonths(agenda ? year : null);
  const months = monthsQuery.data;
  
  const monthData = months?.find(m => m.nroMes === month) ?? null;
  
  const monthQuery = useMonthDetail(monthData?.id);
  
  return {
    agenda,
    month: monthQuery.data ?? monthData,
    year,
    agendaQuery: agendasQuery,
    monthsQuery,
    monthQuery,
  };
}
