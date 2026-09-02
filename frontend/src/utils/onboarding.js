export const ONBOARDING_DISMISS_KEY = 'turnos-profesional:onboarding-dismissed';
export const ONBOARDING_CHANGE_EVENT = 'turnos-profesional:onboarding-change';

export function readOnboardingDismissed() {
  if (typeof window === 'undefined') return false;
  return window.localStorage.getItem(ONBOARDING_DISMISS_KEY) === 'true';
}

export function dismissOnboarding() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(ONBOARDING_DISMISS_KEY, 'true');
  window.dispatchEvent(new CustomEvent(ONBOARDING_CHANGE_EVENT));
}

export function reopenOnboarding() {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(ONBOARDING_DISMISS_KEY);
  window.dispatchEvent(new CustomEvent(ONBOARDING_CHANGE_EVENT));
}

