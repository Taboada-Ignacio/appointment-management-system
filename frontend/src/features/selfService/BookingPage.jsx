import { useCallback, useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, CalendarDays, CheckCircle2, LoaderCircle, Phone, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Field, FieldGroup, FieldLabel, FieldDescription, FieldError } from '@/components/ui/field';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { MonthCalendar } from '@/features/professional/components/MonthCalendar';
import { DailyTimeline } from '@/features/professional/components/DailyTimeline';
import { cn } from '@/lib/utils';
import { selfServiceApi } from './api';
import { normalizeClientName, reservationKey } from './registration';
import { readBooking, saveBooking, clearBooking, dateKey, localDate, professionalToday, friendlyDate } from './session';

const steps = ['Identificación', 'Datos', 'Fecha', 'Horario', 'Confirmación'];
const stepNames = ['identificacion', 'datos', 'fecha', 'horario', 'confirmacion'];
const timeLabel = value => value?.slice(0, 5);

function FormInput({ id, label, error, description, ...props }) {
  return <Field data-invalid={Boolean(error)}><FieldLabel htmlFor={id}>{label}</FieldLabel>
    <Input id={id} aria-invalid={Boolean(error)} aria-describedby={description || error ? `${id}-help` : undefined} {...props} />
    {error ? <FieldError id={`${id}-help`}>{error}</FieldError> : description && <FieldDescription id={`${id}-help`}>{description}</FieldDescription>}
  </Field>;
}

function ContactProfessional({ professional }) {
  const phone = professional?.telefono?.replace(/[^0-9+]/g, '');
  return phone ? <Button variant="outline" asChild><a href={`tel:${phone}`}><Phone data-icon="inline-start" />Contactar al profesional</a></Button> : null;
}

