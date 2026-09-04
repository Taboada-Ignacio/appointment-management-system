import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cancelAbsence, createAbsence, listAbsences, listAffectedAppointments, previewAbsence, previewAbsenceUpdate, resolveAffectedBulkCancellation, resolveAffectedCancellation, resolveAffectedReschedule, updateAbsence } from '../api/absenceApi';

export const absenceKeys = { all: ['professional', 'absences'] };
export const affectedKeys = { all: ['professional', 'affectedAppointments'] };

export function useAbsences() {
  return useQuery({ queryKey: absenceKeys.all, queryFn: () => listAbsences() });
}

export function useCancelAbsence() {
  const client = useQueryClient();
  return useMutation({ mutationFn: cancelAbsence, onSuccess: () => {
    client.invalidateQueries({ queryKey: absenceKeys.all });
    client.invalidateQueries({ queryKey: affectedKeys.all });
    client.invalidateQueries({ queryKey: ['professional'] });
  } });
}

export function useAffectedAppointments() {
  return useQuery({ queryKey: affectedKeys.all, queryFn: () => listAffectedAppointments() });
}

function useAffectedResolution(mutationFn) {
  const client = useQueryClient();
  return useMutation({ mutationFn, onSuccess: () => {
    client.invalidateQueries({ queryKey: affectedKeys.all });
    client.invalidateQueries({ queryKey: ['professional'] });
  } });
}

export function useResolveAffectedCancellation() {
  return useAffectedResolution(({ id, observacion }) => resolveAffectedCancellation(id, observacion));
}

export function useResolveAffectedReschedule() {
  return useAffectedResolution(({ id, ...payload }) => resolveAffectedReschedule(id, payload));
}

export function useResolveAffectedBulkCancellation() {
  return useAffectedResolution(({ ids, observacion }) => resolveAffectedBulkCancellation(ids, observacion));
}

export function usePreviewAbsence() {
  return useMutation({ mutationFn: previewAbsence });
}

export function useCreateAbsence() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: createAbsence,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: absenceKeys.all });
      client.invalidateQueries({ queryKey: ['professional'] });
    },
  });
}

export function usePreviewAbsenceUpdate() {
  return useMutation({ mutationFn: ({ id, payload }) => previewAbsenceUpdate(id, payload) });
}

export function useUpdateAbsence() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => updateAbsence(id, payload),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: absenceKeys.all });
      client.invalidateQueries({ queryKey: affectedKeys.all });
      client.invalidateQueries({ queryKey: ['professional'] });
    },
  });
}
