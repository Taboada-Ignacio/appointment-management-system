import { useEffect, useRef, useState } from 'react';
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
  useAssignedAppointments,
  useCancelAppointment,
} from '../hooks/useAgenda';
import { IntegrationNotice } from '../../../components/ui/IntegrationNotice';
import { EmptyState } from '../../../components/ui/EmptyState';
import { SkeletonTimeline } from '../../../components/ui/LoadingSkeleton';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { useToast } from '../../../components/ui/ToastProvider';
import { getTodayInTimezone, isPast, formatDateLong, addDays, parseDateString, getMonthRange } from '../../../utils/dates';
import { validateGaps } from '../../../utils/gaps';
import { deriveTemporalStatus } from '../../../utils/status';
import { professionalContext } from '../../../config/professional';
import { AlertTriangle, Calendar, CheckCircle2, ChevronLeft, ChevronRight, Save, X, CalendarClock, CalendarPlus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Textarea } from '@/components/ui/textarea';
import { MonthCalendar } from '../components/MonthCalendar';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { validateAppointmentReschedule } from '../api/agendaApi';
import { listSuggestedTimes } from '../api/appointmentApi';

export function MyDayPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const timezone = professionalContext.timezone;
  const today = getTodayInTimezone(timezone);
  const dateStr = searchParams.get('fecha') || searchParams.get('date') || today;

  const [isEditing, setIsEditing] = useState(false);
  const [editingGaps, setEditingGaps] = useState([]);
  const [selectedAppointmentId, setSelectedAppointmentId] = useState(() => searchParams.get('turno'));
  const [cancelOpen, setCancelOpen] = useState(false);
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
  const { data: assignedAppointments = [], isLoading: isLoadingAppointments } = useAssignedAppointments(
    currentMonth ? dateStr : null,
    currentMonth ? dateStr : null
  );
  const updateDayGaps = useUpdateDayGaps();
  const cancelAppointment = useCancelAppointment();
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
    (Boolean(dayId) && isLoadingDetail) ||
    (Boolean(currentMonth) && isLoadingAppointments);
  const isPastDay = isPast(dateStr, timezone);
  const rawStatus = dayDetail?.estadoActual ?? dayInfo?.estadoActual ?? dayInfo?.estado;
  const temporalStatus = deriveTemporalStatus(rawStatus, dateStr, timezone);
  const selectedAppointment = assignedAppointments.find(
    (appointment) => String(appointment.id ?? appointment.turnoId) === String(selectedAppointmentId)
  );

  const handleSelectAppointment = (appointment) => {
    const appointmentId = appointment.id ?? appointment.turnoId;
    setSelectedAppointmentId((current) => String(current) === String(appointmentId) ? null : appointmentId);
  };

  const handleCancelAppointment = async (motivo) => {
    const response = await cancelAppointment.mutateAsync({
      appointmentId: selectedAppointment.id ?? selectedAppointment.turnoId,
      motivo: motivo.trim() || undefined,
    });
    const anticipated = response?.resultado === 'ELIMINADO_ANTICIPADAMENTE';
    success(
      'Turno cancelado',
      anticipated
        ? 'Se eliminó anticipadamente y se registraron la auditoría y la notificación.'
        : 'Quedó cancelado con su motivo, historial y notificación.'
    );
    setCancelOpen(false);
    setSelectedAppointmentId(null);
  };

  const editingValidation = validateGaps(editingGaps);

  const currentDayData = {
    id: dayId || dateStr,
    fecha: dateStr,
    estadoActual: temporalStatus,
    brechas: dayDetail?.brechas || [],
    tiposExcepcion: dayInfo?.tiposExcepcion ?? dayInfo?.excepciones ?? [],
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Agenda diaria"
        title="Mi día"
        description={formatDateLong(dateStr, timezone)}
        status={<StatusBadge status={temporalStatus} />}
        actions={
          <div className="flex items-center gap-2"><Button type="button" onClick={() => navigate(`/profesional/turnos/nuevo?fecha=${dateStr}`)}><CalendarPlus/>Nuevo turno</Button><div className="flex items-center gap-1 rounded-xl border bg-card p-1 shadow-xs">
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
          </div></div>
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
        <div className={selectedAppointment ? 'grid items-start gap-4 lg:grid-cols-2' : ''}>
          <DailyTimeline
            day={currentDayData}
            timezone={timezone}
            onEditGaps={handleStartEditing}
            canEdit={!isPastDay && Boolean(dayId) && !isEditing && !hasExistingGaps}
            appointments={assignedAppointments}
            selectedAppointmentId={selectedAppointmentId}
            onSelectAppointment={handleSelectAppointment}
            showIntegrationNotice={false}
          />
          {selectedAppointment && (
            <AppointmentDetails
              appointment={selectedAppointment}
              timezone={timezone}
              onClose={() => setSelectedAppointmentId(null)}
              onCancel={() => setCancelOpen(true)}
              onReschedule={() => navigate(`/profesional/turnos/${selectedAppointment.id ?? selectedAppointment.turnoId}/cambiar-dia?fecha=${selectedAppointment.fecha}`)}
            />
          )}
        </div>
      )}

      {selectedAppointment && (
        <>
          <CancelAppointmentDialog
            open={cancelOpen}
            appointment={selectedAppointment}
            loading={cancelAppointment.isPending}
            onOpenChange={setCancelOpen}
            onConfirm={handleCancelAppointment}
          />
        </>
      )}
    </div>
  );
}

