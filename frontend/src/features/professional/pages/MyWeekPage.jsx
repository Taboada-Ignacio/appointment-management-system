import { useState } from 'react';
import { PageHeader } from '../components/PageHeader';
import { WeeklyEditor } from '../components/WeeklyEditor';
import {
  useMonths,
  useMonthDetail,
  useConfigureMonthWeekly,
  useAnnualAgendas,
} from '../hooks/useAgenda';
import { IntegrationNotice } from '../../../components/ui/IntegrationNotice';
import { useToast } from '../../../components/ui/ToastProvider';
import { getCurrentYearMonth, MONTH_NAMES } from '../../../utils/dates';
import { validateGaps } from '../../../utils/gaps';
import { isMonthConfigured } from '../../../utils/status';
import { professionalContext } from '../../../config/professional';
import { AlertTriangle, CheckCircle2, LoaderCircle, Sparkles } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

export function MyWeekPage() {
  const { year: currentYear } = getCurrentYearMonth(professionalContext.timezone);
  const { data: agendas } = useAnnualAgendas();
  const currentAgenda = agendas?.find((a) => Number(a.anio) === currentYear);

  const { data: months } = useMonths(currentYear, { enabled: Boolean(currentAgenda) });
  const configureMonthWeekly = useConfigureMonthWeekly();
  const { success, error: showError } = useToast();

  const [selectedMonthId, setSelectedMonthId] = useState('');

  const selectedMonth = months?.find((m) => String(m.id) === String(selectedMonthId));
  const {
    data: selectedMonthDetail,
    isLoading: isLoadingSelectedMonth,
    error: selectedMonthError,
  } = useMonthDetail(selectedMonth?.id);
  const hasVerifiedSelectedMonth = Boolean(
    selectedMonthDetail && Array.isArray(selectedMonthDetail.dias)
  );
  const isSelectedConfigured = hasVerifiedSelectedMonth
    ? isMonthConfigured(selectedMonthDetail)
    : false;

  const handleApply = async (draft) => {
    if (!selectedMonthId || !draft?.diasSemana) return;

    if (!hasVerifiedSelectedMonth || selectedMonthError) {
      showError(
        'No se pudo verificar el mes',
        'La plantilla permanece bloqueada hasta consultar el detalle real del mes seleccionado.'
      );
      return;
    }

    if (isSelectedConfigured) {
      showError(
        'Operación bloqueada por seguridad',
        'El mes seleccionado ya tiene brechas configuradas. Reemplazarlo destructivamente requiere un contrato de impacto que el backend aún no expone.'
      );
      return;
    }

    const invalidDay = draft.diasSemana.find(
      ({ brechas }) => !brechas?.length || !validateGaps(brechas).valid
    );
    if (invalidDay) {
      const validation = validateGaps(invalidDay.brechas);
      showError(
        'Plantilla semanal inválida',
        validation.errors[0] || `El día ${invalidDay.diaSemana} necesita al menos una franja válida.`
      );
      return;
    }

    try {
      await configureMonthWeekly.mutateAsync({
        mesAgendaId: Number(selectedMonthId),
        diasSemana: draft.diasSemana,
      });
      const monthNum = Number(selectedMonth.nroMes ?? selectedMonth.mes ?? 1);
      success(
        'Plantilla aplicada con éxito',
        `Se configuró la disponibilidad semanal para ${MONTH_NAMES[monthNum - 1]} ${currentYear}.`
      );
      setSelectedMonthId('');
    } catch (err) {
      showError('Error al aplicar plantilla', err.message);
    }
  };

  const handleSaveDraft = () => {
    success('Borrador guardado', 'La plantilla semanal se almacenó localmente para tus próximas configuraciones.');
  };

  const targetMonthLabel = selectedMonth
    ? `${MONTH_NAMES[Number(selectedMonth.nroMes ?? selectedMonth.mes ?? 1) - 1]} ${currentYear}`
    : 'mes';

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Configuración de horarios habituales"
        title="Cambiar mi semana"
        description="Definí tu esquema semanal tipo. Esta plantilla se guarda como borrador de trabajo y podés aplicarla a cualquier mes sin configuración."
      />

      {/* Target month selection */}
      <Card className="shadow-none">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Sparkles className="size-4 text-info" />
            <CardTitle>Aplicar plantilla semanal a un mes</CardTitle>
          </div>
          <CardDescription>Solo están habilitados los meses que el backend confirma como vacíos.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4 pt-0">
        <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center">
          <div className="w-full max-w-sm flex-1">
            <Select value={selectedMonthId} onValueChange={setSelectedMonthId}>
              <SelectTrigger id="target-month-select" className="w-full" aria-label="Seleccionar mes de destino">
                <SelectValue placeholder={`Seleccioná un mes de destino (${currentYear})`} />
              </SelectTrigger>
              <SelectContent>
              {months?.map((m) => {
                const monthNum = Number(m.nroMes ?? m.mes ?? 1);
                const name = MONTH_NAMES[monthNum - 1];
                return (
                  <SelectItem key={m.id} value={String(m.id)}>{name}</SelectItem>
                );
              })}
              </SelectContent>
            </Select>
          </div>

          <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
            {selectedMonth
              ? isLoadingSelectedMonth
                ? <><LoaderCircle className="size-3.5 animate-spin" /> Verificando el detalle real del mes…</>
                : selectedMonthError
                  ? <><AlertTriangle className="size-3.5 text-warning" /> No se pudo verificar el mes; aplicación bloqueada</>
                  : isSelectedConfigured
                ? <><AlertTriangle className="size-3.5 text-warning" /> Este mes ya posee configuración previa</>
                : hasVerifiedSelectedMonth
                  ? <><CheckCircle2 className="size-3.5 text-success" /> Mes vacío y listo para recibir la plantilla</>
                  : 'Verificación pendiente'
              : 'Elegí el mes al que querés transferir estos horarios'}
          </span>
        </div>

        {isSelectedConfigured && (
          <IntegrationNotice
            type="warning"
            title="Bloqueo preventivo de sobreescritura"
          >
            Este mes ya cuenta con brechas de atención cargadas. El endpoint de modo semana reemplaza íntegramente la agenda sin emitir un análisis previo de turnos afectados. Por seguridad operativa, realizá los cambios finos en la vista de mes o configurá meses vacíos.
          </IntegrationNotice>
        )}
        </CardContent>
      </Card>

      <WeeklyEditor
        initialDraft={undefined}
        onSave={handleSaveDraft}
        onApply={handleApply}
        canApply={Boolean(
          selectedMonthId &&
          hasVerifiedSelectedMonth &&
          !selectedMonthError &&
          !isSelectedConfigured &&
          !configureMonthWeekly.isPending
        )}
        applyTargetLabel={targetMonthLabel}
      />
    </div>
  );
}
