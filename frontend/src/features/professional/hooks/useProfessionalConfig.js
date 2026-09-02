import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getProfessionalConfig,
  registerProfessionalConfig,
  updateProfessionalConfig,
} from '../api/professionalApi';
import { professionalContext } from '../../../config/professional';

export const professionalConfigKeys = {
  all: (professionalId = professionalContext.id) => ['professional', professionalId],
  config: (professionalId = professionalContext.id) => [...professionalConfigKeys.all(professionalId), 'config'],
};

export function useProfessionalConfig(professionalId = professionalContext.id) {
  return useQuery({
    queryKey: professionalConfigKeys.config(professionalId),
    queryFn: async () => {
      try {
        return await getProfessionalConfig(professionalId);
      } catch (error) {
        if (error?.status === 404) {
          return null;
        }
        throw error;
      }
    },
    enabled: Boolean(professionalId),
  });
}

export function useRegisterProfessionalConfig() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (configData) => registerProfessionalConfig(configData, professionalContext.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: professionalConfigKeys.config(professionalContext.id) });
      queryClient.invalidateQueries({ queryKey: professionalConfigKeys.all(professionalContext.id) });
    },
  });
}

export function useUpdateProfessionalConfig() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (configData) => updateProfessionalConfig(configData, professionalContext.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: professionalConfigKeys.config(professionalContext.id) });
      queryClient.invalidateQueries({ queryKey: professionalConfigKeys.all(professionalContext.id) });
    },
  });
}

