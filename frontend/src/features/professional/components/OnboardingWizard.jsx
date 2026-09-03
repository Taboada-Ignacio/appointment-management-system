import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Calendar,
  CalendarCheck,
  Check,
  CheckCircle2,
  Clock,
  Copy,
  Info,
  Layers,
  Lock,
  RotateCcw,
  ShieldAlert,
  Sparkles,
  Users,
  ArrowRight,
  ArrowLeft,
  Loader2,
  CalendarDays,
  CheckSquare,
} from 'lucide-react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { useToast } from '@/components/ui/ToastProvider';
import { professionalContext } from '@/config/professional';
import { getCurrentYearMonth, BACKEND_DAYS, DAY_NAMES, MONTH_NAMES } from '@/utils/dates';
import { validateGaps } from '@/utils/gaps';
import { WEEKLY_TEMPLATE_KEY } from './WeeklyEditor';
import { GapEditor } from './GapEditor';
import { useRegisterProfessionalConfig, useUpdateProfessionalConfig } from '../hooks/useProfessionalConfig';
import { initializeCalendar } from '../api/agendaApi';
import { ProfessionalTestSwitcher } from './ProfessionalTestSwitcher';
import { cn } from '@/lib/utils';

const DURATION_PRESETS = [
  { value: '15', label: '15 minutos' },
  { value: '20', label: '20 minutos' },
  { value: '30', label: '30 minutos (Recomendado)' },
  { value: '45', label: '45 minutos' },
  { value: '60', label: '60 minutos (1 hora)' },
  { value: '90', label: '90 minutos (1 h 30 min)' },
  { value: 'otro', label: 'Otro (personalizado)...' },
];

const CAPACITY_PRESETS = [
  { value: '1', label: '1 turno a la vez (Atención individual / Estándar)' },
  { value: '2', label: '2 turnos en simultáneo' },
  { value: '3', label: '3 turnos en simultáneo' },
  { value: '4', label: '4 turnos en simultáneo' },
  { value: 'otro', label: 'Otro (personalizado)...' },
];

const THRESHOLD_PRESETS = [
  { value: '0', label: 'Sin anticipación mínima (0 horas)' },
  { value: '12', label: '12 horas antes' },
  { value: '24', label: '24 horas antes (1 día previo - Recomendado)' },
  { value: '48', label: '48 horas antes (2 días previos)' },
  { value: 'otro', label: 'Otro (personalizado)...' },
];

const DEFAULT_SLOTS = [
  { horaInicio: '09:00', horaFin: '13:00' },
  { horaInicio: '14:00', horaFin: '18:00' },
];