export function BookingPage() {
  const [session, setSession] = useState('');
  const [snapshot, setSnapshot] = useState(null);
  const [step, setStep] = useState('identificacion');
  const [draft, setDraft] = useState({});
  const [form, setForm] = useState({ dni: '', telefono: '', nombre: '', apellido: '', email: '' });
  const [fields, setFields] = useState({});
  const [initializing, setInitializing] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [fatal, setFatal] = useState(null);
  const saved = useRef(null);
  const lock = useRef(false);
  const heading = useRef(null);
  const errorAlert = useRef(null);
  const professional = snapshot?.profesional;
  const client = snapshot?.cliente;
  const today = professionalToday(professional?.zonaHoraria || 'America/Argentina/Buenos_Aires');
  const canQuery = Boolean(session && client?.puedeReservar && !fatal && ['fecha', 'horario', 'confirmacion'].includes(step));
  const monthsQuery = useQuery({ queryKey: ['booking', session, 'months'], queryFn: () => selfServiceApi.months(session), enabled: canQuery, retry: false, gcTime: 0 });
  const activeMonths = monthsQuery.data || [];
  const requestedMonth = draft.month?.slice(0, 7) || today.slice(0, 7);
  const monthKey = activeMonths.includes(requestedMonth) ? requestedMonth : activeMonths[0] || '';
  const monthDate = localDate(`${monthKey || today.slice(0, 7)}-01`);
  const monthIndex = activeMonths.indexOf(monthKey);
  const monthYear = monthDate.getFullYear(), monthNumber = monthDate.getMonth() + 1;
  const monthDays = Array.from({ length: new Date(monthYear, monthNumber, 0).getDate() }, (_, i) => {
    const fecha = dateKey(new Date(monthYear, monthNumber - 1, i + 1));
    return { id: fecha, fecha, seleccionable: false };
  });
  function navigateMonth(delta) {
    const nextMonth = activeMonths[monthIndex + delta];
    if (!nextMonth) return;
    const next = `${nextMonth}-01`;
    changeDraft({ month: next, date: '', hour: '', reference: null, pending: null });
  }
  const appointmentsQuery = useQuery({ queryKey: ['booking', session, 'appointments'], queryFn: () => selfServiceApi.appointments(session), enabled: Boolean(session && client && client.estado !== 'NUEVO' && !fatal && step === 'turnos'), retry: false, gcTime: 0 });
  const configQuery = useQuery({ queryKey: ['booking', session, 'config'], queryFn: () => selfServiceApi.config(session), enabled: canQuery, retry: false, gcTime: 0 });
  const config = configQuery.data;
  const datesQuery = useQuery({ queryKey: ['booking', session, 'days', monthKey], queryFn: () => selfServiceApi.days(session, monthKey), enabled: canQuery && Boolean(monthKey), retry: false, gcTime: 0 });
  const gapsQuery = useQuery({ queryKey: ['booking', session, 'gaps', draft.date], queryFn: () => selfServiceApi.gaps(session, draft.date), enabled: canQuery && Boolean(draft.date), retry: false, gcTime: 0 });
  const activeDays = datesQuery.data || [];
  const dates = activeDays.filter(d => d.seleccionable).map(d => d.fecha);
  const gaps = gapsQuery.data || [];
  const loadingDates = datesQuery.isFetching;
  const loadingGaps = gapsQuery.isFetching;
  const queryError = (canQuery && (monthsQuery.error || configQuery.error || datesQuery.error || gapsQuery.error)) || (step === 'turnos' && appointmentsQuery.error);
  const displayedError = error || queryError?.message || '';
  const displayedFatal = fatal || (queryError?.status === 401 ? { kind: 'session', message: 'Tu sesión venció o el enlace fue revocado. Volvé a iniciar para continuar.' } : null);
  const hasSelectedHour = gaps.some(gap => gap.intervalos.some(interval => interval.inicio === draft.hour));
  function refreshAvailability() {
    if (canQuery) {
      monthsQuery.refetch();
      configQuery.refetch();
      if (monthKey) datesQuery.refetch();
      if (draft.date) gapsQuery.refetch();
    }
  }

  const persist = useCallback(patch => { saved.current = { ...saved.current, ...patch }; saveBooking(saved.current); }, []);
  const changeDraft = useCallback(patch => { const next = { ...saved.current?.draft, ...patch }; persist({ draft: next }); setDraft(next); }, [persist]);
  const move = useCallback(next => { persist({ step: next }); setStep(next); setError(''); setFields({}); }, [persist]);
  const applySnapshot = useCallback((data, storedStep) => {
    setSnapshot(data);
    const c = data.cliente;
    setForm(f => ({ ...f, dni: data.dni || '', nombre: c?.nombre || '', apellido: c?.apellido || '', email: c?.email || '', telefono: c?.telefono || '' }));
    if (data.resultado && (!storedStep || storedStep === 'resultado' || (storedStep === 'confirmacion' && saved.current?.draft?.pending?.key === data.claveResultado))) { persist({ draft: {} }); setDraft({}); move('resultado'); }
    else if (!c) move('identificacion');
    else if (c.estado === 'NUEVO') move(storedStep === 'datos' ? 'datos' : 'nuevo');
    else if (storedStep === 'editar') move('editar');
    else if (storedStep === 'turnos') move('turnos');
    else if (!c.puedeReservar) move('panel');
    else move(['fecha', 'horario', 'confirmacion'].includes(storedStep) ? storedStep : 'panel');
  }, [persist, move]);

  useEffect(() => {
    let active = true;
    async function init() {
      let stored = readBooking();
      const hash = window.location.hash.slice(1);
      const link = hash || stored?.link;
      if (hash && stored?.link !== hash) { clearBooking(); stored = null; }
      saved.current = stored || { link, draft: {} };
      if (!link || !/^[A-Za-z0-9_-]{43}$/.test(link)) {
        setFatal({ kind: 'link', message: 'Solicitá al profesional un enlace de reserva válido.' }); setInitializing(false); return;
      }
      try {
        const credential = stored?.session ? stored : await selfServiceApi.open(link);
        const token = stored?.session || credential.token;
        const data = await selfServiceApi.resume(token);
        if (!active) return;
        persist({ session: token, link, draft: stored?.draft || {} });
        setSession(token); setDraft(stored?.draft || {}); applySnapshot(data, stored?.step);
      } catch (e) {
        if (active) setFatal({ kind: stored?.session && e.status === 401 ? 'session' : e.status === 0 ? 'connection' : 'link', message: e.status === 401 ? (stored?.session ? 'Tu sesión venció o el enlace fue revocado. Volvé a iniciar para continuar.' : 'Este enlace ya no está disponible. Solicitá uno nuevo al profesional.') : e.message });
      } finally { if (active) setInitializing(false); }
    }
    init(); return () => { active = false; };
  }, [applySnapshot, persist]);

  useEffect(() => { heading.current?.focus(); }, [step, fatal]);
  useEffect(() => {
    if (displayedError && !busy) errorAlert.current?.focus();
  }, [displayedError, busy]);
  useEffect(() => {
    const previous = document.title;
    document.title = 'Reserva de turnos';
    return () => { document.title = previous; };
  }, []);

  async function restart() {
    if (lock.current) return;
    lock.current = true; setBusy(true); setError('');
    try {
      if (displayedFatal?.kind === 'connection' && saved.current.session) {
        const data = await selfServiceApi.resume(saved.current.session);
        setSession(saved.current.session); setDraft(saved.current.draft || {}); setFatal(null);
        applySnapshot(data, saved.current.step);
        return;
      }
      const result = await selfServiceApi.open(saved.current.link);
      const data = await selfServiceApi.resume(result.token);
      clearBooking(); saved.current = { link: saved.current.link, session: result.token, draft: {} }; saveBooking(saved.current);
      setSession(result.token); setDraft({}); setFatal(null); applySnapshot(data);
    } catch (e) { setFatal({ kind: e.status === 0 ? 'connection' : displayedFatal?.kind === 'connection' && saved.current.session && e.status === 401 ? 'session' : 'link', message: e.status === 401 ? 'Tu sesión o enlace ya no está disponible. Volvé a iniciar o solicitá un enlace nuevo al profesional.' : e.message }); }
    finally { lock.current = false; setBusy(false); }
  }

  function fail(e) {
    if (e.status === 401) setFatal({ kind: 'session', message: 'Tu sesión venció o el enlace fue revocado. Volvé a iniciar para continuar.' });
    else setError(e.status === 429 ? 'Llegaste al límite de intentos. Esperá un minuto antes de volver a intentar.' : e.message || 'No se pudo completar la acción. Volvé a intentar.');
  }

  function edit(name, value) { setForm(f => ({ ...f, [name]: ['nombre', 'apellido'].includes(name) ? normalizeClientName(value, false) : value })); setFields(f => ({ ...f, [name]: '' })); }

  async function submitDetails(event) {
    event.preventDefault(); if (lock.current) return;
    const invalid = {};
    if (step === 'identificacion') {
      if (!/^\d{6,12}$/.test(form.dni.trim())) invalid.dni = 'Ingresá un DNI de 6 a 12 dígitos, sin puntos.';
    } else {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) invalid.email = 'Ingresá un correo válido.';
      if (client?.estado === 'NUEVO' || step === 'editar') {
        if (!form.nombre.trim()) invalid.nombre = 'Ingresá tu nombre.';
        if (!form.apellido.trim()) invalid.apellido = 'Ingresá tu apellido.';
      }
      if (!/^[0-9]{6,20}$/.test(form.telefono.replace(/[\s()+.-]/g, ''))) invalid.telefono = 'Ingresá un teléfono válido.';
    }
    setFields(invalid); if (Object.keys(invalid).length) return;
    lock.current = true; setBusy(true); setError('');
    try {
      let c;
      if (step === 'identificacion') {
        c = await selfServiceApi.identify(session, { dni: form.dni.trim() });
        setSnapshot(s => ({ ...s, cliente: c, dni: form.dni.trim() }));
        if (c.estado !== 'NUEVO') setForm(f => ({ ...f, ...c }));
        move(c.estado === 'NUEVO' ? 'nuevo' : 'panel');
      } else if (client.estado === 'NUEVO') {
        c = await selfServiceApi.register(session, { nombre: normalizeClientName(form.nombre), apellido: normalizeClientName(form.apellido), email: form.email.trim(), telefono: form.telefono.trim() });
        setSnapshot(s => ({ ...s, cliente: c })); move('fecha'); persist({ draft: {} }); setDraft({});
      } else if (step === 'editar') {
        c = await selfServiceApi.contact(session, { nombre: normalizeClientName(form.nombre), apellido: normalizeClientName(form.apellido), email: form.email.trim(), telefono: form.telefono.trim() });
        setSnapshot(s => ({ ...s, cliente: c })); setForm(f => ({ ...f, ...c })); move('panel');
      } else {
        c = await selfServiceApi.contact(session, { email: form.email.trim(), telefono: form.telefono });
        setSnapshot(s => ({ ...s, cliente: c })); move(c.puedeReservar ? 'fecha' : 'bloqueado');
      }
    } catch (e) {
      if (e.status === 409 && (step === 'identificacion' || client?.estado === 'NUEVO')) {
        // Recover an identification/registration that committed before the connection was lost.
        try { applySnapshot(await selfServiceApi.resume(session), 'datos'); } catch (resumeError) { fail(resumeError); }
      } else fail(e);
    } finally { lock.current = false; setBusy(false); }
  }

  async function review() {
    if (lock.current) return; lock.current = true; setBusy(true); setError('');
    try {
      const reference = await selfServiceApi.choose(session, { fecha: draft.date, horaInicio: draft.hour });
      changeDraft({ reference: reference.token, referenceExpires: reference.vence, pending: null }); move('confirmacion');
    } catch (e) { if (e.status === 409) { changeDraft({ hour: '', reference: null }); refreshAvailability(); setError('Ese horario ya no está disponible. Elegí otro.'); } else fail(e); }
    finally { lock.current = false; setBusy(false); }
  }

  async function confirm() {
    if (lock.current) return; lock.current = true; setBusy(true); setError('');
    try {
      const attempt = draft.pending || { key: reservationKey(), body: { referencia: draft.reference, observaciones: draft.observations || '' } };
      changeDraft({ pending: attempt });
      const result = await selfServiceApi.confirm(session, attempt.key, attempt.body);
      setSnapshot(s => ({ ...s, resultado: result })); changeDraft({ pending: null, observations: '', reference: null }); move('resultado');
    } catch (e) {
      if (e.status === 409) {
        changeDraft({ pending: null, reference: null, hour: '' }); move('horario'); refreshAvailability();
        setError('El horario dejó de estar disponible o venció su selección. Elegí un horario para continuar.');
      } else if (e.status === 403) {
        try { applySnapshot(await selfServiceApi.resume(session), 'datos'); } catch (resumeError) { fail(resumeError); }
        changeDraft({ pending: null, reference: null });
      } else {
        if (e.status >= 400 && e.status < 500 && e.status !== 429) changeDraft({ pending: null });
        fail(e);
      }
    } finally { lock.current = false; setBusy(false); }
  }

  const index = stepNames.indexOf(step);
  const title = displayedFatal ? 'No pudimos continuar' : initializing ? 'Preparando tu reserva' : step === 'identificacion' ? 'Empecemos por tus datos' : step === 'editar' ? 'Editar mis datos' : step === 'panel' ? 'Te damos la bienvenida' : step === 'turnos' ? 'Mis turnos' : step === 'nuevo' ? 'Todavía no estás registrado' : step === 'datos' ? client?.estado === 'NUEVO' ? `Registrate con ${professional?.nombre || ''} ${professional?.apellido || ''}` : 'Revisá tus datos de contacto' : step === 'fecha' ? 'Elegí un día' : step === 'horario' ? 'Encontrá tu horario' : step === 'confirmacion' ? 'Revisá tu solicitud' : step === 'resultado' ? snapshot?.resultado?.estado === 'ASIGNADO' ? 'Tu turno está asignado' : 'Tu solicitud espera aprobación' : 'Por ahora no podés reservar';
  const result = snapshot?.resultado;
  const resultDate = result ? new Intl.DateTimeFormat('es-AR', { timeZone: professional?.zonaHoraria, dateStyle: 'long', timeStyle: 'short' }).format(new Date(result.inicio)) : '';
  const blockedMessage = client?.estado === 'INHABILITADO'
      ? 'Tu registro está inhabilitado para reservar. Contactá al profesional para consultar cómo continuar.'
      : client?.estado === 'DADO_DE_BAJA'
        ? 'Tu registro fue dado de baja. Contactá al profesional antes de volver a solicitar un turno.'
        : 'Tu estado actual no permite solicitar turnos. Contactá al profesional para consultar cómo continuar.';

  return <div className="min-h-screen bg-background text-foreground">
    <header className="sticky top-0 z-40 border-b bg-card"><div className="mx-auto flex max-w-6xl items-center flex-wrap justify-between gap-4 px-4 py-5 sm:px-8"><span className="font-heading text-xl font-semibold tracking-tight">Turnos<span className="text-muted-foreground"> / Reserva</span></span><span className="flex items-center gap-2 text-xs text-muted-foreground"><ShieldCheck className="size-4" aria-hidden="true" />Agenda del profesional</span><Button type="button" variant="outline" disabled={initializing || busy} onClick={() => { if (displayedFatal) { restart(); return; } move(client ? client.estado === 'NUEVO' ? 'nuevo' : 'panel' : 'identificacion'); }}><ArrowLeft data-icon="inline-start" />Volver al inicio</Button></div></header>
    <main className="mx-auto grid max-w-6xl gap-6 px-4 py-8 sm:px-8 lg:grid-cols-[minmax(0,1fr)_19rem] lg:py-12">
      <div className="flex min-w-0 flex-col gap-6">
        {index >= 0 && !displayedFatal && <nav aria-label="Pasos de reserva"><ol className="grid grid-cols-5 gap-2">{steps.map((label, i) => <li key={label} aria-current={index === i ? 'step' : undefined} className="flex flex-col gap-2"><span className={cn('flex size-8 items-center justify-center rounded-full border text-sm font-semibold', i <= index ? 'border-primary bg-primary text-primary-foreground' : 'text-muted-foreground')}>{i < index ? <CheckCircle2 className="size-4" /> : i + 1}</span><span className={cn('text-xs', i === index ? 'font-semibold' : 'text-muted-foreground', 'hidden sm:block')}>{label}</span></li>)}</ol><p className="mt-3 text-sm text-muted-foreground sm:hidden">Paso {index + 1} de 5 · {steps[index]}</p></nav>}
        <Card className="overflow-visible">
          <CardHeader><CardTitle><h1 ref={heading} tabIndex={-1} className="font-heading text-2xl leading-tight outline-none sm:text-3xl">{title}</h1></CardTitle>
            <CardDescription>{displayedFatal || initializing ? '' : step === 'identificacion' ? 'Ingresá tu DNI para consultar tus turnos o registrarte como cliente.' : step === 'fecha' ? 'Seleccioná un día activo de la agenda del profesional.' : step === 'horario' ? 'Los horarios están agrupados por franja de atención.' : step === 'confirmacion' ? 'El cupo se comprueba al confirmar. Tu horario todavía no está reservado.' : ''}</CardDescription></CardHeader>
          <CardContent className="relative isolate z-0 flex flex-col gap-5">
            {displayedError && <Alert ref={errorAlert} tabIndex={-1} variant="destructive" role="alert"><AlertTitle>No se pudo completar la acción</AlertTitle><AlertDescription>{displayedError}{(step === 'fecha' || step === 'horario') && <Button variant="outline" onClick={() => { setError(''); refreshAvailability(); }}>Volver a consultar disponibilidad</Button>}</AlertDescription></Alert>}
            {step === 'editar' && !initializing && !displayedFatal && <div className="flex flex-wrap gap-3"><Button type="submit" form="booking-details" disabled={busy}>{busy ? 'Guardando…' : 'Guardar mis datos'}</Button><Button variant="outline" disabled={busy} onClick={() => move('panel')}>Cancelar</Button></div>}
            {initializing ? <p aria-busy="true" className="flex items-center gap-2"><LoaderCircle className="animate-spin" />Consultando la agenda…</p> : displayedFatal ? <><p>{displayedFatal.message}</p>{displayedFatal.kind !== 'link' && <Button disabled={busy} onClick={restart}>{displayedFatal.kind === 'connection' ? 'Volver a consultar' : 'Volver a iniciar'}</Button>}<ContactProfessional professional={professional} /></> : <>
              {(step === 'identificacion' || step === 'datos' || step === 'editar') && <form id="booking-details" noValidate onSubmit={submitDetails}>
                <FieldGroup>
                  {step === 'identificacion' ? <>
                    <FormInput id="booking-dni" label="DNI" value={form.dni} onChange={e => edit('dni', e.target.value)} inputMode="numeric" autoComplete="off" maxLength={12} error={fields.dni} description="Sin puntos ni espacios." disabled={busy} />

                  </> : <>
                    <div className="rounded-xl border bg-muted/30 p-4 text-sm"><p>DNI <strong>{snapshot?.dni}</strong></p>{client?.estado !== 'NUEVO' && <p className="mt-1">{client?.nombre} {client?.apellido}</p>}</div>
                    {(client?.estado === 'NUEVO' || step === 'editar') ? <><FormInput id="booking-name" label="Nombre" value={form.nombre} onChange={e => edit('nombre', e.target.value)} autoComplete="given-name" maxLength={100} error={fields.nombre} disabled={busy} /><FormInput id="booking-surname" label="Apellido" value={form.apellido} onChange={e => edit('apellido', e.target.value)} autoComplete="family-name" maxLength={100} error={fields.apellido} disabled={busy} />{client?.estado === 'NUEVO' && <Alert><AlertTitle>Primero, tu registro</AlertTitle><AlertDescription>Tus datos quedarán pendientes de verificación. Podés solicitar un turno y el profesional deberá aprobarlo.</AlertDescription></Alert>}</> : <FormInput id="booking-contact-phone" label="Teléfono de contacto" type="tel" value={form.telefono} onChange={e => edit('telefono', e.target.value)} autoComplete="tel" maxLength={50} error={fields.telefono} disabled={busy} />}
                    {(client?.estado === 'NUEVO' || step === 'editar') && <FormInput id="booking-register-phone" label="Teléfono" type="tel" value={form.telefono} onChange={e => edit('telefono', e.target.value)} autoComplete="tel" maxLength={50} error={fields.telefono} disabled={busy} />}
                    <FormInput id="booking-email" label="Correo electrónico" type="email" value={form.email} onChange={e => edit('email', e.target.value)} autoComplete="email" maxLength={150} error={fields.email} disabled={busy} />
                  </>}
                </FieldGroup>
              </form>}
              {step === 'nuevo' && <><p>No encontramos un cliente con DNI {snapshot?.dni} registrado con este profesional.</p><Button onClick={() => move('datos')}>Registrarse como cliente de {professional?.nombre} {professional?.apellido}</Button><Button variant="outline" disabled={busy} onClick={restart}>Ingresar otro DNI</Button></>}
              {step === 'panel' && <>
                <p>Hola, {client?.nombre} {client?.apellido}. ¿Qué querés hacer?</p>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Button variant="outline" size="lg" className="h-auto min-h-40 flex-col items-start gap-4 whitespace-normal px-6 py-8" aria-label="Consultar mis turnos" onClick={() => move('turnos')}><CalendarDays data-icon="inline-start" /><span className="font-heading text-xl">Consultar mis turnos</span><span className="text-sm text-muted-foreground">Ver tus turnos y el estado de cada solicitud.</span></Button>
                  <Button size="lg" className="h-auto min-h-40 flex-col items-start gap-4 whitespace-normal px-6 py-8" aria-label="Registrar un nuevo turno" disabled={!client?.puedeReservar} onClick={() => { changeDraft({ month: `${today.slice(0, 7)}-01`, date: '', hour: '', reference: null, observations: '', pending: null }); move('fecha'); }}><ArrowRight data-icon="inline-start" /><span className="font-heading text-xl">Registrar un nuevo turno</span><span className="text-sm">Elegir un día y un horario para tu atención.</span></Button>
                </div>
                {!client?.puedeReservar && <><p>{blockedMessage}</p><ContactProfessional professional={professional} /></>}
                <Button variant="outline" size="lg" disabled={busy} onClick={() => { setForm(f => ({ ...f, ...client })); move('editar'); }}>Editar mis datos</Button>
                <Button variant="ghost" disabled={busy} onClick={restart}>Ingresar otro DNI</Button>
              </>}
              {step === 'turnos' && <>
                  {appointmentsQuery.isFetching ? <p role="status">Consultando tus turnos…</p> : appointmentsQuery.error ? <Button variant="outline" onClick={() => appointmentsQuery.refetch()}>Volver a consultar mis turnos</Button> : appointmentsQuery.data?.length ? <ul className="mt-4 flex flex-col gap-3">{appointmentsQuery.data.map(t => <li key={t.turnoId} className="rounded-lg border p-4"><p className="font-semibold">{t.tipoAtencion || 'Turno'} · #{t.turnoId}</p><p>{new Intl.DateTimeFormat('es-AR', { timeZone: professional?.zonaHoraria, dateStyle: 'long', timeStyle: 'short' }).format(new Date(t.inicio))}</p><p className="text-sm text-muted-foreground">{t.estado?.toLowerCase().replaceAll('_', ' ')}</p></li>)}</ul> : <p className="mt-3 text-muted-foreground">Todavía no tenés turnos con este profesional.</p>}
              </>}
              {step === 'fecha' && <>
                {monthsQuery.isFetching ? <p role="status">Consultando meses activos…</p> : !activeMonths.length ? <p>No hay meses activos disponibles para reservar.</p> : <>
                  <div className="flex justify-between gap-3"><Button variant="outline" disabled={monthIndex <= 0} onClick={() => navigateMonth(-1)}>Mes anterior</Button><Button variant="outline" disabled={monthIndex < 0 || monthIndex >= activeMonths.length - 1} onClick={() => navigateMonth(1)}>Mes siguiente</Button></div>
                  <MonthCalendar year={monthYear} month={monthNumber} loading={loadingDates} publicBooking disableUnselectable selectedDayId={draft.date} days={monthDays.map(d => { const active = activeDays.find(a => a.fecha === d.fecha); return { ...d, ...active, id: d.fecha, seleccionable: Boolean(active?.seleccionable) }; })} onSelectDay={day => { if (!dates.includes(day.fecha) || busy) return; changeDraft({ date: day.fecha, hour: '', reference: null, pending: null }); move('horario'); }} />
                  <p className="text-sm text-muted-foreground">Elegí uno de los días disponibles.</p>
                  {!loadingDates && !monthDays.some(d => dates.includes(d.fecha)) && <p>No hay días activos disponibles para reservar este mes.</p>}
                </>}

              </>}
              {step === 'horario' && <>
                <p className="text-sm text-muted-foreground">{draft.date && friendlyDate(draft.date)}</p>
                {loadingGaps ? <p role="status">Actualizando horarios…</p> : gaps.length ? <DailyTimeline publicBooking selectionDisabled={busy} timezone={professional?.zonaHoraria} showIntegrationNotice={false} day={{ fecha: draft.date, estadoActual: 'ACTIVO', brechas: gaps.map(g => ({ horaInicio: g.inicio, horaFin: g.fin })) }} candidateSlots={gaps.flatMap(g => g.intervalos.map(h => ({ horaInicio: h.inicio, horaFin: h.fin })))} selectedCandidate={gaps.flatMap(g => g.intervalos).filter(h => h.inicio === draft.hour).map(h => ({ horaInicio: h.inicio, horaFin: h.fin }))[0]} onSelectCandidate={h => changeDraft({ hour: h.horaInicio, reference: null, pending: null })} /> : <><p>No quedan horarios disponibles para este día. Podés elegir otra fecha.</p><Button variant="outline" onClick={() => move('fecha')}>Elegir otra fecha</Button></>}
              </>}
              {step === 'confirmacion' && <>
                <dl className="grid gap-4 rounded-xl border bg-muted/30 p-5 text-sm"><div><dt className="text-muted-foreground">Duración del turno</dt><dd className="font-semibold">{config?.duracionMinutos} minutos</dd></div><div><dt className="text-muted-foreground">Fecha y horario</dt><dd className="font-semibold">{draft.date && friendlyDate(draft.date)} · {timeLabel(draft.hour)}</dd></div><div><dt className="text-muted-foreground">Cliente</dt><dd>{client?.nombre} {client?.apellido} · DNI {snapshot?.dni}</dd></div><div><dt className="text-muted-foreground">Contacto</dt><dd className="break-words">{client?.email} · {client?.telefono}</dd></div></dl>
                {['REQUIERE_APROBACION', 'PENDIENTE_DE_VERIFICACION'].includes(client?.estado) && <Alert><AlertTitle>Esta solicitud requiere aprobación</AlertTitle><AlertDescription>El profesional debe aprobarla antes de que tu turno quede asignado.</AlertDescription></Alert>}
                <Field><FieldLabel htmlFor="booking-observations">Observaciones (opcional)</FieldLabel><Textarea id="booking-observations" maxLength={1000} value={draft.observations || ''} onChange={e => changeDraft({ observations: e.target.value })} disabled={busy || Boolean(draft.pending)} /></Field>
                {draft.pending && !busy && <Alert><AlertTitle>Comprobá el resultado de tu solicitud</AlertTitle><AlertDescription>Reintentá la confirmación con el mismo pedido. No se creará un turno duplicado.</AlertDescription></Alert>}
              </>}
              {step === 'resultado' && <><CheckCircle2 className="size-12 text-primary" aria-hidden="true" /><p>{result?.estado === 'ASIGNADO' ? 'La reserva se completó. Guardá los datos de tu turno.' : 'La solicitud fue registrada. Esperá la aprobación del profesional antes de considerar confirmado el turno.'}</p><dl className="grid gap-3 rounded-xl border p-5"><div><dt className="text-sm text-muted-foreground">Turno</dt><dd className="font-heading text-xl">#{result?.turnoId}</dd></div><div><dt className="text-sm text-muted-foreground">Fecha y horario</dt><dd>{resultDate}</dd></div><div><dt className="text-sm text-muted-foreground">Estado</dt><dd>{result?.estado === 'ASIGNADO' ? 'Asignado' : 'Pendiente de aprobación'}</dd></div></dl><Button variant="outline" onClick={() => move('turnos')}>Consultar mis turnos</Button><ContactProfessional professional={professional} /></>}
            </>}
          </CardContent>
          {!initializing && !displayedFatal && index >= 0 && <CardFooter className="sticky bottom-0 z-30 flex flex-wrap justify-between gap-3 border-t bg-card py-4 shadow-[0_-4px_12px_rgba(0,0,0,0.06)]">
            <div>{index > 1 && <Button variant="outline" disabled={busy || Boolean(draft.pending)} onClick={() => move(index === 2 ? 'panel' : stepNames[index - 1])}><ArrowLeft data-icon="inline-start" />Volver</Button>}</div>
            {index <= 1 ? <Button type="submit" form="booking-details" disabled={busy}>{busy && <LoaderCircle className="animate-spin" data-icon="inline-start" />}{index === 0 ? 'Ingresar con mi DNI' : client?.estado === 'NUEVO' ? 'Registrarme y elegir fecha' : 'Continuar'}<ArrowRight data-icon="inline-end" /></Button> : index === 2 ? <p className="text-sm text-muted-foreground">Seleccioná un día del calendario.</p> : index === 3 ? <Button disabled={!hasSelectedHour || loadingGaps || busy} onClick={review}>{busy && <LoaderCircle className="animate-spin" data-icon="inline-start" />}Revisar solicitud<ArrowRight data-icon="inline-end" /></Button> : <Button disabled={busy} onClick={confirm}>{busy && <LoaderCircle className="animate-spin" data-icon="inline-start" />}{draft.pending ? 'Reintentar confirmación' : ['REQUIERE_APROBACION', 'PENDIENTE_DE_VERIFICACION'].includes(client?.estado) ? 'Solicitar turno' : 'Confirmar turno'}</Button>}
          </CardFooter>}
        </Card>
        {professional && <p className="text-xs text-muted-foreground">Los horarios corresponden a la zona horaria del profesional: {professional.zonaHoraria}.</p>}
      </div>
      <aside className="order-first lg:order-last"><div className="flex flex-col gap-4 lg:sticky lg:top-8"><div className="border-l-4 border-primary pl-5"><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-muted-foreground">Tu profesional</p><p className="font-heading text-2xl font-semibold leading-tight">{professional ? `${professional.nombre} ${professional.apellido}` : 'Reserva de turnos'}</p>{professional?.especialidad && <p className="mt-2 text-muted-foreground">{professional.especialidad}</p>}</div>{draft.date && !displayedFatal && <div className="hidden rounded-xl border bg-card p-5 lg:block"><p className="flex items-center gap-2 text-sm font-semibold"><CalendarDays className="size-4" />Tu selección</p><p className="mt-3 text-sm">{friendlyDate(draft.date)}</p>{draft.hour && <p className="mt-1 font-heading text-xl">{timeLabel(draft.hour)}</p>}</div>}<p className="hidden text-sm leading-relaxed text-muted-foreground lg:block">La disponibilidad se actualiza al confirmar. Si el horario se ocupa, podés elegir otro sin repetir tus datos.</p></div></aside>
    </main>
  </div>;
}
