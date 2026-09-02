const DEFAULT_PROFESSIONAL = Object.freeze({
  id: 1,
  name: 'Profesional',
  specialty: '',
  timezone: 'America/Argentina/Buenos_Aires',
  actor: 'profesional',
});

const PROFESSIONAL_STORAGE_KEY = 'appointment-management.professional-context.v1';

function nonEmpty(value, fallback) {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback;
}

function positiveInteger(value, fallback) {
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : fallback;
}

const environment = import.meta.env;

function storedProfessional() {
  if (typeof window === 'undefined') return null;

  try {
    const stored = JSON.parse(window.localStorage.getItem(PROFESSIONAL_STORAGE_KEY));
    return positiveInteger(stored?.id, null) ? stored : null;
  } catch {
    return null;
  }
}

const testProfessional = storedProfessional();

export const professionalContext = Object.freeze({
  id: positiveInteger(testProfessional?.id, positiveInteger(environment.VITE_PROFESSIONAL_ID, DEFAULT_PROFESSIONAL.id)),
  name: nonEmpty(testProfessional?.name, nonEmpty(environment.VITE_PROFESSIONAL_NAME, DEFAULT_PROFESSIONAL.name)),
  specialty: nonEmpty(testProfessional?.specialty, nonEmpty(environment.VITE_PROFESSIONAL_SPECIALTY, DEFAULT_PROFESSIONAL.specialty)),
  timezone: nonEmpty(environment.VITE_PROFESSIONAL_TIMEZONE, DEFAULT_PROFESSIONAL.timezone),
  actor: nonEmpty(environment.VITE_PROFESSIONAL_ACTOR, DEFAULT_PROFESSIONAL.actor),
  testMode: Boolean(testProfessional),
});

export function saveTestProfessional(professional) {
  const id = positiveInteger(professional?.id, null);
  if (!id || typeof window === 'undefined') {
    throw new Error('El ID del profesional debe ser un entero positivo.');
  }

  const context = {
    id,
    name: nonEmpty(
      `${professional?.nombre || ''} ${professional?.apellido || ''}`.trim(),
      `Profesional #${id}`,
    ),
    specialty: nonEmpty(professional?.especialidad, ''),
  };
  window.localStorage.setItem(PROFESSIONAL_STORAGE_KEY, JSON.stringify(context));
  return context;
}

export function clearTestProfessional() {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(PROFESSIONAL_STORAGE_KEY);
  }
}

export { DEFAULT_PROFESSIONAL, PROFESSIONAL_STORAGE_KEY };
export default professionalContext;
