import { useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CheckCircle2, ChevronLeft, ChevronRight } from 'lucide-react';
import { PageHeader } from '../components/PageHeader';
import { MonthCalendar } from '../components/MonthCalendar';
import { DailyTimeline } from '../components/DailyTimeline';
import { useAssignedAppointments, useDayDetail, useRescheduleAppointment, useSelectableDays } from '../hooks/useAgenda';
import { validateAppointmentReschedule } from '../api/agendaApi';
import { listSuggestedTimes } from '../api/appointmentApi';
import { professionalContext } from '../../../config/professional';
import { formatDateLong, getMonthRange, parseDateString } from '../../../utils/dates';
import { useToast } from '../../../components/ui/ToastProvider';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Textarea } from '@/components/ui/textarea';

export function RescheduleAppointmentPage() {
  const { appointmentId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { success } = useToast();
  const timezone = professionalContext.timezone;
  const originalDate = searchParams.get('fecha');
  const { data: appointments = [], isLoading } = useAssignedAppointments(originalDate, originalDate);
  const appointment = appointments.find((item) => String(item.id ?? item.turnoId) === String(appointmentId));
  const reschedule = useRescheduleAppointment();

  if (isLoading) return <div className="rounded-xl border bg-card p-8 text-sm text-muted-foreground">Cargando turno…</div>;
  if (!appointment) return <EmptyState title="No se encontró el turno" description="El turno ya no está asignado o la fecha original no es válida." action={{ label: 'Volver a Mi día', onClick: () => navigate(`/profesional/mi-dia${originalDate ? `?fecha=${originalDate}` : ''}`) }} />;

  const confirm = async (target) => {
    await reschedule.mutateAsync({ appointmentId, ...target });
    success('Turno reprogramado', 'El nuevo día y horario quedaron confirmados y se generó la notificación.');
    navigate(`/profesional/mi-dia?fecha=${target.fecha}&turno=${appointmentId}`);
  };

  return <RescheduleWorkspace appointment={appointment} timezone={timezone} loading={reschedule.isPending} onConfirm={confirm} onBack={() => navigate(`/profesional/mi-dia?fecha=${originalDate}&turno=${appointmentId}`)} />;
}

function RescheduleWorkspace({ appointment, timezone, loading, onConfirm, onBack }) {
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
  const { data: days = [], isLoading: loadingDays } = useSelectableDays(firstDay, lastDay);
  const startTime = instantTimeLabel(appointment.inicioEstimado, timezone);
  const endTime = instantTimeLabel(appointment.finEstimado, timezone);
  const { data: selectedDayDetail } = useDayDetail(showTimeline ? selectedDay?.diaAgendaId ?? selectedDay?.id : null);
  const { data: selectedDayAppointments = [] } = useAssignedAppointments(showTimeline ? selectedDay?.fecha : null, showTimeline ? selectedDay?.fecha : null);
  const recommendedDays = days.filter((day) => day.seleccionable && day.fecha !== appointment.fecha).sort((a, b) => a.fecha.localeCompare(b.fecha)).slice(0, 3);

  const changeMonth = (direction) => {
    setView((current) => {
      const date = new Date(current.year, current.month - 1 + direction, 1);
      return { year: date.getFullYear(), month: date.getMonth() + 1 };
    });
    setSelectedDay(null); setSelectedSlot(null); setAlternatives([]); setValidationState('idle'); setShowTimeline(false); setMessage('');
  };

  const chooseDay = async (day) => {
    const different = day?.fecha && day.fecha !== appointment.fecha;
    if (!different || !day?.seleccionable) {
      setSelectedDay(null); setSelectedSlot(null); setValidationState('idle');
      setMessage(!different ? 'Elegí un día diferente al actual.' : 'Ese día no está habilitado para recibir turnos.');
      return;
    }
    const sequence = ++validationSequence.current;
    setSelectedDay(day); setSelectedSlot(null); setAlternatives([]); setValidationState('checking'); setShowTimeline(false); setMessage('');
    try {
      await validateAppointmentReschedule(appointment.id ?? appointment.turnoId, {
        motivo: 'Validación previa', nuevoDiaAgendaId: day.diaAgendaId ?? day.id,
        nuevoInicio: localToInstant(day.fecha, startTime), nuevoFin: localToInstant(day.fecha, endTime),
      });
      if (sequence !== validationSequence.current) return;
      setSelectedSlot({ horaInicio: startTime, horaFin: endTime });
      setValidationState('available');
    } catch (error) {
      if (sequence !== validationSequence.current) return;
      let suggested = [];
      try { suggested = await listSuggestedTimes(day.fecha, appointment.tipoAtencionId ?? appointment.tipoAtencion?.id); } catch { suggested = []; }
      if (sequence !== validationSequence.current) return;
      setAlternatives((suggested || []).filter((slot) => !(slot.advertencias || []).length).slice(0, 3));
      setValidationState('unavailable');
      setMessage(error.message || 'El horario original no está disponible en ese día.');
    }
  };

  const submit = async () => {
    if (step === 1) { if (selectedDay && selectedSlot) setStep(2); return; }
    setMessage('');
    try {
      await onConfirm({
        nuevoDiaAgendaId: selectedDay.diaAgendaId ?? selectedDay.id,
        fecha: selectedDay.fecha,
        nuevoInicio: localToInstant(selectedDay.fecha, selectedSlot.horaInicio),
        nuevoFin: localToInstant(selectedDay.fecha, selectedSlot.horaFin),
        motivo: observation.trim() ? `${reason}: ${observation.trim()}` : reason,
      });
    } catch (error) {
      setStep(1); setSelectedSlot(null); setValidationState('unavailable');
      setMessage(`${error.message || 'Ese horario dejó de estar disponible.'} Elegí una alternativa u otro día.`);
    }
  };

  const clientName = `${appointment.cliente?.nombre || ''} ${appointment.cliente?.apellido || ''}`.trim();
  return <div className="mx-auto w-full max-w-[96rem] space-y-6">
    <PageHeader eyebrow="Reprogramación de turno" title="Cambiar día" description="Elegí una nueva fecha y confirmá el cambio." actions={<Button variant="outline" onClick={onBack}><ArrowLeft />Volver al turno</Button>} />
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
      <Card>
        <CardHeader><CardTitle>{step === 1 ? '1. Elegir disponibilidad' : '2. Revisar y confirmar'}</CardTitle></CardHeader>
        <CardContent className="space-y-5">
          {step === 1 ? <>
            <div className="flex items-center justify-between gap-3"><Button size="icon" variant="outline" aria-label="Mes anterior para reprogramar" onClick={() => changeMonth(-1)}><ChevronLeft /></Button><strong>{view.month}/{view.year}</strong><Button size="icon" variant="outline" aria-label="Mes siguiente para reprogramar" onClick={() => changeMonth(1)}><ChevronRight /></Button></div>
            <div className="[&_[role=gridcell]]:min-h-20"><MonthCalendar year={view.year} month={view.month} days={days.map((day) => ({ ...day, id: day.diaAgendaId ?? day.id, estadoActual: day.estadoActual ?? day.estado }))} selectedDayId={selectedDay?.diaAgendaId ?? selectedDay?.id} onSelectDay={chooseDay} loading={loadingDays} showAvailabilitySummary /></div>
            {recommendedDays.length > 0 && <div><p className="mb-2 font-semibold">Próximos días disponibles</p><div className="flex flex-wrap gap-2">{recommendedDays.map((day) => <Button key={day.fecha} size="sm" variant="outline" onClick={() => chooseDay(day)}>{formatDateLong(day.fecha, timezone)}</Button>)}</div></div>}
            {validationState === 'checking' && <p role="status" className="text-muted-foreground">Comprobando el horario original…</p>}
            {validationState === 'available' && <Alert className="border-emerald-500/40 bg-emerald-50 dark:bg-emerald-950/30"><CheckCircle2 /><AlertTitle>Horario disponible</AlertTitle><AlertDescription>Se puede conservar {startTime}–{endTime}.</AlertDescription></Alert>}
            {validationState === 'unavailable' && <Alert variant="destructive"><AlertTriangle /><AlertTitle>Horario no disponible</AlertTitle><AlertDescription>{message} Elegí una alternativa u otro día.</AlertDescription></Alert>}
            {alternatives.length > 0 && <div className="rounded-xl border p-4"><p className="font-semibold">Horarios recomendados</p><div className="mt-3 flex flex-wrap gap-2">{alternatives.map((slot) => { const start=String(slot.horaInicio).slice(0,5), end=String(slot.horaFin).slice(0,5); return <Button key={`${start}-${end}`} variant={selectedSlot?.horaInicio === start ? 'default' : 'outline'} onClick={() => setSelectedSlot({ horaInicio:start, horaFin:end })}>{start}–{end}</Button>; })}</div><Button variant="ghost" className="mt-2" onClick={() => setShowTimeline((value) => !value)}>{showTimeline ? 'Ocultar cronograma completo' : 'Ver cronograma completo'}</Button></div>}
            {showTimeline && selectedDayDetail && <DailyTimeline day={selectedDayDetail} timezone={timezone} appointments={selectedDayAppointments} candidateSlots={alternatives} selectedCandidate={selectedSlot} onSelectCandidate={(slot) => setSelectedSlot({ horaInicio:String(slot.horaInicio).slice(0,5), horaFin:String(slot.horaFin).slice(0,5) })} showIntegrationNotice={false} />}
          </> : <>
            <div className="grid gap-4 md:grid-cols-2"><Comparison title="Antes" date={appointment.fecha} start={startTime} end={endTime} timezone={timezone} /><Comparison title="Después" date={selectedDay.fecha} start={selectedSlot.horaInicio} end={selectedSlot.horaFin} timezone={timezone} highlighted /></div>
            <div className="grid gap-4 md:grid-cols-2"><div><label htmlFor="reschedule-reason" className="font-semibold">Motivo</label><select id="reschedule-reason" className="mt-2 h-10 w-full rounded-lg border bg-background px-3" value={reason} onChange={(event) => setReason(event.target.value)}><option>Solicitud del paciente</option><option>Cambio operativo</option><option>Reorganización de agenda</option><option>Otro</option></select></div><div><label htmlFor="reschedule-observation" className="font-semibold">Observación opcional</label><Textarea id="reschedule-observation" className="mt-2" value={observation} onChange={(event) => setObservation(event.target.value)} /></div></div>
            <Button variant="ghost" onClick={() => setStep(1)}><ChevronLeft />Volver a elegir</Button>
          </>}
          <div className="flex justify-end border-t pt-4"><Button disabled={!selectedDay || !selectedSlot || loading} onClick={submit}>{loading ? 'Confirmando…' : step === 1 ? 'Continuar' : 'Confirmar reprogramación'}</Button></div>
        </CardContent>
      </Card>
      <Card className="h-fit xl:sticky xl:top-4"><CardHeader><CardTitle>Turno actual</CardTitle></CardHeader><CardContent className="space-y-4"><Detail label="Paciente" value={clientName} /><Detail label="Fecha" value={formatDateLong(appointment.fecha, timezone)} /><Detail label="Horario" value={`${startTime}–${endTime}`} /><Detail label="Atención" value={appointment.tipoAtencion?.nombre || 'Configuración general'} /><Detail label="Estado" value={appointment.estado || 'ASIGNADO'} /></CardContent></Card>
    </div>
  </div>;
}

function Detail({ label, value }) { return <div><p className="text-sm font-semibold text-muted-foreground">{label}</p><p className="mt-1 text-base font-medium">{value || '—'}</p></div>; }
function Comparison({ title, date, start, end, timezone, highlighted=false }) { return <div className={`rounded-xl border p-5 ${highlighted ? 'border-emerald-500/40 bg-emerald-50 dark:bg-emerald-950/30' : ''}`}><p className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">{title}</p><p className="mt-3 text-lg font-semibold">{formatDateLong(date, timezone)}</p><p>{start}–{end}</p></div>; }
function instantTimeLabel(value, timezone) { return value ? new Intl.DateTimeFormat('en-GB', { timeZone: timezone, hour:'2-digit', minute:'2-digit', hourCycle:'h23' }).format(new Date(value)) : ''; }
function localToInstant(date, time) { return new Date(`${date}T${time}:00`).toISOString(); }
