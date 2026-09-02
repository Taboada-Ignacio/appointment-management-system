import { useState } from 'react';
import { ArrowRight, CalendarDays, CheckCircle, Power, RefreshCw, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import { DatePickerField } from '@/components/DatePickerField';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { IntegrationNotice } from '@/components/ui/IntegrationNotice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Switch } from '@/components/ui/switch';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { getDaysInMonth, MONTH_NAMES, toDateString } from '@/utils/dates';
import { validateGaps } from '@/utils/gaps';
import { canActivate, canInactivate, isMonthConfigured, normalizeStatus } from '@/utils/status';
import {
  useActivateMonth,
  useConfigureMonthDaily,
  useConfigureMonthWeekly,
  useInactivateMonth,
  useRepeatMonth,
  useSetRepeatConfig,
} from '../hooks/useAgenda';
import { GapEditor } from './GapEditor';
import { WEEKLY_TEMPLATE_KEY } from './WeeklyEditor';
import { useToast } from '@/components/ui/ToastProvider';

const DEFAULT_CUSTOM_GAPS = Object.freeze([
  Object.freeze({ horaInicio: '09:00', horaFin: '13:00' }),
]);

export function MonthConfigurator({ monthData, year, onConfigured }) {
  const { success, error: showError } = useToast();
  const [activeTab, setActiveTab] = useState('semana');
  const [confirmInactivate, setConfirmInactivate] = useState(false);
  const configureWeekly = useConfigureMonthWeekly();
  const configureDaily = useConfigureMonthDaily();
  const activateMonth = useActivateMonth();
  const inactivateMonth = useInactivateMonth();
  const repeatMonthMutation = useRepeatMonth();
  const setRepeatConfigMutation = useSetRepeatConfig();

  const monthNum = Number(monthData?.nroMes ?? monthData?.mes ?? 1);
  const monthName = MONTH_NAMES[monthNum - 1] || `Mes ${monthNum}`;
  const monthAgendaId = monthData?.id;
  const monthKey = `${year}-${monthNum}`;
  const defaultDate = toDateString(year, monthNum, 1);
  const lastDate = toDateString(year, monthNum, getDaysInMonth(year, monthNum));
  const [customConfiguration, setCustomConfiguration] = useState(() => ({
    monthKey,
    date: defaultDate,
    gaps: DEFAULT_CUSTOM_GAPS.map((gap) => ({ ...gap })),
  }));
  const isCurrentMonthConfiguration = customConfiguration.monthKey === monthKey;
  const customDate = isCurrentMonthConfiguration ? customConfiguration.date : defaultDate;
  const customGaps = isCurrentMonthConfiguration
    ? customConfiguration.gaps
    : DEFAULT_CUSTOM_GAPS.map((gap) => ({ ...gap }));
  const setCustomDate = (date) => setCustomConfiguration({ monthKey, date, gaps: customGaps });
  const setCustomGaps = (gaps) => setCustomConfiguration({ monthKey, date: customDate, gaps });

  let savedWeeklyDraft = null;
  try {
    const raw = localStorage.getItem(WEEKLY_TEMPLATE_KEY);
    if (raw) savedWeeklyDraft = JSON.parse(raw);
  } catch {
    savedWeeklyDraft = null;
  }

  const hasWeeklyTemplate = Boolean(savedWeeklyDraft?.diasSemana?.length);
  const hasMonthDetail = Array.isArray(monthData?.dias);
  const hasConfig = hasMonthDetail && isMonthConfigured(monthData);
  const status = normalizeStatus(monthData?.estadoActual);
  const canAct = canActivate(status, hasConfig);
  const canInact = canInactivate(status);
  const repeatEnabled = Boolean(monthData?.repetirConfiguracion);
  const canRepeatNow = hasConfig && repeatEnabled && monthNum < 12;
  const selectedDateSummary = monthData?.dias?.find((day) => day.fecha === customDate);
  const selectedDateHasGaps = Number(selectedDateSummary?.cantidadBrechas ?? 0) > 0;
  const customGapsValidation = validateGaps(customGaps);

  async function handleApplyWeekly() {
    if (!monthAgendaId || !hasWeeklyTemplate) return;
    if (!hasMonthDetail) {
      showError('No se pudo verificar el mes', 'La configuración permanece bloqueada hasta cargar el detalle real del mes.');
      return;
    }
    if (hasConfig) {
      showError('Operación bloqueada por seguridad', 'La plantilla semanal no se puede aplicar sobre un mes con brechas porque el backend no ofrece un preview de turnos afectados.');
      return;
    }
    const invalidDay = savedWeeklyDraft.diasSemana.find(({ brechas }) => !brechas?.length || !validateGaps(brechas).valid);
    if (invalidDay) {
      const validation = validateGaps(invalidDay.brechas);
      showError('Revisá la plantilla semanal', validation.errors[0] || `El día ${invalidDay.diaSemana} necesita al menos una franja válida.`);
      return;
    }
    try {
      await configureWeekly.mutateAsync({ mesAgendaId: monthAgendaId, diasSemana: savedWeeklyDraft.diasSemana });
      success('Plantilla semanal aplicada', `Se configuraron los días de ${monthName} ${year}.`);
      onConfigured?.();
    } catch (error) {
      showError('Error al configurar la semana', error.message);
    }
  }

  async function handleSaveCustomDate() {
    if (!monthAgendaId || !customDate) return;
    if (!hasMonthDetail) {
      showError('No se pudo verificar el mes', 'La fecha no se puede configurar hasta cargar el detalle real del mes.');
      return;
    }
    if (customDate < defaultDate || customDate > lastDate) {
      showError('Fecha fuera del mes', `Elegí una fecha entre ${defaultDate} y ${lastDate}.`);
      return;
    }
    if (selectedDateHasGaps) {
      showError('Edición bloqueada por seguridad', 'La fecha elegida ya tiene brechas. El backend no ofrece un preview de turnos afectados antes de reemplazarlas.');
      return;
    }
    if (customGaps.length === 0 || !customGapsValidation.valid) {
      showError('Revisá las brechas', customGapsValidation.errors[0] || 'Agregá al menos una franja horaria válida.');
      return;
    }
    try {
      await configureDaily.mutateAsync({ mesAgendaId: monthAgendaId, dias: [{ fecha: customDate, brechas: customGaps }] });
      success('Día configurado', `Se guardaron las brechas para ${customDate}.`);
      onConfigured?.();
    } catch (error) {
      showError('Error al configurar el día', error.message);
    }
  }

  async function handleActivate() {
    if (!monthAgendaId) return;
    if (!hasConfig) {
      showError('Mes sin brechas', 'No podés activar un mes sin días de atención configurados.');
      return;
    }
    try {
      await activateMonth.mutateAsync(monthAgendaId);
      success('Mes activado', `${monthName} ${year} ya está habilitado para recibir turnos.`);
      onConfigured?.();
    } catch (error) {
      showError('Error al activar mes', error.message);
    }
  }

  async function handleInactivate() {
    if (!monthAgendaId) return;
    try {
      await inactivateMonth.mutateAsync(monthAgendaId);
      success('Mes inactivado', `${monthName} ${year} ya no recibirá nuevos turnos.`);
      setConfirmInactivate(false);
      onConfigured?.();
    } catch (error) {
      showError('Error al inactivar mes', error.message);
    }
  }

  async function handleToggleRepeat(checked) {
    if (!monthAgendaId) return;
    try {
      await setRepeatConfigMutation.mutateAsync({ mesAgendaId: monthAgendaId, repetirConfiguracion: checked });
      success('Configuración actualizada', checked ? 'Se activó la repetición automática.' : 'Se desactivó la repetición automática.');
      onConfigured?.();
    } catch (error) {
      showError('Error al cambiar repetición', error.message);
    }
  }

  async function handleRepeatNow() {
    if (!monthAgendaId) return;
    if (monthNum >= 12) {
      showError('Repetición no disponible', 'El backend no permite replicar diciembre hacia una agenda del año siguiente.');
      return;
    }
    if (!repeatEnabled) {
      showError('Activá la repetición primero', 'El contrato del backend exige habilitar la repetición antes de replicar el mes.');
      return;
    }
    if (!hasConfig) {
      showError('Mes sin brechas', 'Configurá al menos un día antes de replicar el mes.');
      return;
    }
    try {
      await repeatMonthMutation.mutateAsync(monthAgendaId);
      success('Configuración replicada', `Se copió la configuración de ${monthName} al mes siguiente.`);
      onConfigured?.();
    } catch (error) {
      showError('Error al replicar mes', error.message);
    }
  }

  const activationHint = !hasConfig ? 'Debés configurar al menos una franja antes de activar' : 'El estado actual no permite activar el mes';

  return (
    <div className="space-y-5">
      <Card className="shadow-none">
        <CardContent className="flex flex-col items-start justify-between gap-4 p-5 md:flex-row md:items-center">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-info">Estado del mes</p>
            <div className="mt-1.5 flex items-center gap-3">
              <h3 className="font-heading text-lg font-semibold tracking-tight">{monthName} {year}</h3>
              <StatusBadge status={status} />
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              {hasConfig ? 'El mes cuenta con franjas horarias configuradas.' : 'El mes está vacío. Configurá los días antes de activarlo.'}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {canInact && (
              <Button type="button" variant="destructive" onClick={() => setConfirmInactivate(true)} disabled={inactivateMonth.isPending}>
                <Power data-icon="inline-start" />
                Inactivar mes
              </Button>
            )}
            <TooltipProvider delayDuration={250}>
            <Tooltip>
              <TooltipTrigger asChild>
                <span>
                  <Button type="button" onClick={handleActivate} disabled={!canAct || activateMonth.isPending}>
                    <CheckCircle data-icon="inline-start" />
                    Activar mes
                  </Button>
                </span>
              </TooltipTrigger>
              {!canAct && <TooltipContent>{activationHint}</TooltipContent>}
            </Tooltip>
            </TooltipProvider>
          </div>
        </CardContent>
      </Card>

      <Card className="shadow-none">
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <CardHeader className="border-b pb-4">
            <CardTitle>Configurar disponibilidad</CardTitle>
            <CardDescription>Aplicá tu semana habitual o definí una fecha puntual.</CardDescription>
            <TabsList className="mt-3 w-full sm:w-fit">
              <TabsTrigger value="semana" onClick={() => setActiveTab('semana')}>
                <Sparkles data-icon="inline-start" />
                Aplicar plantilla semanal
              </TabsTrigger>
              <TabsTrigger value="fecha" onClick={() => setActiveTab('fecha')}>
                <CalendarDays data-icon="inline-start" />
                Configurar por fecha puntual
              </TabsTrigger>
            </TabsList>
          </CardHeader>
          <CardContent className="p-5 sm:p-6">
            <TabsContent value="semana" className="mt-0 max-w-2xl space-y-4">
              <p className="text-sm leading-6">
                Aplicá el horario habitual guardado en tu plantilla semanal a todos los días de <strong>{monthName} {year}</strong>.
              </p>
              {hasWeeklyTemplate ? (
                <div className="rounded-xl border bg-muted/30 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-xs font-semibold">Plantilla disponible</span>
                    <Button asChild variant="link" size="sm" className="h-auto px-0">
                      <Link to="/profesional/mi-semana">Editar plantilla <ArrowRight data-icon="inline-end" /></Link>
                    </Button>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {savedWeeklyDraft.diasSemana.length} días configurados ({savedWeeklyDraft.diasSemana.map((day) => day.diaSemana).join(', ')}).
                  </p>
                </div>
              ) : (
                <div className="flex flex-col items-start justify-between gap-3 rounded-xl border border-warning/25 bg-warning/8 p-4 sm:flex-row sm:items-center">
                  <span className="text-xs text-warning">No tenés una plantilla semanal guardada aún.</span>
                  <Button asChild variant="outline" size="sm"><Link to="/profesional/mi-semana">Crear plantilla</Link></Button>
                </div>
              )}
              {hasConfig && (
                <IntegrationNotice type="warning" title="Aplicación semanal bloqueada">
                  Este mes ya tiene brechas configuradas. La plantilla semanal no puede aplicarse porque reemplazaría toda la configuración sin un preview de turnos afectados.
                </IntegrationNotice>
              )}
              <Button type="button" onClick={handleApplyWeekly} disabled={!hasMonthDetail || hasConfig || !hasWeeklyTemplate || configureWeekly.isPending}>
                {configureWeekly.isPending ? 'Aplicando...' : 'Aplicar plantilla al mes'}
              </Button>
            </TabsContent>

            <TabsContent value="fecha" className="mt-0 max-w-xl space-y-5">
              <p className="text-sm leading-6">Elegí una fecha específica de {monthName} {year} para configurar sus franjas horarias de atención:</p>
              <DatePickerField id="custom-date-picker" label="Fecha a configurar" value={customDate} min={defaultDate} max={lastDate} onChange={setCustomDate} />
              {selectedDateHasGaps && (
                <IntegrationNotice type="warning" title="Fecha protegida contra sobreescritura">
                  Esta fecha ya tiene brechas configuradas. No se puede reemplazar sin consultar antes los turnos afectados, y ese contrato todavía no existe en el backend.
                </IntegrationNotice>
              )}
              <div className="space-y-2">
                <p className="text-sm font-medium">Brechas horarias para esta fecha</p>
                <GapEditor gaps={customGaps} onChange={setCustomGaps} />
              </div>
              <Button type="button" onClick={handleSaveCustomDate} disabled={!hasMonthDetail || selectedDateHasGaps || configureDaily.isPending || customGaps.length === 0 || !customGapsValidation.valid}>
                {configureDaily.isPending ? 'Guardando...' : 'Guardar brechas para esta fecha'}
              </Button>
            </TabsContent>
          </CardContent>
        </Tabs>
      </Card>

      <Card className="shadow-none">
        <CardHeader>
          <div className="flex items-center gap-2">
            <RefreshCw className="size-4 text-info" />
            <CardTitle>Replicación y repetición</CardTitle>
          </div>
          <CardDescription>Copiá la configuración de este mes al siguiente o habilitá la repetición automática.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col items-start justify-between gap-4 pt-0 sm:flex-row sm:items-center">
          <div className="flex items-center gap-3">
            <Switch id="repeat-month" role="checkbox" checked={repeatEnabled} onCheckedChange={handleToggleRepeat} disabled={monthNum >= 12 || setRepeatConfigMutation.isPending} />
            <label htmlFor="repeat-month" className="text-sm font-medium leading-5">Repetir configuración automáticamente al mes siguiente</label>
          </div>
          <Button type="button" variant="outline" onClick={handleRepeatNow} disabled={!canRepeatNow || repeatMonthMutation.isPending}>
            <RefreshCw data-icon="inline-start" />
            Replicar ahora al mes siguiente
          </Button>
        </CardContent>
        {monthNum >= 12 && (
          <CardContent className="pt-0">
            <IntegrationNotice type="warning" title="Límite del contrato anual">
              La API actual no permite replicar diciembre hacia enero de la agenda siguiente.
            </IntegrationNotice>
          </CardContent>
        )}
      </Card>

      <ConfirmDialog
        open={confirmInactivate}
        onOpenChange={setConfirmInactivate}
        title={`Inactivar ${monthName} ${year}`}
        description="El mes dejará de aceptar nuevos turnos. La configuración de horarios se conservará."
        confirmLabel="Inactivar mes"
        variant="danger"
        loading={inactivateMonth.isPending}
        onConfirm={handleInactivate}
      />
    </div>
  );
}
