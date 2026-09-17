import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CalendarClock, CheckCircle2, ChevronLeft, ChevronRight } from 'lucide-react';
import { PageHeader } from '../components/PageHeader';
import { MonthCalendar } from '../components/MonthCalendar';
import { DailyTimeline } from '../components/DailyTimeline';
import { useAssignedAppointments, useDayDetail, useRescheduleAppointment, useSelectableDays } from '../hooks/useAgenda';
import { useAffectedAppointments, useResolveAffectedReschedule } from '../hooks/useAbsences';
import { affectedAppointmentToAppointment } from '../utils/affectedAppointmentAdapter';
import { validateAppointmentReschedule } from '../api/agendaApi';
import { listSuggestedTimes } from '../api/appointmentApi';
import { professionalContext } from '../../../config/professional';
import { formatDateLong, getMonthRange, parseDateString } from '../../../utils/dates';
import { useToast } from '../../../components/ui/ToastProvider';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { TimeWheelPicker } from '@/components/ui/TimeWheelPicker';
import { MonthWheelPicker } from '@/components/ui/MonthWheelPicker';

const time = (value) => String(value || '').slice(0, 5);
const localToInstant = (date, hour) => new Date(`${date}T${hour}:00`).toISOString();
const NO_UNAVAILABLE_DATES = [];

export function CompleteRescheduleAppointmentPage() {
  const { appointmentId } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { success, error: showError } = useToast();
  const originalDate = params.get('fecha');
  const affectedAppointmentId = params.get('afectacionId');
  const { data: appointments = [], isLoading } = useAssignedAppointments(originalDate, originalDate);
  const { data: affectedAppointments = [], isLoading: affectedLoading } = useAffectedAppointments();
  const affectedAppointment = affectedAppointmentId
    ? affectedAppointments.find((item) => String(item.afectacionId) === String(affectedAppointmentId))
    : null;
  const appointment = affectedAppointmentToAppointment(affectedAppointment)
    || appointments.find((item) => String(item.id ?? item.turnoId) === String(appointmentId));
  const reschedule = useRescheduleAppointment();
  const resolveAffected = useResolveAffectedReschedule();

  if (isLoading || (affectedAppointmentId && affectedLoading)) return <div className="rounded-xl border bg-card p-8 text-sm text-muted-foreground">Cargando turno…</div>;
  if (!appointment) return <EmptyState title="No se encontró el turno" description="No pudimos recuperar los datos del turno o de su afectación." action={{ label: 'Volver a Turnos afectados', onClick: () => navigate('/profesional/turnos-afectados') }} />;

  const confirm = async (target) => {
    try {
      if (affectedAppointmentId) {
        await resolveAffected.mutateAsync({
          id: affectedAppointmentId,
          nuevoDiaAgendaId: target.nuevoDiaAgendaId,
          nuevoInicio: target.nuevoInicio,
          nuevoFin: target.nuevoFin,
          observacion: target.motivo,
        });
      } else {
        await reschedule.mutateAsync({ appointmentId, ...target });
      }
      success('Turno reprogramado', 'La nueva fecha y el nuevo horario quedaron confirmados.');
      navigate(`/profesional/mi-dia?fecha=${target.fecha}&turno=${appointmentId}`);
    } catch (error) {
      showError('No se pudo reprogramar el turno', error.message);
      throw error;
    }
  };

  return <CompleteRescheduleWorkspace appointment={appointment} timezone={professionalContext.timezone} loading={reschedule.isPending || resolveAffected.isPending} onConfirm={confirm} onBack={() => navigate(`/profesional/mi-dia?fecha=${originalDate}&turno=${appointmentId}`)} />;
}

