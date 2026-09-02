import { professionalContext } from '../../../config/professional';
import { api } from '../../../services/api';

function professionalBasePath(professionalId = professionalContext.id) {
  return `/api/profesionales/${encodeURIComponent(professionalId)}`;
}

const actorOptions = Object.freeze({
  headers: Object.freeze({ 'X-Usuario': professionalContext.actor }),
});

export function getProfessional(professionalId = professionalContext.id) {
  return api.get(professionalBasePath(professionalId));
}

export function getProfessionalConfig(professionalId = professionalContext.id) {
  return api.get(`${professionalBasePath(professionalId)}/configuracion`);
}

export function registerProfessionalConfig(configData, professionalId = professionalContext.id) {
  return api.post(
    `${professionalBasePath(professionalId)}/configuracion`,
    configData,
    actorOptions
  );
}

export function updateProfessionalConfig(configData, professionalId = professionalContext.id) {
  return api.put(
    `${professionalBasePath(professionalId)}/configuracion`,
    configData,
    actorOptions
  );
}

export const professionalApi = {
  getProfessional,
  getProfessionalConfig,
  registerProfessionalConfig,
  updateProfessionalConfig,
};

export default professionalApi;

