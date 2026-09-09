import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertTriangle, CalendarPlus, ChevronLeft, ChevronRight, Search } from 'lucide-react';
import { PageHeader } from '../components/PageHeader';
import { MonthCalendar } from '../components/MonthCalendar';
import { DailyTimeline } from '../components/DailyTimeline';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { TimeWheelPicker } from '@/components/ui/TimeWheelPicker';
import { MonthWheelPicker } from '@/components/ui/MonthWheelPicker';
import { useToast } from '@/components/ui/ToastProvider';
import { formatMonthYear, getMonthRange, getTodayInTimezone, parseDateString } from '@/utils/dates';
import { professionalContext } from '@/config/professional';
import { createManualAppointment, getAppointmentConfiguration, getAppointmentDay, listAppointmentDays, listSuggestedTimes, searchClients, validateManualAppointment } from '../api/appointmentApi';
import { useAssignedAppointments } from '../hooks/useAgenda';

const WARNING_LABELS = {
  HORARIO_FUERA_DE_BRECHA: 'El horario está fuera de las franjas horarias configuradas (brechas de atención).',
  CAPACIDAD_SUPERADA: 'Se superará la capacidad simultánea configurada.',
  DURACION_DIFERENTE_AL_TIPO_ATENCION: 'La duración difiere de la configurada para el tipo de atención.',
};
const time = (value) => String(value || '').slice(0, 5);
const instant = (date, hour) => new Date(`${date}T${hour}:00`).toISOString();