export function CompleteRescheduleWorkspace({ appointment, timezone, unavailableDates = NO_UNAVAILABLE_DATES, loading, onConfirm, onBack }) {
  const initial = parseDateString(appointment.fecha);
  const [step, setStep] = useState(1);
  const [view, setView] = useState({ year: initial.year, month: initial.month });
  const [candidateDay, setCandidateDay] = useState(null);
  const [selectedDay, setSelectedDay] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [reason, setReason] = useState('Solicitud del paciente');
  const [observation, setObservation] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [timelineZoom, setTimelineZoom] = useState(100);
  const { firstDay, lastDay } = getMonthRange(view.year, view.month);
  const { data: days = [], isLoading: loadingDays } = useSelectableDays(firstDay, lastDay);
  const unavailableDateSet = useMemo(() => new Set(unavailableDates), [unavailableDates]);
  const { data: dayDetail } = useDayDetail(selectedDay?.diaAgendaId ?? selectedDay?.id);
  const { data: assignedAppointments = [] } = useAssignedAppointments(selectedDay?.fecha, selectedDay?.fecha);

  useEffect(() => {
    if (!selectedDay?.fecha) return;
    let active = true;
    setBusy(true);
    listSuggestedTimes(selectedDay.fecha, appointment.tipoAtencionId ?? appointment.tipoAtencion?.id)
      .then((items) => { if (active) setSuggestions((items || []).filter((slot) => !(slot.advertencias || []).length)); })
      .catch(() => { if (active) setSuggestions([]); })
      .finally(() => { if (active) setBusy(false); });
    return () => { active = false; };
  }, [appointment.tipoAtencion?.id, appointment.tipoAtencionId, selectedDay]);

  const calendarDays = days.map((day) => {
    const blockedByPendingException = unavailableDateSet.has(day.fecha);
    return {
      ...day,
      id: day.diaAgendaId ?? day.id,
      estadoActual: day.estadoActual ?? day.estado,
      seleccionable: day.seleccionable && !blockedByPendingException,
      mensaje: blockedByPendingException
        ? 'Este día quedará afectado por la excepción que estás registrando.'
        : day.mensaje,
    };
  });
  const selectedSuggestion = suggestions.find((slot) => start === time(slot.horaInicio) && end === time(slot.horaFin));
  const client = appointment.cliente || {};
  const clientName = `${client.nombre || ''} ${client.apellido || ''}`.trim() || 'Sin nombre informado';
  const originalStart = instantTimeLabel(appointment.inicioEstimado, timezone);
  const originalEnd = instantTimeLabel(appointment.finEstimado, timezone);

  const changeMonth = (delta) => {
    setView((current) => {
      const next = new Date(current.year, current.month - 1 + delta, 1);
      return { year: next.getFullYear(), month: next.getMonth() + 1 };
    });
    setCandidateDay(null);
  };

  const acceptDay = (day = candidateDay) => {
    if (!day?.seleccionable) return;
    setSelectedDay(day);
    setStart(''); setEnd(''); setSuggestions([]); setMessage('');
    setStep(2);
  };

  const handleDayClick = (day) => {
    const currentId = candidateDay?.diaAgendaId ?? candidateDay?.id;
    const clickedId = day?.diaAgendaId ?? day?.id;
    if (currentId != null && String(currentId) === String(clickedId)) {
      acceptDay(day);
      return;
    }
    setCandidateDay(day);
  };

  const chooseSlot = (slot) => {
    setStart(time(slot.horaInicio));
    setEnd(time(slot.horaFin));
    setMessage('');
  };

  const review = async () => {
    setBusy(true); setMessage('');
    try {
      await validateAppointmentReschedule(appointment.id ?? appointment.turnoId, {
        motivo: 'Validación previa',
        nuevoDiaAgendaId: selectedDay.diaAgendaId ?? selectedDay.id,
        nuevoInicio: localToInstant(selectedDay.fecha, start),
        nuevoFin: localToInstant(selectedDay.fecha, end),
      });
      setStep(3);
    } catch (error) {
      setMessage(error.message || 'El horario elegido no está disponible. Elegí otro horario.');
    } finally { setBusy(false); }
  };

  const confirm = async () => {
    setBusy(true); setMessage('');
    try {
      await onConfirm({
        nuevoDiaAgendaId: selectedDay.diaAgendaId ?? selectedDay.id,
        fecha: selectedDay.fecha,
        nuevoInicio: localToInstant(selectedDay.fecha, start),
        nuevoFin: localToInstant(selectedDay.fecha, end),
        motivo: observation.trim() ? `${reason}: ${observation.trim()}` : reason,
      });
    } catch (error) {
      setStep(2);
      setMessage(`${error.message || 'Ese horario dejó de estar disponible.'} Elegí otro horario.`);
    } finally { setBusy(false); }
  };

  return <div className="mx-auto w-full max-w-[96rem] space-y-6">
    <PageHeader eyebrow="Operación" title="Reprogramar turno" description="Elegí una nueva fecha y un nuevo horario para el cliente asignado." actions={<Button variant="outline" onClick={onBack}><ArrowLeft />Volver al turno</Button>} />
    <div aria-label="Progreso de la reprogramación" className="grid gap-2 sm:grid-cols-3">
      {['Día', 'Cliente y horario', 'Confirmación'].map((label, index) => <div key={label} className={`rounded-lg border px-3 py-2 text-sm ${step === index + 1 ? 'border-primary bg-primary/10 font-semibold' : ''}`}>{index + 1}. {label}</div>)}
    </div>

    {step === 1 && <Card><CardHeader><CardTitle>Seleccionar día</CardTitle></CardHeader><CardContent className="space-y-4">
      <div className="flex items-center justify-between gap-3"><Button variant="outline" size="icon" aria-label="Mes anterior" onClick={() => changeMonth(-1)}><ChevronLeft /></Button><MonthWheelPicker aria-label="Seleccionar mes para reprogramar" value={view.month} onChange={(month) => { setView((current) => ({ ...current, month })); setCandidateDay(null); }} triggerVariant="ios-compact" /><Button variant="outline" size="icon" aria-label="Mes siguiente" onClick={() => changeMonth(1)}><ChevronRight /></Button></div>
      <MonthCalendar year={view.year} month={view.month} days={calendarDays} selectedDayId={candidateDay?.diaAgendaId ?? candidateDay?.id} onSelectDay={handleDayClick} onDoubleClickDay={acceptDay} loading={loadingDays} disableUnselectable />
      {candidateDay && !candidateDay.seleccionable && <p role="alert" className="text-sm font-medium text-destructive">{candidateDay.mensaje || 'Día no seleccionable'}</p>}
      <div className="flex justify-end"><Button disabled={!candidateDay?.seleccionable} onClick={() => acceptDay()}>Seleccionar día</Button></div>
    </CardContent></Card>}

    {step === 2 && <Card><CardHeader><div className="flex items-center justify-between gap-3"><CardTitle>Cliente y nuevo horario</CardTitle><Button variant="outline" onClick={() => setStep(1)}><ChevronLeft />Cambiar día</Button></div></CardHeader><CardContent className="grid gap-6 lg:grid-cols-3">
      <section className="space-y-4 lg:col-span-1" aria-label="Turnos disponibles"><DailyTimeline day={dayDetail} timezone={timezone} appointments={assignedAppointments} candidateSlots={suggestions} selectedCandidate={selectedSuggestion} onSelectCandidate={chooseSlot} zoom={timelineZoom} onZoomChange={setTimelineZoom} showIntegrationNotice={false} /></section>
      <section className="space-y-5 rounded-xl border bg-muted/15 p-4 lg:col-span-2" aria-label="Datos de la reprogramación">
        <div className="rounded-xl border bg-background p-4"><p className="text-xs font-bold uppercase tracking-wide text-info">Cliente asignado</p><p className="mt-2 text-lg font-semibold">{clientName}</p><p className="text-sm text-muted-foreground">{[client.tipoDocumento, client.numeroDocumento].filter(Boolean).join(' ') || 'Documento no informado'}</p></div>
        <div><p className="font-semibold">Turnos disponibles</p><p className="text-sm text-muted-foreground">Seleccioná un turno del cronograma o ingresá el horario manualmente.</p><div className="mt-3 flex flex-wrap gap-2">{suggestions.map((slot) => <Button key={`${slot.horaInicio}-${slot.horaFin}`} variant={selectedSuggestion === slot ? 'default' : 'outline'} onClick={() => chooseSlot(slot)}>{time(slot.horaInicio)}–{time(slot.horaFin)}</Button>)}</div></div>
        {!busy && suggestions.length === 0 && <Alert><CalendarClock /><AlertTitle>Sin turnos sugeridos</AlertTitle><AlertDescription>Podés ingresar un horario manual para comprobar su disponibilidad.</AlertDescription></Alert>}
        <div className="grid gap-3 sm:grid-cols-2"><div><Label htmlFor="reschedule-start">Hora de inicio</Label><TimeWheelPicker id="reschedule-start" aria-label="Hora de inicio" value={start} minuteStep={5} onChange={(value) => { setStart(value); setMessage(''); }} /></div><div><Label htmlFor="reschedule-end">Hora de fin</Label><TimeWheelPicker id="reschedule-end" aria-label="Hora de fin" value={end} minuteStep={5} onChange={(value) => { setEnd(value); setMessage(''); }} /></div></div>
        {message && <Alert variant="destructive"><AlertTriangle /><AlertTitle>Horario no disponible</AlertTitle><AlertDescription>{message}</AlertDescription></Alert>}
        <Button disabled={!start || !end || start >= end || busy} onClick={review}>{busy ? 'Validando…' : 'Continuar a confirmación'}</Button>
      </section>
    </CardContent></Card>}

    {step === 3 && <Card><CardHeader><CardTitle role="heading" aria-level={2}>Confirmar reprogramación</CardTitle></CardHeader><CardContent className="space-y-5">
      <div className="grid gap-4 md:grid-cols-2"><Comparison title="Turno actual" value={`${appointment.fecha} · ${originalStart}–${originalEnd}`} /><Comparison title="Turno reprogramado" value={`${selectedDay.fecha} · ${start}–${end}`} highlighted /></div>
      <div className="rounded-xl border p-4"><p className="font-semibold">{clientName}</p><p className="text-sm text-muted-foreground">{[client.tipoDocumento, client.numeroDocumento].filter(Boolean).join(' ')}</p><p className="mt-2 text-sm">{appointment.tipoAtencion?.nombre || 'Configuración general'}</p></div>
      <Alert className="border-emerald-500/40 bg-emerald-50 dark:bg-emerald-950/30"><CheckCircle2 /><AlertTitle>Horario validado</AlertTitle><AlertDescription>La agenda y la capacidad permiten realizar la reprogramación.</AlertDescription></Alert>
      <div className="grid gap-4 md:grid-cols-2"><div><Label htmlFor="complete-reschedule-reason">Motivo</Label><select id="complete-reschedule-reason" className="mt-2 h-10 w-full rounded-lg border bg-background px-3" value={reason} onChange={(event) => setReason(event.target.value)}><option>Solicitud del paciente</option><option>Cambio operativo</option><option>Reorganización de agenda</option><option>Otro</option></select></div><div><Label htmlFor="complete-reschedule-observation">Observación opcional</Label><Textarea id="complete-reschedule-observation" className="mt-2" value={observation} onChange={(event) => setObservation(event.target.value)} /></div></div>
      <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={() => setStep(2)}><ChevronLeft />Modificar horario</Button><Button disabled={loading || busy} onClick={confirm}>{loading || busy ? 'Reprogramando…' : 'Confirmar reprogramación'}</Button></div>
    </CardContent></Card>}
  </div>;
}

function Comparison({ title, value, highlighted = false }) { return <div className={`rounded-xl border p-5 ${highlighted ? 'border-emerald-500/40 bg-emerald-50 dark:bg-emerald-950/30' : ''}`}><p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{title}</p><p className="mt-3 font-semibold">{value}</p></div>; }
function instantTimeLabel(value, timezone) { return value ? new Intl.DateTimeFormat('en-GB', { timeZone: timezone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(value)) : ''; }