export function OnboardingWizard({ initialStep = 1, savedConfig = null, onComplete }) {
  const navigate = useNavigate();
  const { success, error: showError } = useToast();
  const registerConfig = useRegisterProfessionalConfig();
  const updateConfig = useUpdateProfessionalConfig();

  const { year: currentYear, month: currentMonth } = getCurrentYearMonth(professionalContext.timezone);
  const nextMonthNumber = currentMonth === 12 ? 1 : currentMonth + 1;
  const nextMonthYear = currentMonth === 12 ? currentYear + 1 : currentYear;

  const [step, setStep] = useState(initialStep);
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);

  // Step 1: Professional Parameters Form state
  const [durationSelect, setDurationSelect] = useState(() =>
    savedConfig ? String(savedConfig.duracionAproximadaPorTurno) : '30'
  );
  const [customDuration, setCustomDuration] = useState(() =>
    savedConfig ? String(savedConfig.duracionAproximadaPorTurno) : '30'
  );

  const [capacitySelect, setCapacitySelect] = useState(() =>
    savedConfig ? String(savedConfig.cantidadMaxTurnosALaVez) : '1'
  );
  const [customCapacity, setCustomCapacity] = useState(() =>
    savedConfig ? String(savedConfig.cantidadMaxTurnosALaVez) : '1'
  );

  const [thresholdSelect, setThresholdSelect] = useState(() =>
    savedConfig ? String(savedConfig.umbralCancelacionHoras) : '24'
  );
  const [customThreshold, setCustomThreshold] = useState(() =>
    savedConfig ? String(savedConfig.umbralCancelacionHoras) : '24'
  );

  const [agendaSoloManejada, setAgendaSoloManejada] = useState(
    () => savedConfig?.agendaSoloManejadaPorProfesional || false
  );
  const [formErrors, setFormErrors] = useState({});

  // Step 2: Weekly Schedule & Gaps state
  const [repeatMonthConfig, setRepeatMonthConfig] = useState(true);
  const [weeklyDraft, setWeeklyDraft] = useState(() => {
    try {
      const saved = localStorage.getItem(WEEKLY_TEMPLATE_KEY);
      if (saved) return JSON.parse(saved);
    } catch {
      // ignore
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

  // Step 3: Massive Generation Progress state
  const [isGenerating, setIsGenerating] = useState(false);
  const [generationProgress, setGenerationProgress] = useState(0);
  const [generationStatusText, setGenerationStatusText] = useState('');
  const [checklist, setChecklist] = useState([
    { id: 'config', label: 'Parámetros del consultorio registrados', done: Boolean(savedConfig) },
    { id: 'template', label: 'Plantilla de horarios semanales guardada', done: false },
    { id: 'annual', label: `Agenda anual ${currentYear} creada`, done: false },
    {
      id: 'activeMonths',
      label: `Mes actual (${MONTH_NAMES[currentMonth - 1]}) y siguiente (${MONTH_NAMES[nextMonthNumber - 1]}) activados (días en estado ACTIVO)`,
      done: false,
    },
    {
      id: 'inactiveMonths',
      label: 'Demás meses del año creados en estado INACTIVO (y sus días inactivos)',
      done: false,
    },
    { id: 'daysAndGaps', label: 'Franjas horarias y disponibilidad asignadas', done: false },
  ]);

  // Saved summary
  const [savedSummary, setSavedSummary] = useState(savedConfig);
  const [calendarSummary, setCalendarSummary] = useState(null);

  // Resolve numeric values
  const finalDuration = durationSelect === 'otro' ? Number(customDuration) : Number(durationSelect);
  const finalCapacity = capacitySelect === 'otro' ? Number(customCapacity) : Number(capacitySelect);
  const finalThreshold = thresholdSelect === 'otro' ? Number(customThreshold) : Number(thresholdSelect);

  const validateStep1 = () => {
    const errors = {};
    if (!Number.isSafeInteger(finalDuration) || finalDuration <= 0) {
      errors.duration = 'La duración debe ser un número entero mayor a 0 minutos.';
    }
    if (!Number.isSafeInteger(finalCapacity) || finalCapacity <= 0) {
      errors.capacity = 'La cantidad de turnos debe ser mayor o igual a 1.';
    }
    if (!Number.isSafeInteger(finalThreshold) || finalThreshold < 0) {
      errors.threshold = 'El umbral de cancelación no puede ser negativo.';
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleStep1Submit = async (e) => {
    e?.preventDefault();
    if (!validateStep1()) return;

    try {
      const payload = {
        cantidadMaxTurnosALaVez: finalCapacity,
        duracionAproximadaPorTurno: finalDuration,
        agendaSoloManejadaPorProfesional: agendaSoloManejada,
        umbralCancelacionHoras: finalThreshold,
      };

      const result = savedSummary
        ? await updateConfig.mutateAsync(payload)
        : await registerConfig.mutateAsync(payload);
      setSavedSummary(result || payload);
      setChecklist((prev) => prev.map((item) => (item.id === 'config' ? { ...item, done: true } : item)));
      success(
        savedSummary ? 'Cambios aplicados' : 'Configuración guardada',
        savedSummary
          ? 'Los parámetros del profesional se actualizaron correctamente.'
          : 'Parámetros del profesional registrados con éxito.',
      );
      setStep(2);
    } catch (err) {
      showError('Error al guardar configuración', err.message || 'No se pudo registrar la configuración.');
    }
  };

  // Step 2 handlers: Toggle Day, Edit Gaps, Copy Schedule to other days
  const handleDayToggle = (dayCode) => {
    setWeeklyDraft((prev) => {
      const exists = prev.diasSemana.some((d) => d.diaSemana === dayCode);
      if (exists) {
        return { ...prev, diasSemana: prev.diasSemana.filter((d) => d.diaSemana !== dayCode) };
      }
      return {
        ...prev,
        diasSemana: [
          ...prev.diasSemana,
          { diaSemana: dayCode, brechas: [{ horaInicio: '09:00', horaFin: '13:00' }] },
        ],
      };
    });
  };

  const handleGapsChange = (dayCode, newGaps) => {
    setWeeklyDraft((prev) => ({
      ...prev,
      diasSemana: prev.diasSemana.map((day) =>
        day.diaSemana === dayCode ? { ...day, brechas: newGaps } : day
      ),
    }));
  };

  const handleCopyGapsToAllActiveDays = (sourceDayCode) => {
    const sourceDay = weeklyDraft.diasSemana.find((d) => d.diaSemana === sourceDayCode);
    if (!sourceDay) return;

    setWeeklyDraft((prev) => ({
      ...prev,
      diasSemana: prev.diasSemana.map((day) => ({
        ...day,
        brechas: JSON.parse(JSON.stringify(sourceDay.brechas)),
      })),
    }));
    success('Horarios duplicados', `Se copiaron las franjas de ${sourceDayCode} a todos los días activos.`);
  };

  const isWeeklyDraftValid =
    weeklyDraft.diasSemana.length > 0 &&
    weeklyDraft.diasSemana.every((d) => d.brechas?.length > 0 && validateGaps(d.brechas).valid);

  const handlePromptConfirmation = () => {
    if (!isWeeklyDraftValid) {
      showError('Horarios incompletos', 'Configurá al menos un día laborable con franjas horarias válidas.');
      return;
    }
    setConfirmDialogOpen(true);
  };

  // Step 2 -> Step 3: Massive Generation Execution upon Confirmation
  const handleConfirmAndExecuteGeneration = async () => {
    setConfirmDialogOpen(false);
    setStep(3);
    setIsGenerating(true);
    setGenerationProgress(10);
    setGenerationStatusText('Guardando plantilla de horarios en navegador...');

    try {
      // 1. Save weekly draft in localStorage
      localStorage.setItem(WEEKLY_TEMPLATE_KEY, JSON.stringify(weeklyDraft));
      setChecklist((prev) => prev.map((item) => (item.id === 'template' ? { ...item, done: true } : item)));
      setGenerationProgress(25);

      // 2. Initialize the complete calendar atomically in the backend
      setGenerationStatusText(`Creando y verificando la configuración inicial del calendario...`);
      const result = await initializeCalendar(weeklyDraft.diasSemana, repeatMonthConfig);
      if (!result?.completado) {
        throw new Error('El backend no confirmó la inicialización completa del calendario.');
      }
      setCalendarSummary(result);
      setChecklist((prev) => prev.map((item) => (item.id === 'annual' ? { ...item, done: true } : item)));
      setGenerationProgress(65);

      setChecklist((prev) =>
        prev.map((item) =>
          item.id === 'activeMonths' || item.id === 'inactiveMonths'
            ? { ...item, done: true }
            : item
        )
      );
      setGenerationStatusText('Validando meses, días laborables y franjas horarias...');
      setChecklist((prev) => prev.map((item) => (item.id === 'daysAndGaps' ? { ...item, done: true } : item)));
      setGenerationProgress(100);
      setGenerationStatusText('¡Generación y activación completada con éxito!');
      success('Agenda lista', `Se crearon tus horarios y los meses ${MONTH_NAMES[currentMonth - 1]} y ${MONTH_NAMES[nextMonthNumber - 1]} están activos.`);
    } catch (err) {
      showError('Error en generación de agenda', err.message || 'Ocurrió un problema durante la generación.');
      setStep(2);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleFinishAndStartWorking = () => {
    onComplete?.();
    navigate('/profesional/mi-dia');
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col overflow-y-auto bg-background px-4 py-8 sm:px-6 lg:px-8">
      {/* Header bar with Test Switcher */}
      <div className="mx-auto flex max-w-5xl items-center justify-between pb-6">
        <div className="flex items-center gap-3">
          <div className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm">
            <Sparkles className="size-5" />
          </div>
          <div>
            <h1 className="font-heading text-lg font-bold tracking-tight sm:text-xl">
              Puesta en marcha del consultorio
            </h1>
            <p className="text-xs text-muted-foreground">
              {savedConfig && initialStep === 2
                ? 'Tus parámetros ya están guardados. Continuá con los horarios para activar tu agenda.'
                : 'Completá estos pasos para configurar tu consultorio y activar tu agenda de turnos.'}
            </p>
          </div>
        </div>
        <ProfessionalTestSwitcher />
      </div>

      {/* Stepper Progress Indicator */}
      <div className="mx-auto mb-8 max-w-5xl">
        <div className="grid grid-cols-3 gap-2 sm:gap-4">
          <StepBadge stepNumber={1} title="1. Parámetros" active={step === 1} completed={step > 1 || Boolean(savedConfig)} />
          <StepBadge stepNumber={2} title="2. Días y Horarios" active={step === 2} completed={step > 2} />
          <StepBadge stepNumber={3} title="3. Generación y Éxito" active={step === 3} completed={step === 3 && !isGenerating} />
        </div>
      </div>

      {/* Wizard Content Card */}
      <div className="mx-auto max-w-5xl w-full">
        {/* ========================================================================= */}
        {/* PASO 1: Parámetros del Profesional */}
        {/* ========================================================================= */}
        {step === 1 && (
          <Card className="shadow-md">
            <CardHeader className="border-b bg-card/60">
              <div className="flex items-center gap-2">
                <Badge variant="secondary" className="gap-1.5 font-mono text-xs">
                  <Clock className="size-3.5 text-info" />
                  Paso 1 de 2
                </Badge>
              </div>
              <CardTitle className="text-xl font-bold">Parámetros de Atención y Turnos</CardTitle>
              <CardDescription>
                Definí cómo funciona la atención para el profesional #{professionalContext.id} (
                {professionalContext.name}).
              </CardDescription>
            </CardHeader>

            <form onSubmit={handleStep1Submit}>
              <CardContent className="space-y-8 p-6 sm:p-8">
                {/* Field 1: Duración aproximada por turno */}
                <div className="grid gap-4 rounded-xl border bg-card/40 p-4 transition-colors sm:p-5 lg:grid-cols-12 lg:gap-6">
                  <div className="space-y-3 lg:col-span-7">
                    <Label htmlFor="duracion-select" className="text-sm font-semibold text-foreground">
                      Duración estimada por turno
                    </Label>
                    <Select value={durationSelect} onValueChange={(val) => setDurationSelect(val)}>
                      <SelectTrigger id="duracion-select" className="w-full">
                        <SelectValue placeholder="Seleccioná una duración" />
                      </SelectTrigger>
                      <SelectContent>
                        {DURATION_PRESETS.map((preset) => (
                          <SelectItem key={preset.value} value={preset.value}>
                            {preset.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>

                    {durationSelect === 'otro' && (
                      <div className="mt-2 space-y-1.5 animate-in fade-in-50">
                        <Label htmlFor="duracion-custom" className="text-xs text-muted-foreground">
                          Ingresá los minutos de duración:
                        </Label>
                        <div className="flex items-center gap-2">
                          <Input
                            id="duracion-custom"
                            type="number"
                            min="1"
                            max="720"
                            value={customDuration}
                            onChange={(e) => setCustomDuration(e.target.value)}
                            placeholder="Ej: 40"
                            className="max-w-36"
                          />
                          <span className="text-xs font-medium text-muted-foreground">minutos</span>
                        </div>
                      </div>
                    )}

                    {formErrors.duration && (
                      <p className="text-xs font-medium text-destructive">{formErrors.duration}</p>
                    )}
                  </div>

                  <div className="flex flex-col justify-center rounded-lg border border-dashed bg-muted/40 p-3.5 text-xs lg:col-span-5">
                    <div className="flex items-center gap-1.5 font-semibold text-info">
                      <Info className="size-3.5 shrink-0" />
                      <span>¿Para qué sirve?</span>
                    </div>
                    <p className="mt-1 leading-relaxed text-muted-foreground">
                      Define el bloque de tiempo estándar asignado a cada cita. Se usa para calcular automáticamente
                      los intervalos disponibles y la separación entre turnos.
                    </p>
                  </div>
                </div>

                {/* Field 2: Cantidad máxima de turnos a la vez */}
                <div className="grid gap-4 rounded-xl border bg-card/40 p-4 transition-colors sm:p-5 lg:grid-cols-12 lg:gap-6">
                  <div className="space-y-3 lg:col-span-7">
                    <Label htmlFor="capacity-select" className="text-sm font-semibold text-foreground">
                      Capacidad simultánea de atención
                    </Label>
                    <Select value={capacitySelect} onValueChange={(val) => setCapacitySelect(val)}>
                      <SelectTrigger id="capacity-select" className="w-full">
                        <SelectValue placeholder="Seleccioná la cantidad de turnos" />
                      </SelectTrigger>
                      <SelectContent>
                        {CAPACITY_PRESETS.map((preset) => (
                          <SelectItem key={preset.value} value={preset.value}>
                            {preset.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>

                    {capacitySelect === 'otro' && (
                      <div className="mt-2 space-y-1.5 animate-in fade-in-50">
                        <Label htmlFor="capacity-custom" className="text-xs text-muted-foreground">
                          Ingresá la cantidad máxima en paralelo:
                        </Label>
                        <div className="flex items-center gap-2">
                          <Input
                            id="capacity-custom"
                            type="number"
                            min="1"
                            max="50"
                            value={customCapacity}
                            onChange={(e) => setCustomCapacity(e.target.value)}
                            placeholder="Ej: 5"
                            className="max-w-36"
                          />
                          <span className="text-xs font-medium text-muted-foreground">turnos a la vez</span>
                        </div>
                      </div>
                    )}

                    {formErrors.capacity && (
                      <p className="text-xs font-medium text-destructive">{formErrors.capacity}</p>
                    )}
                  </div>

                  <div className="flex flex-col justify-center rounded-lg border border-dashed bg-muted/40 p-3.5 text-xs lg:col-span-5">
                    <div className="flex items-center gap-1.5 font-semibold text-info">
                      <Users className="size-3.5 shrink-0" />
                      <span>¿Para qué sirve?</span>
                    </div>
                    <p className="mt-1 leading-relaxed text-muted-foreground">
                      Indica cuántos pacientes o clientes podés recibir al mismo tiempo en una misma franja horaria. Si
                      tu atención es personalizada uno a uno, seleccioná 1.
                    </p>
                  </div>
                </div>

                {/* Field 3: Umbral de cancelación anticipada */}
                <div className="grid gap-4 rounded-xl border bg-card/40 p-4 transition-colors sm:p-5 lg:grid-cols-12 lg:gap-6">
                  <div className="space-y-3 lg:col-span-7">
                    <Label htmlFor="threshold-select" className="text-sm font-semibold text-foreground">
                      Tiempo mínimo de cancelación anticipada
                    </Label>
                    <Select value={thresholdSelect} onValueChange={(val) => setThresholdSelect(val)}>
                      <SelectTrigger id="threshold-select" className="w-full">
                        <SelectValue placeholder="Seleccioná el umbral de cancelación" />
                      </SelectTrigger>
                      <SelectContent>
                        {THRESHOLD_PRESETS.map((preset) => (
                          <SelectItem key={preset.value} value={preset.value}>
                            {preset.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>

                    {thresholdSelect === 'otro' && (
                      <div className="mt-2 space-y-1.5 animate-in fade-in-50">
                        <Label htmlFor="threshold-custom" className="text-xs text-muted-foreground">
                          Ingresá las horas mínimas de anticipación:
                        </Label>
                        <div className="flex items-center gap-2">
                          <Input
                            id="threshold-custom"
                            type="number"
                            min="0"
                            max="720"
                            value={customThreshold}
                            onChange={(e) => setCustomThreshold(e.target.value)}
                            placeholder="Ej: 72"
                            className="max-w-36"
                          />
                          <span className="text-xs font-medium text-muted-foreground">horas</span>
                        </div>
                      </div>
                    )}

                    {formErrors.threshold && (
                      <p className="text-xs font-medium text-destructive">{formErrors.threshold}</p>
                    )}
                  </div>

                  <div className="flex flex-col justify-center rounded-lg border border-dashed bg-muted/40 p-3.5 text-xs lg:col-span-5">
                    <div className="flex items-center gap-1.5 font-semibold text-info">
                      <ShieldAlert className="size-3.5 shrink-0" />
                      <span>¿Para qué sirve?</span>
                    </div>
                    <p className="mt-1 leading-relaxed text-muted-foreground">
                      Establece el plazo mínimo previo al inicio del turno para permitir su cancelación o baja sin
                      penalidad o sin considerarse cancelación de último momento.
                    </p>
                  </div>
                </div>

                {/* Field 4: Agenda exclusiva */}
                <div className="grid gap-4 rounded-xl border bg-card/40 p-4 transition-colors sm:p-5 lg:grid-cols-12 lg:gap-6">
                  <div className="flex flex-col justify-between gap-3 lg:col-span-7">
                    <div>
                      <Label htmlFor="agenda-exclusiva" className="text-sm font-semibold text-foreground">
                        Modalidad de gestión de turnos
                      </Label>
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        {agendaSoloManejada
                          ? 'Agenda privada: Solo vos o tu consultorio pueden asignar turnos.'
                          : 'Agenda compartida: Permite autogestión de turnos por parte de los clientes.'}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <Switch
                        id="agenda-exclusiva"
                        checked={agendaSoloManejada}
                        onCheckedChange={setAgendaSoloManejada}
                        aria-label="Agenda exclusiva para gestión interna"
                      />
                      <span className="text-xs font-medium">
                        {agendaSoloManejada ? 'Solo administrada por profesional' : 'Permite autogestión de clientes'}
                      </span>
                    </div>
                  </div>

                  <div className="flex flex-col justify-center rounded-lg border border-dashed bg-muted/40 p-3.5 text-xs lg:col-span-5">
                    <div className="flex items-center gap-1.5 font-semibold text-info">
                      <Lock className="size-3.5 shrink-0" />
                      <span>¿Cómo funciona?</span>
                    </div>
                    <p className="mt-1 leading-relaxed text-muted-foreground">
                      Si está activado, los pacientes no podrán solicitar turnos por el portal público de autogestión;
                      todos los turnos deberán ser cargados manualmente por vos o tu equipo.
                    </p>
                  </div>
                </div>
              </CardContent>

              <CardFooter className="flex items-center justify-between border-t bg-muted/10 px-6 py-4">
                <p className="text-xs text-muted-foreground">
                  Paso 1 de 2 · Los datos se guardan directamente en el backend.
                </p>
                <Button type="submit" size="default" disabled={registerConfig.isPending || updateConfig.isPending} className="gap-2">
                  {registerConfig.isPending || updateConfig.isPending ? (
                    <>
                      <Loader2 className="size-4 animate-spin" /> Guardando...
                    </>
                  ) : (
                    <>
                      {savedSummary ? 'Aplicar cambios y continuar' : 'Guardar y continuar a horarios'} <ArrowRight className="size-4" />
                    </>
                  )}
                </Button>
              </CardFooter>
            </form>
          </Card>
        )}

        {/* ========================================================================= */}
        {/* PASO 2: Días y Brechas Horarias de la Semana */}
        {/* ========================================================================= */}
        {step === 2 && (
          <Card className="shadow-md">
            <CardHeader className="border-b bg-card/60">
              <div className="flex items-center gap-2">
                <Badge variant="secondary" className="gap-1.5 font-mono text-xs">
                  <CalendarDays className="size-3.5 text-info" />
                  Paso 2 de 2
                </Badge>
              </div>
              <CardTitle className="text-xl font-bold">Días de Atención y Franjas Horarias</CardTitle>
              <CardDescription>
                Definí tu semana habitual de atención. Podés encender los días que atendés, ajustar sus horas de inicio y fin,
                o duplicar tus horarios fácilmente.
              </CardDescription>
            </CardHeader>

            <CardContent className="space-y-6 p-6 sm:p-8">
              {/* Opción para preparar también el mes siguiente */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-xl border bg-card/60 p-4 sm:p-5 transition-colors">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <RotateCcw className="size-4 text-primary" />
                    <Label htmlFor="repeat-month-switch" className="text-sm font-semibold cursor-pointer">
                      Repetir configuración al mes siguiente
                    </Label>
                    <Badge variant="secondary" className="text-[10px] font-mono uppercase">
                      Por defecto
                    </Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {repeatMonthConfig
                      ? `La misma semana se aplicará a ${MONTH_NAMES[nextMonthNumber - 1]} de ${nextMonthYear}. Solo los días laborables quedarán activos.`
                      : `La configuración se aplicará únicamente a ${MONTH_NAMES[currentMonth - 1]}.`}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <Switch
                    id="repeat-month-switch"
                    checked={repeatMonthConfig}
                    onCheckedChange={setRepeatMonthConfig}
                    aria-label="Repetir configuración al mes siguiente"
                  />
                  <span className="text-xs font-medium">
                    {repeatMonthConfig ? 'Aplicar también al mes siguiente' : 'Aplicar solo al mes actual'}
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                {BACKEND_DAYS.map((dayCode, index) => {
                  const dayConfig = weeklyDraft.diasSemana.find((d) => d.diaSemana === dayCode);
                  const isEnabled = Boolean(dayConfig);
                  const dayName = DAY_NAMES[index] || dayCode;

                  return (
                    <Card
                      key={dayCode}
                      className={cn(
                        'shadow-none transition-colors',
                        isEnabled ? 'border-primary/40 bg-card' : 'bg-muted/30 text-muted-foreground'
                      )}
                    >
                      <CardContent className="flex flex-col gap-3.5 p-4">
                        <div className="flex items-center justify-between border-b pb-2.5">
                          <div>
                            <p className="font-heading text-sm font-semibold text-foreground">{dayName}</p>
                            <p className="text-[11px] text-muted-foreground">
                              {isEnabled
                                ? `${dayConfig.brechas?.length || 0} ${dayConfig.brechas?.length === 1 ? 'franja' : 'franjas'}`
                                : 'No laborable'}
                            </p>
                          </div>
                          <Switch
                            checked={isEnabled}
                            onCheckedChange={() => handleDayToggle(dayCode)}
                            aria-label={`Habilitar ${dayName}`}
                          />
                        </div>

                        {isEnabled ? (
                          <div className="space-y-3">
                            <GapEditor
                              gaps={dayConfig.brechas || []}
                              onChange={(newGaps) => handleGapsChange(dayCode, newGaps)}
                            />
                            {dayConfig.brechas?.length > 0 && (
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() => handleCopyGapsToAllActiveDays(dayCode)}
                                className="h-7 w-full gap-1.5 text-[11px] text-muted-foreground hover:text-foreground"
                                title="Copiar estas franjas a todos los días habilitados"
                              >
                                <Copy className="size-3" /> Copiar horario a los demás días
                              </Button>
                            )}
                          </div>
                        ) : (
                          <p className="py-4 text-center text-xs italic text-muted-foreground">
                            Cerrado / No laborable
                          </p>
                        )}
                      </CardContent>
                    </Card>
                  );
                })}
              </div>

              {!isWeeklyDraftValid && (
                <div className="rounded-xl border border-warning/30 bg-warning/10 p-3.5 text-xs text-warning">
                  Habilitá al menos un día con franjas horarias válidas (la hora de fin debe ser posterior a la de inicio).
                </div>
              )}
            </CardContent>

            <CardFooter className="flex items-center justify-between border-t bg-muted/10 px-6 py-4">
              <Button type="button" variant="outline" onClick={() => setStep(1)} className="gap-2">
                <ArrowLeft className="size-4" /> Volver a parámetros
              </Button>
              <Button
                type="button"
                onClick={handlePromptConfirmation}
                disabled={!isWeeklyDraftValid}
                className="gap-2 ml-auto"
              >
                Confirmar horarios y generar agenda <ArrowRight className="size-4" />
              </Button>
            </CardFooter>

            {/* Confirmation AlertDialog (1a) */}
            <AlertDialog open={confirmDialogOpen} onOpenChange={setConfirmDialogOpen}>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>¿Confirmar horarios y generar agenda?</AlertDialogTitle>
                  <AlertDialogDescription asChild>
                    <div className="space-y-2 text-xs text-muted-foreground">
                    <p>
                      Al confirmar, se creará la agenda anual para el año <strong>{currentYear}</strong>.
                    </p>
                    <p>
                      Se activará el mes actual (<strong>{MONTH_NAMES[currentMonth - 1]}</strong>) y el siguiente (
                      <strong>{MONTH_NAMES[nextMonthNumber - 1]}</strong>), y los días de atención de ambos meses quedarán
                      también en estado <strong className="text-foreground">ACTIVO</strong> para recibir turnos.
                    </p>
                    <p>
                      Los demás meses del año se crearán con estado <strong className="text-foreground">INACTIVO</strong>, y todos
                      sus días quedarán también en estado <strong className="text-foreground">INACTIVO</strong>.
                    </p>
                    <p className="font-semibold text-foreground">
                      Días a habilitar por semana: {weeklyDraft.diasSemana.length} días laborables.
                    </p>
                    <p>
                      Repetición mensual:{' '}
                      <strong className="text-foreground">
                        {repeatMonthConfig
                          ? `Sí, aplicar también a ${MONTH_NAMES[nextMonthNumber - 1]} de ${nextMonthYear}`
                          : 'No, aplicar solo al mes actual'}
                      </strong>
                    </p>
                    </div>
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Volver a editar</AlertDialogCancel>
                  <AlertDialogAction onClick={handleConfirmAndExecuteGeneration}>
                    Confirmar e iniciar
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </Card>
        )}

        {/* ========================================================================= */}
        {/* PASO 3: Generación Masiva en Vivo y Pantalla de Éxito Final */}
        {/* ========================================================================= */}
        {step === 3 && (
          <Card className="shadow-lg">
            {isGenerating ? (
              /* Loading / Progress State (4a) */
              <CardContent className="space-y-8 p-8 sm:p-12 text-center">
                <div className="mx-auto flex size-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <Loader2 className="size-8 animate-spin" />
                </div>

                <div className="space-y-2">
                  <h2 className="font-heading text-xl font-bold tracking-tight sm:text-2xl">
                    Aprovisionando tu consultorio y agenda {currentYear}
                  </h2>
                  <p className="text-sm text-muted-foreground">{generationStatusText}</p>
                </div>

                <div className="mx-auto max-w-md space-y-2">
                  <Progress value={generationProgress} className="h-2.5" />
                  <p className="text-right font-mono text-xs font-bold text-primary">{generationProgress}%</p>
                </div>

                {/* Checklist in real time */}
                <div className="mx-auto max-w-md rounded-xl border bg-card/60 p-4 text-left">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground mb-3">
                    Tareas de aprovisionamiento
                  </h3>
                  <ul className="space-y-2.5 text-xs">
                    {checklist.map((item) => (
                      <li key={item.id} className="flex items-center gap-2.5">
                        {item.done ? (
                          <span className="flex size-4 shrink-0 items-center justify-center rounded-full bg-success text-success-foreground text-[10px] font-bold">
                            ✓
                          </span>
                        ) : (
                          <span className="flex size-4 shrink-0 items-center justify-center rounded-full border border-muted-foreground/40 text-transparent text-[10px]">
                            •
                          </span>
                        )}
                        <span className={cn(item.done ? 'text-foreground font-medium' : 'text-muted-foreground')}>
                          {item.label}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              </CardContent>
            ) : (
              /* Final Success / Summary Screen (5a) */
              <>
                <CardHeader className="text-center pb-2 pt-8">
                  <div className="mx-auto flex size-16 items-center justify-center rounded-full bg-success/15 text-success">
                    <CheckCircle2 className="size-10" />
                  </div>
                  <CardTitle className="mt-4 text-2xl font-bold tracking-tight">
                    ¡Todo listo! Tu consultorio está preparado para trabajar
                  </CardTitle>
                  <CardDescription className="text-sm max-w-lg mx-auto">
                    El backend confirmó la configuración profesional y el calendario inicial.
                    Los días laborables de <strong>{MONTH_NAMES[currentMonth - 1]}</strong>
                    {repeatMonthConfig && <> y <strong>{MONTH_NAMES[nextMonthNumber - 1]} de {nextMonthYear}</strong></>} quedaron activos.
                    Los días no laborables y los meses restantes quedaron inactivos.
                  </CardDescription>
                </CardHeader>

                <CardContent className="space-y-6 p-6 sm:p-8">
                  <div className="rounded-xl border bg-muted/20 p-5">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground mb-3">
                      Resumen del consultorio
                    </h3>
                    <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3">
                      <SummaryItem
                        label="Duración por turno"
                        value={`${savedSummary?.duracionAproximadaPorTurno || finalDuration} minutos`}
                      />
                      <SummaryItem
                        label="Capacidad simultánea"
                        value={`${savedSummary?.cantidadMaxTurnosALaVez || finalCapacity} ${(savedSummary?.cantidadMaxTurnosALaVez || finalCapacity) === 1 ? 'turno' : 'turnos'} a la vez`}
                      />
                      <SummaryItem
                        label="Umbral de cancelación"
                        value={
                          (savedSummary?.umbralCancelacionHoras ?? finalThreshold) === 0
                            ? 'Sin límite'
                            : `${savedSummary?.umbralCancelacionHoras ?? finalThreshold} hs previas`
                        }
                      />
                      <SummaryItem
                        label="Modalidad de agenda"
                        value={
                          savedSummary?.agendaSoloManejadaPorProfesional ?? agendaSoloManejada
                            ? 'Exclusiva profesional'
                            : 'Autogestión permitida'
                        }
                      />
                      <SummaryItem
                        label="Días de atención configurados"
                        value={`${calendarSummary?.diasLaborablesPorSemana ?? weeklyDraft.diasSemana.length} días laborables`}
                      />
                      <SummaryItem
                        label="Repetir configuración al mes siguiente"
                        value={
                          calendarSummary?.repetidoAlMesSiguiente
                            ? `Aplicada a ${MONTH_NAMES[nextMonthNumber - 1]} de ${nextMonthYear}`
                            : 'Desactivado (solo mes actual)'
                        }
                      />
                      <SummaryItem
                        label="Meses y días activos"
                        value={(calendarSummary?.mesesConfigurados || [])
                          .map((mes) => `${MONTH_NAMES[mes.nroMes - 1]} ${mes.anio}: ${mes.diasActivos} días`)
                          .join(' · ')}
                      />
                      <SummaryItem
                        label="Meses restantes"
                        value={`${Math.max(0, (calendarSummary?.agendasAnuales?.length || 1) * 12 - (calendarSummary?.mesesConfigurados?.length || 0))} meses inactivos`}
                      />
                      <SummaryItem
                        label="Agenda anual creada"
                        value={(calendarSummary?.agendasAnuales || [currentYear]).join(', ')}
                        className="sm:col-span-2 md:col-span-3"
                      />
                    </dl>
                  </div>

                  <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
                    <Button
                      size="lg"
                      className="w-full sm:w-auto px-8 gap-2 text-base shadow-sm"
                      onClick={handleFinishAndStartWorking}
                    >
                      <Sparkles className="size-4" /> Empezar a trabajar (Ir a Mi Día)
                    </Button>
                  </div>
                </CardContent>
              </>
            )}
          </Card>
        )}
      </div>
    </div>
  );
}

function StepBadge({ stepNumber, title, active, completed }) {
  return (
    <div
      className={cn(
        'flex items-center gap-2 rounded-xl border p-3 text-xs transition-colors',
        active && 'border-primary bg-primary/10 font-bold text-foreground shadow-xs',
        completed && 'border-success/40 bg-success/10 text-success font-semibold',
        !active && !completed && 'border-border bg-card/40 text-muted-foreground opacity-60'
      )}
    >
      <span
        className={cn(
          'flex size-5 shrink-0 items-center justify-center rounded-full text-[11px]',
          completed ? 'bg-success text-success-foreground' : active ? 'bg-primary text-primary-foreground' : 'border'
        )}
      >
        {completed ? '✓' : stepNumber}
      </span>
      <span className="truncate">{title}</span>
    </div>
  );
}

function SummaryItem({ label, value, className }) {
  return (
    <div className={cn('flex flex-col rounded-lg border bg-card px-3.5 py-2.5', className)}>
      <dt className="text-[11px] text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm font-semibold text-foreground">{value}</dd>
    </div>
  );
}