export function NewAppointmentPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { success, error: showError } = useToast();
  const today = getTodayInTimezone(professionalContext.timezone);
  const initial = parseDateString(params.get('fecha') || today);
  const [step, setStep] = useState(1);
  const [query, setQuery] = useState('');
  const [clients, setClients] = useState([]);
  const [client, setClient] = useState(null);
  const [days, setDays] = useState([]);
  const [candidateDay, setCandidateDay] = useState(null);
  const [selectedDay, setSelectedDay] = useState(null);
  const [dayDetail, setDayDetail] = useState(null);
  const [viewMonth, setViewMonth] = useState({ year: initial.year, month: initial.month });
  const [types, setTypes] = useState([]);
  const [typesLoaded, setTypesLoaded] = useState(false);
  const [typeId, setTypeId] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [notes, setNotes] = useState('');
  const [preview, setPreview] = useState(null);
  const [busy, setBusy] = useState(false);
  const [timelineZoom, setTimelineZoom] = useState(100);
  const [showOverlapping, setShowOverlapping] = useState(false);

  useEffect(() => {
    getAppointmentConfiguration().then((data) => {
      setTypes(data ? [{ id: 'configuracion', nombre: 'Configuración general', duracionMinutos: data.duracionAproximadaPorTurno, capacidadSimultanea: data.cantidadMaxTurnosALaVez }] : []);
      if (data) setTypeId('configuracion');
    }).catch((err) => showError('No se pudo cargar la configuración', err.message))
      .finally(() => setTypesLoaded(true));
  }, [showError]);

  useEffect(() => {
    const { firstDay, lastDay } = getMonthRange(viewMonth.year, viewMonth.month);
    listAppointmentDays(firstDay, lastDay).then((data) => setDays(data || []))
      .catch((err) => showError('No se pudo cargar el calendario', err.message));
  }, [viewMonth, showError]);

  useEffect(() => {
    if (!selectedDay || !typeId) return;
    listSuggestedTimes(selectedDay.fecha).then(setSuggestions).catch(() => setSuggestions([]));
  }, [selectedDay, typeId]);

  const selectedType = types.find((type) => String(type.id) === typeId);
  const date = selectedDay?.fecha || '';
  const { data: assignedAppointments = [] } = useAssignedAppointments(date || null, date || null);
  const payload = useMemo(() => ({
    diaAgendaId: selectedDay?.diaAgendaId ?? selectedDay?.id,
    fecha: date,
    clienteId: client?.id,
    tipoAtencionId: null,
    inicioEstimado: start ? instant(date, start) : null,
    finEstimado: end ? instant(date, end) : null,
    observaciones: notes,
  }), [selectedDay, date, client, typeId, start, end, notes]);

  async function findClients() {
    if (!query.trim()) return;
    setBusy(true);
    try { const data = await searchClients(query); setClients(data?.content || data || []); }
    catch (err) { showError('No se pudo buscar', err.message); }
    finally { setBusy(false); }
  }

  async function acceptDay(day = candidateDay) {
    if (!day?.seleccionable) return;
    setBusy(true);
    try {
      setDayDetail(await getAppointmentDay(day.diaAgendaId ?? day.id));
      const sameDay = String(selectedDay?.diaAgendaId ?? selectedDay?.id) === String(day.diaAgendaId ?? day.id);
      setSelectedDay(day);
      if (!sameDay) { setSuggestions([]); setStart(''); setEnd(''); }
      setPreview(null);
      setStep(3);
    } catch (err) { showError('No se pudo seleccionar el día', err.message); }
    finally { setBusy(false); }
  }

  function changeMonth(delta) {
    setViewMonth((current) => {
      const next = new Date(current.year, current.month - 1 + delta, 1);
      return { year: next.getFullYear(), month: next.getMonth() + 1 };
    });
    setCandidateDay(null);
  }

  function chooseSuggestion(slot) {
    setStart(time(slot.horaInicio)); setEnd(time(slot.horaFin)); setPreview(null);
  }

  const selectedSuggestion = suggestions.find((slot) => start === time(slot.horaInicio) && end === time(slot.horaFin)) || null;

  async function validate() {
    setBusy(true);
    try { setPreview(await validateManualAppointment(payload)); setStep(4); }
    catch (err) { showError('No se puede crear el turno', err.message); }
    finally { setBusy(false); }
  }

  async function confirm() {
    setBusy(true);
    try {
      const result = await createManualAppointment({ ...payload, tokenConfirmacion: preview?.tokenConfirmacion });
      if (!result?.creado) { setPreview(result); return; }
      success('Turno creado', 'El turno quedó asignado y la notificación fue registrada.');
      navigate(`/profesional/mi-dia?fecha=${date}`);
    } catch (err) { showError('No se pudo crear el turno', err.message); }
    finally { setBusy(false); }
  }

  return <div className="space-y-6">
    <PageHeader eyebrow="Operación" title="Nuevo turno" description="Alta manual de un turno para un cliente" />
    <div aria-label="Progreso del alta" className="grid gap-2 sm:grid-cols-4">
      {['Cliente', 'Día', 'Horario y atención', 'Confirmación'].map((label, index) => {
        const target = index + 1;
        return target < step
          ? <button type="button" key={label} className="rounded-lg border px-3 py-2 text-left text-sm transition hover:bg-muted" onClick={() => { if (target < 4) setPreview(null); setStep(target); }}>{target}. {label}</button>
          : <div key={label} className={`rounded-lg border px-3 py-2 text-sm ${step === target ? 'border-primary bg-primary/10 font-semibold' : ''}`}>{target}. {label}</div>;
      })}
    </div>

    {step === 1 && <Card><CardHeader><CardTitle>Buscar cliente</CardTitle></CardHeader><CardContent className="space-y-4">
      <div className="flex gap-2"><Input aria-label="Nombre, apellido o DNI" placeholder="Nombre, apellido o DNI" value={query} onChange={(e) => setQuery(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && findClients()} /><Button onClick={findClients} disabled={busy}><Search />Buscar</Button></div>
      <div className="grid gap-2">{clients.map((item) => <Button key={item.id} variant="outline" className="h-auto justify-start py-3" onClick={() => { setClient(item); setStep(2); }}>{item.nombre} {item.apellido} · {item.tipoDocumento} {item.numeroDocumento}</Button>)}</div>
    </CardContent></Card>}

    {step === 2 && <Card><CardHeader><div className="flex items-center justify-between gap-3"><div><CardTitle>Seleccionar día</CardTitle><p className="mt-1 text-sm text-muted-foreground">Un clic marca el día; doble clic lo selecciona directamente.</p></div><div className="flex items-center gap-1"><Button variant="outline" size="icon" aria-label="Mes anterior" onClick={() => changeMonth(-1)}><ChevronLeft /></Button><MonthWheelPicker aria-label="Seleccionar mes del turno" value={viewMonth.month} onChange={(month) => { setViewMonth((current) => ({ ...current, month })); setCandidateDay(null); }} triggerVariant="ios-compact" /><Button variant="outline" size="icon" aria-label="Mes siguiente" onClick={() => changeMonth(1)}><ChevronRight /></Button></div></div></CardHeader><CardContent className="space-y-4">
      <div>
        <MonthCalendar year={viewMonth.year} month={viewMonth.month} days={days.map((day) => ({ ...day, id: day.diaAgendaId ?? day.id, estadoActual: day.estado }))} selectedDayId={candidateDay?.diaAgendaId ?? candidateDay?.id} onSelectDay={setCandidateDay} onDoubleClickDay={acceptDay} />
      </div>
      {candidateDay && !candidateDay.seleccionable && <p role="alert" className="text-sm font-medium text-destructive">{candidateDay.mensaje || 'Día no seleccionable'}</p>}
      <div className="flex justify-between"><Button variant="outline" onClick={() => setStep(1)}><ChevronLeft />Volver al cliente</Button><Button disabled={!candidateDay?.seleccionable || busy} onClick={() => acceptDay()}>Seleccionar día</Button></div>
    </CardContent></Card>}

    {step === 3 && <Card><CardHeader><div className="flex items-center justify-between"><CardTitle>Horario según configuración</CardTitle><Button variant="outline" onClick={() => { setPreview(null); setStep(2); }}><ChevronLeft />Volver al día</Button></div></CardHeader><CardContent className="grid gap-6 lg:grid-cols-3">
      <section data-testid="appointment-availability-column" className="space-y-4 lg:col-span-1" aria-label="Disponibilidad del día">
        <DailyTimeline day={dayDetail} timezone={professionalContext.timezone} showIntegrationNotice={false} appointments={assignedAppointments} candidateSlots={showOverlapping ? suggestions : suggestions.filter((slot) => !(slot.advertencias || []).includes('CAPACIDAD_SUPERADA'))} selectedCandidate={selectedSuggestion} onSelectCandidate={chooseSuggestion} zoom={timelineZoom} onZoomChange={setTimelineZoom} />
        {!selectedType && <p className="rounded-xl border p-4 text-sm text-muted-foreground">Configurá la duración aproximada y la capacidad para calcular candidatos.</p>}
      </section>
      <section className="space-y-4 rounded-xl border bg-muted/15 p-4 lg:col-span-2" aria-label="Horario estimado">
        <div className="flex items-center justify-between gap-3 rounded-xl border bg-background p-3">
          <Label htmlFor="show-overlapping" className="cursor-pointer text-sm">Mostrar turnos con superposición</Label>
          <Switch id="show-overlapping" checked={showOverlapping} onCheckedChange={setShowOverlapping} />
        </div>
        <div><p className="mb-2 font-semibold">Configuración de turnos</p>{selectedType && <div className="rounded-lg border bg-background p-3"><span className="font-semibold">Duración aproximada: {selectedType.duracionMinutos} min</span><span className="ml-4">Capacidad simultánea: {selectedType.capacidadSimultanea}</span></div>}</div>
        {typesLoaded && types.length === 0 && <Alert variant="destructive"><AlertTriangle /><AlertTitle>No hay configuración profesional</AlertTitle><AlertDescription>Definí la duración aproximada y la cantidad máxima de turnos simultáneos antes de registrar turnos.</AlertDescription></Alert>}
        <div><p className="font-semibold">Inicio y fin estimado</p><p className="text-sm text-muted-foreground">Elegí una opción o ingresá el horario manualmente.</p></div>
        <div className="grid gap-3 sm:grid-cols-2"><div><Label htmlFor="start">Hora de inicio</Label><TimeWheelPicker id="start" aria-label="Hora de inicio" value={start} minuteStep={5} onChange={(value) => { setStart(value); setPreview(null); }} /></div><div><Label htmlFor="end">Hora de fin</Label><TimeWheelPicker id="end" aria-label="Hora de fin" value={end} minuteStep={5} onChange={(value) => { setEnd(value); setPreview(null); }} /></div></div>
        <div><Label htmlFor="notes">Observaciones</Label><Textarea id="notes" value={notes} onChange={(e) => { setNotes(e.target.value); setPreview(null); }} /></div>
        {selectedType && <div className="rounded-lg border p-3 text-sm"><p className="font-semibold">{selectedType.nombre}</p><p>{selectedType.duracionMinutos} minutos · Capacidad simultánea: {selectedType.capacidadSimultanea}</p></div>}
        <Alert><AlertTriangle /><AlertTitle>Validación de agenda</AlertTitle><AlertDescription>Verificaremos brechas, excepciones, duración y capacidad. Los horarios extraordinarios requieren aceptar sus avisos.</AlertDescription></Alert>
        <Button disabled={!typeId || !start || !end || start >= end || busy} onClick={validate}>Revisar avisos y confirmar</Button>
      </section>
    </CardContent></Card>}

    {step === 4 && preview && <Card><CardHeader><CardTitle>Confirmar turno</CardTitle></CardHeader><CardContent className="space-y-4">
      <div className="rounded-xl border p-4"><p className="font-semibold">{preview.cliente?.nombre} {preview.cliente?.apellido}</p><p>{preview.cliente?.tipoDocumento} {preview.cliente?.numeroDocumento}</p><p>{date} · {start}–{end}</p><p>{preview.tipoAtencion?.nombre || selectedType?.nombre}</p></div>
      {(preview.advertencias || []).map((warning) => <Alert key={warning} variant="destructive"><AlertTriangle /><AlertTitle>Advertencia</AlertTitle><AlertDescription>{WARNING_LABELS[warning] || warning}</AlertDescription></Alert>)}
      {!preview.advertencias?.length && <Alert><CalendarPlus /><AlertTitle>Agenda validada</AlertTitle><AlertDescription>El horario respeta la agenda y la capacidad configurada.</AlertDescription></Alert>}
      <div className="flex gap-2"><Button aria-label="Volver al horario" variant="outline" onClick={() => { setPreview(null); setStep(3); }}><ChevronLeft />Modificar</Button><Button onClick={confirm} disabled={busy}><CalendarPlus />{busy ? 'Creando...' : 'Confirmar y crear'}</Button></div>
    </CardContent></Card>}
  </div>;
}
