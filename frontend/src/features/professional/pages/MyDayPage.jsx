import { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { DailyTimeline } from '../components/DailyTimeline';
import { GapEditor } from '../components/GapEditor';
import {
  useAnnualAgendas,
  useSelectableDays,
  useDayDetail,
  useUpdateDayGaps,
  useMonths,
} from '../hooks/useAgenda';
import { IntegrationNotice } from '../../../components/ui/IntegrationNotice';
import { EmptyState } from '../../../components/ui/EmptyState';
import { SkeletonTimeline } from '../../../components/ui/LoadingSkeleton';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { useToast } from '../../../components/ui/ToastProvider';
import { getTodayInTimezone, isPast, formatDateLong, addDays, parseDateString } from '../../../utils/dates';
import { validateGaps } from '../../../utils/gaps';
import { deriveTemporalStatus } from '../../../utils/status';
import { professionalContext } from '../../../config/professional';
import { Calendar, ChevronLeft, ChevronRight, Save, X, CalendarClock } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function MyDayPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const timezone = professionalContext.timezone;
  const today = getTodayInTimezone(timezone);
  const dateStr = searchParams.get('fecha') || searchParams.get('date') || today;

  const [isEditing, setIsEditing] = useState(false);
  const [editingGaps, setEditingGaps] = useState([]);
  const { success, error: showError } = useToast();

  const { year, month } = parseDateString(dateStr);
  const { data: agendas, isLoading: isLoadingAgendas } = useAnnualAgendas();
  const yearAgenda = agendas?.find((agenda) => Number(agenda.anio) === year);
  const { data: months, isLoading: isLoadingMonths } = useMonths(
    year,
    { enabled: Boolean(yearAgenda) }
  );
  const currentMonth = months?.find((m) => Number(m.nroMes ?? m.mes) === month);

  // Fetch selectable days for this date to obtain diaAgendaId
  const { data: selectableDays, isLoading: isLoadingDays, error: daysError } = useSelectableDays(
    currentMonth ? dateStr : null,
    currentMonth ? dateStr : null
  );

  const dayInfo = selectableDays?.find((d) => d.fecha === dateStr) || selectableDays?.[0];
  const dayId = dayInfo?.diaAgendaId || dayInfo?.id;

  const { data: dayDetail, isLoading: isLoadingDetail } = useDayDetail(dayId);
  const updateDayGaps = useUpdateDayGaps();
  const hasExistingGaps =
    Number(dayInfo?.cantidadBrechas ?? 0) > 0 ||
    Boolean(dayDetail?.brechas?.length);

  const handlePrevDay = () => {
    setSearchParams({ fecha: addDays(dateStr, -1) });
    setIsEditing(false);
  };

  const handleNextDay = () => {
    setSearchParams({ fecha: addDays(dateStr, 1) });
    setIsEditing(false);
  };

  const handleToday = () => {
    setSearchParams({ fecha: today });
    setIsEditing(false);
  };

  const handleStartEditing = (dayToEdit) => {
    if (hasExistingGaps) {
      showError(
        'Edición bloqueada por seguridad',
        'Este día ya tiene brechas configuradas. El backend no permite consultar el impacto sobre turnos antes de reemplazarlas.'
      );
      return;
    }
    setEditingGaps(dayToEdit?.brechas || dayDetail?.brechas || []);
    setIsEditing(true);
  };

  const handleCancelEditing = () => {
    setIsEditing(false);
  };

  const handleSaveGaps = async () => {
    if (!dayId) {
      showError('No se puede guardar', 'No se encontró el identificador del día en la agenda.');
      return;
    }
    if (hasExistingGaps) {
      showError(
        'Edición bloqueada por seguridad',
        'No se pueden sobrescribir brechas existentes sin un preview de turnos afectados.'
      );
      setIsEditing(false);
      return;
    }

    const validation = validateGaps(editingGaps);
    if (editingGaps.length === 0 || !validation.valid) {
      showError(
        'Revisá las brechas',
        validation.errors[0] || 'Agregá al menos una franja horaria válida antes de guardar.'
      );
      return;
    }
    try {
      await updateDayGaps.mutateAsync({
        diaAgendaId: dayId,
        brechas: editingGaps,
      });
      success('Día actualizado', `Se guardaron las franjas horarias de ${formatDateLong(dateStr, timezone)}.`);
      setIsEditing(false);
    } catch (err) {
      showError('Error al guardar', err.message || 'No se pudieron actualizar las brechas.');
    }
  };

  const isLoading =
    isLoadingAgendas ||
    (Boolean(yearAgenda) && isLoadingMonths) ||
    (Boolean(currentMonth) && isLoadingDays) ||
    (Boolean(dayId) && isLoadingDetail);
  const isPastDay = isPast(dateStr, timezone);
  const rawStatus = dayDetail?.estadoActual ?? dayInfo?.estadoActual ?? dayInfo?.estado;
  const temporalStatus = deriveTemporalStatus(rawStatus, dateStr, timezone);
  const editingValidation = validateGaps(editingGaps);

  const currentDayData = {
    id: dayId || dateStr,
    fecha: dateStr,
    estadoActual: temporalStatus,
    brechas: dayDetail?.brechas || [],
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Agenda diaria"
        title="Mi día"
        description={formatDateLong(dateStr, timezone)}
        status={<StatusBadge status={temporalStatus} />}
        actions={
          <div className="flex items-center gap-1 rounded-xl border bg-card p-1 shadow-xs">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handlePrevDay}
              aria-label="Día anterior"
            >
              <ChevronLeft className="h-4 w-4" />
              <span className="hidden sm:inline">Anterior</span>
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleToday}
            >
              Hoy
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleNextDay}
              aria-label="Día siguiente"
            >
              <span className="hidden sm:inline">Siguiente</span>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        }
      />

      {daysError && (
        <EmptyState
          icon={CalendarClock}
          title="Error al consultar el día"
          description="No se pudo cargar la información del día desde el backend."
          action={{
            label: 'Reintentar',
            onClick: () => window.location.reload(),
          }}
        />
      )}

      {!isLoading && !currentMonth && (
        <EmptyState
          icon={Calendar}
          title={`Sin agenda anual para ${year}`}
          description={`No existe una agenda anual creada para el año ${year}. Creá la agenda desde el panel de configuración para comenzar a operar.`}
          action={{
            label: 'Ir a configuración de agenda',
            onClick: () => navigate('/profesional/configuracion'),
          }}
        />
      )}

      {!isLoading && currentMonth && hasExistingGaps && (
        <IntegrationNotice
          type="warning"
          title="Edición del día bloqueada"
        >
          Este día ya contiene brechas. Hasta que el backend exponga un preview de turnos afectados, solo se permite configurar días vacíos.
        </IntegrationNotice>
      )}

      {isEditing && (
        <Card className="border-ring/50 shadow-none">
          <CardHeader className="flex-row items-start justify-between border-b">
            <div>
              <CardTitle>Editar franjas de atención del día</CardTitle>
              <p className="mt-1 text-xs text-muted-foreground">
                Definí las horas de inicio y fin para {formatDateLong(dateStr, timezone)}.
              </p>
            </div>
            <Button
              type="button"
              onClick={handleCancelEditing}
              variant="ghost"
              size="icon-sm"
              aria-label="Cancelar edición"
            >
              <X className="h-5 w-5" />
            </Button>
          </CardHeader>
          <CardContent className="space-y-4 p-5">

          <IntegrationNotice
            type="warning"
            title="Advertencia sobre turnos previos"
          >
            Modificar las brechas directamente actualizará el día. Si existían turnos asignados fuera de las nuevas brechas, el backend procesará sus bajas automáticamente sin preview previo.
          </IntegrationNotice>

          <GapEditor gaps={editingGaps} onChange={setEditingGaps} />

          <div className="flex items-center justify-end gap-2 border-t pt-4">
            <Button
              type="button"
              onClick={handleCancelEditing}
              disabled={updateDayGaps.isPending}
              variant="outline"
            >
              Cancelar
            </Button>
            <Button
              type="button"
              onClick={handleSaveGaps}
              disabled={
                updateDayGaps.isPending ||
                editingGaps.length === 0 ||
                !editingValidation.valid ||
                hasExistingGaps
              }
            >
              <Save className="h-3.5 w-3.5" />
              <span>{updateDayGaps.isPending ? 'Guardando...' : 'Guardar cambios'}</span>
            </Button>
          </div>
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <SkeletonTimeline className="rounded-xl border bg-card p-6" />
      ) : (
        <DailyTimeline
          day={currentDayData}
          timezone={timezone}
          onEditGaps={handleStartEditing}
          canEdit={!isPastDay && Boolean(dayId) && !isEditing && !hasExistingGaps}
        />
      )}
    </div>
  );
}