function AppointmentDetails({ appointment, timezone, onClose, onCancel, onReschedule }) {
  const client = appointment.cliente || {};
  const attention = appointment.tipoAtencion || {};
  const fullName = `${client.nombre || ''} ${client.apellido || ''}`.trim() || 'Sin nombre informado';
  const dateTime = (value) => value
    ? new Intl.DateTimeFormat('es-AR', {
        timeZone: timezone,
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(value))
    : '—';
  const value = (data) => data === null || data === undefined || data === '' ? '—' : String(data);

  const rows = [
    ['Estado', appointment.estado || 'ASIGNADO'],
    ['Turno', appointment.id ?? appointment.turnoId],
    ['Fecha', appointment.fecha],
    ['Inicio estimado', dateTime(appointment.inicioEstimado)],
    ['Fin estimado', dateTime(appointment.finEstimado)],
    ['Inicio real', dateTime(appointment.inicioReal)],
    ['Fin real', dateTime(appointment.finReal)],
    ['Origen', appointment.origen],
    ['Paciente', fullName],
    ['Documento', [client.tipoDocumento, client.numeroDocumento].filter(Boolean).join(' ')],
    ['ID del paciente', appointment.clienteId ?? client.id],
    ['Tipo de atención', attention.nombre],
    ['Duración', attention.duracionMinutos ? `${attention.duracionMinutos} minutos` : null],
    ['Capacidad simultánea', attention.capacidadSimultanea],
    ['ID del tipo de atención', appointment.tipoAtencionId ?? attention.id],
    ['ID del día de agenda', appointment.diaAgendaId],
    ['ID del profesional', appointment.profesionalId],
    ['Observaciones', appointment.observaciones],
  ];

  return (
    <Card aria-label={`Detalle del turno de ${fullName}`} className="shadow-none lg:sticky lg:top-4">
      <CardHeader className="flex-row items-start justify-between border-b">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-info">Turno seleccionado</p>
          <CardTitle className="mt-1">{fullName}</CardTitle>
        </div>
        <CardAction className="flex flex-wrap items-center justify-end gap-2">
          <Button type="button" size="sm" variant="destructive" onClick={onCancel}>Cancelar turno</Button>
          <Button type="button" size="sm" variant="outline" onClick={onReschedule}>Cambiar día</Button>
          <Button type="button" variant="ghost" size="icon-sm" aria-label="Cerrar detalle del turno" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent className="p-5">
        <dl className="grid gap-x-5 gap-y-3 sm:grid-cols-2">
          {rows.map(([label, data]) => (
            <div key={label} className={label === 'Observaciones' ? 'sm:col-span-2' : ''}>
              <dt className="text-xs font-semibold text-muted-foreground">{label}</dt>
              <dd className="mt-0.5 break-words text-sm">{value(data)}</dd>
            </div>
          ))}
        </dl>
      </CardContent>
    </Card>
  );
}

function CancelAppointmentDialog({ open, appointment, loading, onOpenChange, onConfirm }) {
  const [reason, setReason] = useState('');
  const [message, setMessage] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    setMessage('');
    try {
      await onConfirm(reason);
      setReason('');
    } catch (error) {
      setMessage(error.message || 'No se pudo cancelar el turno.');
    }
  };

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title="Cancelar turno"
      description="El sistema aplicará la regla temporal configurada: antes del umbral eliminará el turno; dentro del umbral lo conservará como CANCELADO y exigirá un motivo. Un turno ya iniciado no puede cancelarse."
      confirmLabel="Confirmar cancelación"
      variant="danger"
      loading={loading}
      onConfirm={submit}
    >
      <div className="space-y-2 py-2">
        <label htmlFor="appointment-cancellation-reason" className="text-sm font-semibold">Motivo</label>
        <Textarea
          id="appointment-cancellation-reason"
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          placeholder="Obligatorio si la cancelación está dentro del umbral"
        />
        {message && <p role="alert" className="text-sm font-medium text-destructive">{message}</p>}
        <p className="text-xs text-muted-foreground">Turno #{appointment.id ?? appointment.turnoId}</p>
      </div>
    </ConfirmDialog>
  );
}

