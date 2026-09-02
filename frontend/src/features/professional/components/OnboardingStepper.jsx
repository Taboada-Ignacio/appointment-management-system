import { Check, ChevronRight, Circle, LoaderCircle, X } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { professionalContext } from '../../../config/professional';
import { useToast } from '../../../components/ui/ToastProvider';
import { useCreateAnnualAgenda } from '../hooks/useAgenda';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  readOnboardingDismissed,
  dismissOnboarding,
  ONBOARDING_CHANGE_EVENT,
} from '../../../utils/onboarding';

const WEEK_DRAFT_KEY = 'turnos-profesional:weekly-template';

export function OnboardingStepper({ agendaState }) {
  const { success, error: showError } = useToast();
  const createAgenda = useCreateAnnualAgenda();
  const [dismissed, setDismissed] = useState(() => readOnboardingDismissed());
  const [hasWeekDraft, setHasWeekDraft] = useState(() => Boolean(window.localStorage.getItem(WEEK_DRAFT_KEY)));
  const agenda = agendaState?.agenda;
  const month = agendaState?.month;

  useEffect(() => {
    const sync = () => {
      setDismissed(readOnboardingDismissed());
      setHasWeekDraft(Boolean(window.localStorage.getItem(WEEK_DRAFT_KEY)));
    };
    window.addEventListener(ONBOARDING_CHANGE_EVENT, sync);
    window.addEventListener('storage', sync);
    return () => {
      window.removeEventListener(ONBOARDING_CHANGE_EVENT, sync);
      window.removeEventListener('storage', sync);
    };
  }, []);

  const configured = Boolean(month?.dias?.some((day) => Number(day.cantidadBrechas) > 0));
  const active = month?.estadoActual === 'ACTIVO';
  const completed = Boolean(agenda && configured && active);
  const shouldShow = !completed && (!agenda || !dismissed);

  const steps = useMemo(
    () => [
      {
        label: 'Configurar mi semana',
        detail: 'Definí los días y las brechas base.',
        done: hasWeekDraft || configured,
        to: '/profesional/mi-semana',
      },
      {
        label: 'Generar agenda anual',
        detail: `Creá los 12 meses de ${agendaState?.year ?? new Date().getFullYear()}.`,
        done: Boolean(agenda),
        action: 'generate',
      },
      {
        label: 'Configurar un mes',
        detail: 'Aplicá la semana o elegí fechas puntuales.',
        done: configured,
        to: '/profesional/mi-mes?configurar=true',
      },
      {
        label: 'Activar el mes',
        detail: 'Validá la disponibilidad antes de publicarla.',
        done: active,
        to: '/profesional/mi-mes',
      },
    ],
    [active, agenda, agendaState?.year, configured, hasWeekDraft]
  );

  if (!shouldShow || agendaState?.agendaQuery?.isLoading) return null;

  const currentIndex = Math.max(0, steps.findIndex((step) => !step.done));

  async function handleGenerate() {
    try {
      await createAgenda.mutateAsync(agendaState.year);
      success('Agenda anual generada', `Ya están disponibles los 12 meses de ${agendaState.year}.`);
    } catch (err) {
      showError('No se pudo generar la agenda', err.message);
    }
  }

  function dismiss() {
    dismissOnboarding();
    setDismissed(true);
  }

  return (
    <Card className="mb-7 overflow-hidden border-ring/25 bg-accent/25 shadow-none" aria-labelledby="onboarding-title">
      <div className="flex items-start justify-between gap-4 border-b border-ring/15 px-4 py-4 sm:px-5">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-info">Primera configuración</p>
          <h2 id="onboarding-title" className="mt-1 font-heading text-lg font-semibold tracking-tight">
            Prepará tu agenda para recibir turnos
          </h2>
          <p className="mt-1 max-w-2xl text-xs leading-5 text-muted-foreground">
            El backend conserva la fuente de verdad. Este recorrido te lleva por cada operación disponible.
          </p>
        </div>
        {agenda && (
          <Button type="button" onClick={dismiss} variant="ghost" className="-mr-2" aria-label="Continuar más tarde">
            <X className="h-4 w-4" />
            <span className="hidden sm:inline">Continuar más tarde</span>
          </Button>
        )}
      </div>

      <ol className="grid gap-px bg-border md:grid-cols-2 xl:grid-cols-4">
        {steps.map((step, index) => {
          const isCurrent = index === currentIndex;
          const Icon = step.done ? Check : isCurrent ? ChevronRight : Circle;
          const content = (
            <>
              <span
                className={cn(
                  'mt-0.5 grid h-7 w-7 shrink-0 place-items-center rounded-full border text-xs font-extrabold',
                  step.done && 'border-success bg-success text-success-foreground',
                  isCurrent && !step.done && 'border-ring bg-ring text-white',
                  !step.done && !isCurrent && 'border-border text-muted-foreground'
                )}
              >
                <Icon className="h-3.5 w-3.5" aria-hidden="true" />
              </span>
              <span className="min-w-0">
                <span className="block text-[13px] font-semibold text-foreground">{step.label}</span>
                <span className="mt-1 block text-[11px] leading-4 text-muted-foreground">{step.detail}</span>
              </span>
            </>
          );

          if (step.action === 'generate' && !step.done) {
            return (
              <li key={step.label} className="bg-card/80">
                <Button
                  type="button"
                  onClick={handleGenerate}
                  disabled={createAgenda.isPending || !hasWeekDraft}
                  variant="ghost"
                  className="h-full min-h-28 w-full justify-start whitespace-normal rounded-none p-4 text-left hover:bg-card"
                  title={!hasWeekDraft ? 'Primero guardá una plantilla semanal' : undefined}
                >
                  {createAgenda.isPending ? <LoaderCircle className="mt-1 size-5 animate-spin text-info" /> : content}
                </Button>
              </li>
            );
          }

          return (
            <li key={step.label} className="bg-card/80">
              {step.to && (!step.done || isCurrent) ? (
                <Link to={step.to} className="flex min-h-28 items-start gap-3 p-4 transition hover:bg-card">
                  {content}
                </Link>
              ) : (
                <div className="flex min-h-28 items-start gap-3 p-4">{content}</div>
              )}
            </li>
          );
        })}
      </ol>

      {!professionalContext.id && (
        <p className="border-t border-warning/20 px-5 py-3 text-xs font-semibold text-warning">
          Configurá VITE_PROFESIONAL_ID antes de operar la agenda.
        </p>
      )}
    </Card>
  );
}
