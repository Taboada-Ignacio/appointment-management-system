import { useState } from 'react';
import { AlertCircle, Save, Sparkles } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';
import { BACKEND_DAYS, DAY_NAMES } from '@/utils/dates';
import { validateGaps } from '@/utils/gaps';
import { GapEditor } from './GapEditor';

export const WEEKLY_TEMPLATE_KEY = 'turnos-profesional:weekly-template';

const DEFAULT_SLOTS = [
  { horaInicio: '09:00', horaFin: '13:00' },
  { horaInicio: '14:00', horaFin: '18:00' },
];

export function WeeklyEditor({ initialDraft = null, onSave = null, onApply = null, canApply = false, applyTargetLabel = '' }) {
  const [draft, setDraft] = useState(() => {
    if (initialDraft?.diasSemana) return initialDraft;
    try {
      const saved = localStorage.getItem(WEEKLY_TEMPLATE_KEY);
      if (saved) return JSON.parse(saved);
    } catch {
      // Local storage is optional; fall back to the initial template.
    }
    return {
      diasSemana: [
        { diaSemana: 'MONDAY', brechas: [...DEFAULT_SLOTS] },
        { diaSemana: 'TUESDAY', brechas: [...DEFAULT_SLOTS] },
        { diaSemana: 'WEDNESDAY', brechas: [...DEFAULT_SLOTS] },
        { diaSemana: 'THURSDAY', brechas: [...DEFAULT_SLOTS] },
        { diaSemana: 'FRIDAY', brechas: [{ horaInicio: '09:00', horaFin: '14:00' }] },
      ],
    };
  });

  const isDirty = (() => {
    try {
      const saved = localStorage.getItem(WEEKLY_TEMPLATE_KEY);
      return Boolean(saved && JSON.stringify(draft) !== saved);
    } catch {
      return false;
    }
  })();

  const handleDayToggle = (dayCode) => {
    setDraft((previous) => {
      const exists = previous.diasSemana.some((day) => day.diaSemana === dayCode);
      if (exists) {
        return { ...previous, diasSemana: previous.diasSemana.filter((day) => day.diaSemana !== dayCode) };
      }
      return {
        ...previous,
        diasSemana: [
          ...previous.diasSemana,
          { diaSemana: dayCode, brechas: [{ horaInicio: '09:00', horaFin: '13:00' }] },
        ],
      };
    });
  };

  const handleGapsChange = (dayCode, newGaps) => {
    setDraft((previous) => ({
      ...previous,
      diasSemana: previous.diasSemana.map((day) =>
        day.diaSemana === dayCode ? { ...day, brechas: newGaps } : day
      ),
    }));
  };

  const handleSaveDraft = () => {
    localStorage.setItem(WEEKLY_TEMPLATE_KEY, JSON.stringify(draft));
    onSave?.(draft);
  };

  const isDraftValid =
    draft.diasSemana.length > 0 &&
    draft.diasSemana.every((day) => day.brechas.length > 0 && validateGaps(day.brechas).valid);

  return (
    <div className="flex flex-col gap-6">
      <Card className="shadow-none">
        <CardContent className="flex flex-col items-start justify-between gap-4 p-5 sm:flex-row sm:items-center">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="font-heading text-lg font-semibold tracking-tight">Plantilla Semanal de Atención</h2>
              {isDirty && (
                <Badge variant="outline" className="border-warning/20 bg-warning/10 text-warning">
                  Borrador no guardado
                </Badge>
              )}
            </div>
            <p className="mt-1 text-xs leading-5 text-muted-foreground">
              Se almacena localmente como borrador y solo se aplica a meses verificados como vacíos.
            </p>
          </div>

          <div className="flex w-full items-center gap-2 sm:w-auto">
            <Button type="button" variant="outline" onClick={handleSaveDraft} disabled={!isDraftValid} className="flex-1 sm:flex-none">
              <Save data-icon="inline-start" />
              Guardar borrador
            </Button>
            {onApply && (
              <Button
                type="button"
                onClick={() => onApply(draft)}
                disabled={!canApply || !isDraftValid}
                className="flex-1 sm:flex-none"
                title={!canApply ? 'Seleccioná un mes vacío y sin configuración para aplicar' : undefined}
              >
                <Sparkles data-icon="inline-start" />
                Aplicar a {applyTargetLabel || 'mes'}
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {!isDraftValid && (
        <Alert className="border-warning/25 bg-warning/8 text-warning">
          <AlertCircle aria-hidden="true" />
          <AlertDescription className="text-warning">
            Habilitá al menos un día con una franja horaria válida antes de guardar o aplicar la plantilla.
          </AlertDescription>
        </Alert>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
        {BACKEND_DAYS.map((dayCode, index) => {
          const dayConfig = draft.diasSemana.find((day) => day.diaSemana === dayCode);
          const isEnabled = Boolean(dayConfig);
          const dayName = DAY_NAMES[index] || dayCode;

          return (
            <Card
              key={dayCode}
              className={cn(
                'shadow-none transition-colors',
                isEnabled ? 'border-ring/45 bg-card' : 'bg-muted/35 text-muted-foreground'
              )}
            >
              <CardContent className="flex flex-col gap-4 p-4">
                <div className="flex items-center justify-between border-b pb-3">
                  <div>
                    <p className="font-heading text-sm font-semibold">{dayName}</p>
                    <p className="mt-0.5 text-[11px] text-muted-foreground">
                      {isEnabled ? `${dayConfig.brechas.length} ${dayConfig.brechas.length === 1 ? 'franja' : 'franjas'}` : 'No laborable'}
                    </p>
                  </div>
                  <Switch
                    checked={isEnabled}
                    onCheckedChange={() => handleDayToggle(dayCode)}
                    aria-label={`Habilitar atención los días ${dayName}`}
                  />
                </div>

                {isEnabled ? (
                  <GapEditor gaps={dayConfig.brechas || []} onChange={(newGaps) => handleGapsChange(dayCode, newGaps)} />
                ) : (
                  <p className="py-3 text-xs italic text-muted-foreground">Día no laborable / cerrado</p>
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