function RescheduleAppointmentDialog({ open, appointment, timezone, loading, onOpenChange, onConfirm }) {
  const initial = parseDateString(appointment.fecha);
  const [view, setView] = useState({ year: initial.year, month: initial.month });
  const [selectedDay, setSelectedDay] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [alternatives, setAlternatives] = useState([]);
  const [validationState, setValidationState] = useState('idle');
  const [step, setStep] = useState(1);
  const [reason, setReason] = useState('Solicitud del paciente');
  const [observation, setObservation] = useState('');
  const [showTimeline, setShowTimeline] = useState(false);
  const [message, setMessage] = useState('');
  const validationSequence = useRef(0);
  const { firstDay, lastDay } = getMonthRange(view.year, view.month);
  const { data: days = [], isLoading } = useSelectableDays(open ? firstDay : null, open ? lastDay : null);
  const startTime = instantTimeLabel(appointment.inicioEstimado, timezone);
  const endTime = instantTimeLabel(appointment.finEstimado, timezone);
  const { data: selectedDayDetail } = useDayDetail(showTimeline ? selectedDay?.diaAgendaId ?? selectedDay?.id : null);
  const { data: selectedDayAppointments = [] } = useAssignedAppointments(
    showTimeline ? selectedDay?.fecha : null,
    showTimeline ? selectedDay?.fecha : null
  );
  const recommendedDays = days.filter((day) => day.seleccionable && day.fecha !== appointment.fecha)
    .sort((a, b) => a.fecha.localeCompare(b.fecha)).slice(0, 3);

  useEffect(() => {
    if (!open) return;
    setSelectedDay(null);
    setSelectedSlot(null);
    setAlternatives([]);
    setValidationState('idle');
    setStep(1);
    setReason('Solicitud del paciente');
    setObservation('');
    setShowTimeline(false);
    setMessage('');
  }, [open, appointment.id, appointment.turnoId]);

  const changeMonth = (direction) => {
    setView((current) => {
      const date = new Date(current.year, current.month - 1 + direction, 1);
      return { year: date.getFullYear(), month: date.getMonth() + 1 };
    });
    setSelectedDay(null);
    setSelectedSlot(null);
    setAlternatives([]);
    setValidationState('idle');
    setShowTimeline(false);
    setMessage('');
  };

  const chooseDay = async (day) => {
    const isDifferentDay = day?.fecha && day.fecha !== appointment.fecha;
    if (!isDifferentDay || !day?.seleccionable) {
      setSelectedDay(null);
      setSelectedSlot(null);
      setValidationState('idle');
      setMessage(!isDifferentDay ? 'Elegí un día diferente al actual.' : 'Ese día no está habilitado para recibir turnos. Elegí otro día.');
      return;
    }
    const sequence = ++validationSequence.current;
    const originalSlot = { horaInicio: startTime, horaFin: endTime };
    setSelectedDay(day);
    setSelectedSlot(null);
    setAlternatives([]);
    setValidationState('checking');
    setShowTimeline(false);
    setMessage('');
    try {
      await validateAppointmentReschedule(appointment.id ?? appointment.turnoId, {
        motivo: 'Validación previa',
        nuevoDiaAgendaId: day.diaAgendaId ?? day.id,
        nuevoInicio: localToInstant(day.fecha, startTime),
        nuevoFin: localToInstant(day.fecha, endTime),
      });
      if (sequence !== validationSequence.current) return;
      setSelectedSlot(originalSlot);
      setValidationState('available');
    } catch (error) {
      if (sequence !== validationSequence.current) return;
      let suggested = [];
      try {
        suggested = await listSuggestedTimes(day.fecha, appointment.tipoAtencionId ?? appointment.tipoAtencion?.id);
      } catch {
        suggested = [];
      }
      if (sequence !== validationSequence.current) return;
      setAlternatives((suggested || []).filter((slot) => !(slot.advertencias || []).length).slice(0, 3));
      setValidationState('unavailable');
      setMessage(error.message || 'El horario original no está disponible en ese día.');
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    if (step === 1) {
      if (selectedDay && selectedSlot) setStep(2);
      return;
    }
    if (!selectedDay || !selectedSlot) return;
    setMessage('');
    try {
      await onConfirm({
        diaAgendaId: selectedDay.diaAgendaId ?? selectedDay.id,
        fecha: selectedDay.fecha,
        nuevoInicio: localToInstant(selectedDay.fecha, selectedSlot.horaInicio),
        nuevoFin: localToInstant(selectedDay.fecha, selectedSlot.horaFin),
        motivo: observation.trim() ? `${reason}: ${observation.trim()}` : reason,
      });
    } catch (error) {
      setStep(1);
      setSelectedSlot(null);
      setValidationState('unavailable');
      setMessage(`${error.message || 'Ese horario dejó de estar disponible.'} Elegí una alternativa u otro día y confirmá nuevamente.`);
    }
  };

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title="Cambiar día del turno"
      description={step === 1 ? 'Elegí el nuevo día. Intentaremos conservar el horario original y ofreceremos alternativas si no está disponible.' : 'Revisá el cambio antes de confirmar la reprogramación.'}
      confirmLabel={step === 1 ? 'Continuar' : 'Confirmar reprogramación'}
      confirmDisabled={!selectedDay || !selectedSlot}
      loading={loading}
      onConfirm={submit}
      contentClassName="!w-[70vw] !max-w-[70vw] max-h-[95vh]"
    >
      <div className="space-y-4 py-2">
        <div className="grid gap-3 rounded-xl border bg-muted/20 p-4 sm:grid-cols-4">
          <DetailItem label="Paciente" value={`${appointment.cliente?.nombre || ''} ${appointment.cliente?.apellido || ''}`.trim()} />
          <DetailItem label="Fecha actual" value={appointment.fecha} />
          <DetailItem label="Horario" value={`${startTime}–${endTime}`} />
          <DetailItem label="Atención" value={appointment.tipoAtencion?.nombre || 'Configuración general'} />
        </div>
        {step === 1 ? <>
        <div className="flex items-center justify-between gap-2">
          <Button type="button" size="icon" variant="outline" aria-label="Mes anterior para reprogramar" onClick={() => changeMonth(-1)}><ChevronLeft /></Button>
          <span className="font-semibold">{view.month}/{view.year}</span>
          <Button type="button" size="icon" variant="outline" aria-label="Mes siguiente para reprogramar" onClick={() => changeMonth(1)}><ChevronRight /></Button>
        </div>
        <div className="[&_[role=gridcell]]:min-h-16 sm:[&_[role=gridcell]]:min-h-20">
          <MonthCalendar
            year={view.year}
            month={view.month}
            days={days.map((day) => ({ ...day, id: day.diaAgendaId ?? day.id, estadoActual: day.estadoActual ?? day.estado }))}
            selectedDayId={selectedDay?.diaAgendaId ?? selectedDay?.id}
            onSelectDay={chooseDay}
            loading={isLoading}
            showAvailabilitySummary
          />
        </div>
        {recommendedDays.length > 0 && <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Próximos días disponibles</p>
          <div className="flex flex-wrap gap-2">{recommendedDays.map((day) => <Button key={day.fecha} type="button" size="sm" variant="outline" onClick={() => chooseDay(day)}>{formatDateLong(day.fecha, timezone)}</Button>)}</div>
        </div>}
        {validationState === 'checking' && <p role="status" className="text-sm text-muted-foreground">Comprobando el horario original…</p>}
        {validationState === 'available' && <Alert className="border-emerald-500/40 bg-emerald-50 text-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-100"><CheckCircle2 /><AlertTitle>Horario disponible</AlertTitle><AlertDescription>Se puede conservar {startTime}–{endTime}. Continuá para revisar el cambio.</AlertDescription></Alert>}
        {validationState === 'unavailable' && <Alert variant="destructive"><AlertTriangle /><AlertTitle>El horario original no está disponible</AlertTitle><AlertDescription>{message} Seleccioná una alternativa o elegí otro día.</AlertDescription></Alert>}
        {alternatives.length > 0 && <div className="rounded-xl border p-4">
          <p className="font-semibold">Horarios recomendados</p>
          <div className="mt-3 flex flex-wrap gap-2">{alternatives.map((slot) => <Button key={`${slot.horaInicio}-${slot.horaFin}`} type="button" variant={selectedSlot?.horaInicio === String(slot.horaInicio).slice(0, 5) ? 'default' : 'outline'} onClick={() => setSelectedSlot({ horaInicio: String(slot.horaInicio).slice(0, 5), horaFin: String(slot.horaFin).slice(0, 5) })}>{String(slot.horaInicio).slice(0, 5)}–{String(slot.horaFin).slice(0, 5)}</Button>)}</div>
          <Button type="button" variant="ghost" className="mt-2" onClick={() => setShowTimeline((current) => !current)}>{showTimeline ? 'Ocultar cronograma completo' : 'Ver cronograma completo'}</Button>
        </div>}
        {showTimeline && selectedDayDetail && <DailyTimeline day={selectedDayDetail} timezone={timezone} appointments={selectedDayAppointments} candidateSlots={alternatives} selectedCandidate={selectedSlot} onSelectCandidate={(slot) => setSelectedSlot({ horaInicio: String(slot.horaInicio).slice(0, 5), horaFin: String(slot.horaFin).slice(0, 5) })} showIntegrationNotice={false} />}
        </> : <>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl border p-4"><p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Antes</p><p className="mt-2 font-semibold">{formatDateLong(appointment.fecha, timezone)}</p><p className="text-sm">{startTime}–{endTime}</p></div>
            <div className="rounded-xl border border-emerald-500/40 bg-emerald-50 p-4 dark:bg-emerald-950/30"><p className="text-xs font-semibold uppercase tracking-wide text-emerald-700 dark:text-emerald-300">Después</p><p className="mt-2 font-semibold">{formatDateLong(selectedDay.fecha, timezone)}</p><p className="text-sm">{selectedSlot.horaInicio}–{selectedSlot.horaFin}</p></div>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div><label htmlFor="reschedule-reason" className="text-sm font-semibold">Motivo</label><select id="reschedule-reason" className="mt-1 h-9 w-full rounded-lg border bg-background px-3 text-sm" value={reason} onChange={(event) => setReason(event.target.value)}><option>Solicitud del paciente</option><option>Cambio operativo</option><option>Reorganización de agenda</option><option>Otro</option></select></div>
            <div><label htmlFor="reschedule-observation" className="text-sm font-semibold">Observación opcional</label><Textarea id="reschedule-observation" value={observation} onChange={(event) => setObservation(event.target.value)} /></div>
          </div>
          <Button type="button" variant="ghost" onClick={() => setStep(1)}><ChevronLeft />Volver a elegir</Button>
          {message && <p role="alert" className="text-sm font-medium text-destructive">{message}</p>}
        </>}
      </div>
    </ConfirmDialog>
  );
}

function DetailItem({ label, value }) {
  return <div><p className="text-xs font-semibold text-muted-foreground">{label}</p><p className="mt-1 font-medium">{value || '—'}</p></div>;
}

function instantTimeLabel(value, timezone) {
  if (!value) return '';
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(value));
}

function localToInstant(date, time) {
  return new Date(`${date}T${time}:00`).toISOString();
}
