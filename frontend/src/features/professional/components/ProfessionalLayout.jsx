import { LoaderCircle, Menu, ShieldCheck } from 'lucide-react';
import { useMemo, useRef, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { professionalContext } from '@/config/professional';
import { useAgendaMonth, useAnnualAgendas } from '../hooks/useAgenda';
import { useProfessionalConfig } from '../hooks/useProfessionalConfig';
import { useAffectedAppointments } from '../hooks/useAbsences';
import { ProfessionalSidebar } from './ProfessionalSidebar';
import { OnboardingStepper } from './OnboardingStepper';
import { OnboardingWizard } from './OnboardingWizard';
import { ProfessionalTestSwitcher } from './ProfessionalTestSwitcher';

function zonedYearMonth() {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: professionalContext.timezone,
    year: 'numeric',
    month: 'numeric',
  }).formatToParts(new Date());
  return {
    year: Number(parts.find((part) => part.type === 'year')?.value),
    month: Number(parts.find((part) => part.type === 'month')?.value),
  };
}

export function ProfessionalLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const menuButtonRef = useRef(null);
  const queryClient = useQueryClient();

  const current = useMemo(() => zonedYearMonth(), []);
  const agendaState = useAgendaMonth(current.year, current.month);
  const { data: config, isLoading: isLoadingConfig } = useProfessionalConfig(professionalContext.id);
  const { data: agendas, isLoading: isLoadingAgendas } = useAnnualAgendas();
  const affectedQuery = useAffectedAppointments();
  const pendingAffectedCount = affectedQuery.data?.filter((item) => item.resolucion === 'PENDIENTE').length;

  const hasConfig = Boolean(config);
  const hasCurrentYearAgenda = Array.isArray(agendas) && agendas.some((agenda) => agenda.anio === current.year);

  if (isLoadingConfig || isLoadingAgendas) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background" aria-busy="true">
        <div className="flex flex-col items-center gap-3">
          <LoaderCircle className="size-8 animate-spin text-primary" />
          <p className="text-sm font-medium text-muted-foreground">Verificando estado del consultorio...</p>
        </div>
      </div>
    );
  }

  if (!hasConfig) {
    return (
      <OnboardingWizard
        initialStep={1}
        onComplete={() => {
          queryClient.invalidateQueries({ queryKey: ['professional', professionalContext.id] });
        }}
      />
    );
  }

  if (!hasCurrentYearAgenda) {
    return (
      <OnboardingWizard
        initialStep={2}
        savedConfig={config}
        onComplete={() => {
          queryClient.invalidateQueries({ queryKey: ['professional', professionalContext.id] });
        }}
      />
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <a
        href="#contenido-principal"
        className="fixed left-3 top-3 z-[70] -translate-y-24 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-lg focus:translate-y-0"
      >
        Saltar al contenido
      </a>

      <ProfessionalSidebar
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        hasAnnualAgenda={Boolean(agendaState.agenda)}
        returnFocusRef={menuButtonRef}
        pendingAffectedCount={pendingAffectedCount}
      />

      <div className="lg:pl-72">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b bg-background/92 px-4 backdrop-blur-md sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <Button
              ref={menuButtonRef}
              type="button"
              variant="outline"
              size="icon"
              className="lg:hidden"
              onClick={() => setDrawerOpen(true)}
              aria-label="Abrir menú"
              aria-expanded={drawerOpen}
              aria-controls="navegacion-profesional-movil"
            >
              <Menu className="h-5 w-5" />
            </Button>
            <Separator orientation="vertical" className="hidden h-6 lg:block" />
            <div>
              <p className="font-heading text-sm font-semibold tracking-tight sm:text-base">Centro de agenda</p>
              <p className="hidden text-[10px] font-bold uppercase tracking-[0.11em] text-muted-foreground sm:block">Hora oficial · {professionalContext.timezone}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <ProfessionalTestSwitcher />
            <div className="hidden items-center gap-2 rounded-full border bg-card px-3 py-1.5 text-[11px] font-semibold text-muted-foreground shadow-xs md:flex">
              <span className="relative flex size-2" aria-hidden="true">
                <span className="absolute inline-flex size-full animate-ping rounded-full bg-success opacity-30" />
                <span className="relative inline-flex size-2 rounded-full bg-success" />
              </span>
              <ShieldCheck className="size-3.5 text-info" aria-hidden="true" />
              <span>Datos del backend</span>
            </div>
          </div>
        </header>

        <main id="contenido-principal" className="mx-auto w-full max-w-[94rem] px-4 pb-12 pt-5 sm:px-6 lg:px-8 lg:pt-7">
          <OnboardingStepper agendaState={agendaState} />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
