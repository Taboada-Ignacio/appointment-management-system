import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CalendarOff,
  CalendarPlus,
  CheckCircle2,
  Clock3,
  Palmtree,
  Plus,
  SlidersHorizontal,
  Trash2,
  UserRoundX,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { DateRangePickerField } from '@/components/DateRangePickerField';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { TimeWheelPicker } from '@/components/ui/TimeWheelPicker';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/ToastProvider';
import { PageHeader } from '../components/PageHeader';
import { useCreateAbsence, usePreviewAbsence, usePreviewAbsenceUpdate, useUpdateAbsence } from '../hooks/useAbsences';
import { useSelectableDays } from '../hooks/useAgenda';
import { AffectedAppointmentsPanel, ExceptionsPanel } from '../components/ExceptionPanels';
import { TYPE_LABELS } from '../utils/exceptionLabels';

const TYPES = [
  { id: 'VACACIONES', title: 'Vacaciones', icon: Palmtree, description: 'Cierra completamente la agenda durante un período continuo. Incluye todos los días del rango.' },
  { id: 'DIA_NO_LABORABLE', title: 'Día no laborable', icon: CalendarOff, description: 'Cierra uno o varios días puntuales sin modificar tus horarios habituales.' },
  { id: 'BLOQUEO_HORARIO', title: 'Bloqueo de Horario', icon: Clock3, description: 'Quita temporalmente una o más franjas. El resto del día continúa disponible.' },
];

const SUGGESTED_REASONS = {
  register: ['Vacaciones programadas', 'Compromiso personal', 'Capacitación', 'Reunión institucional', 'Trámite'],
  habilitaciones: ['Atención extraordinaria', 'Guardia especial', 'Refuerzo de atención', 'Compensación horaria', 'Sobreturnos programados'],
  modificaciones: ['Horario corrido especial', 'Cambio excepcional de turno', 'Ajuste de jornada', 'Atención reducida programada'],
};

const SECTION_CONFIG = {
  register: {
    eyebrow: 'Ausencias y Modificaciones Excepcionales',
    title: 'Registrar Ausencias',
    description: 'Registrá cambios puntuales sin modificar la configuración habitual de tu agenda.',
    step1Title: 'Registrar una ausencia',
    step1Subtitle: 'Elegí el tipo para conocer exactamente cómo afectará la disponibilidad.',
    step1Label: 'Datos de la ausencia',
    step2Label: 'Impacto y resolución',
    initialType: 'VACACIONES',
    successTitle: 'La excepción ha sido cargada con éxito',
    successSubtitle: 'quedó registrada y ya se aplica sobre la agenda.',
    anotherActionLabel: 'Registrar otra excepción',
    anotherActionPath: '/profesional/ausencias/registrar',
    confirmButtonLabel: 'Confirmar ausencia',
  },
  habilitaciones: {
    eyebrow: 'Ausencias y Modificaciones Excepcionales',
    title: 'Registrar Habilitaciones Extraordinarias',
    description: 'Habilitá atención en días u horarios que habitualmente no están disponibles.',
    step1Title: 'Habilitar atención extraordinaria',
    step1Subtitle: 'Definí el día o período y las franjas horarias habilitadas para recibir turnos.',
    step1Label: 'Datos de la habilitación',
    step2Label: 'Confirmación',
    initialType: 'HABILITACION_EXTRAORDINARIA',
    successTitle: 'Habilitación extraordinaria cargada con éxito',
    successSubtitle: 'quedó registrada y ya se aplica sobre la agenda.',
    anotherActionLabel: 'Registrar otra habilitación',
    anotherActionPath: '/profesional/ausencias/habilitaciones',
    confirmButtonLabel: 'Confirmar habilitación',
  },
  modificaciones: {
    eyebrow: 'Ausencias y Modificaciones Excepcionales',
    title: 'Registrar Modificaciones Extraordinarias',
    description: 'Sustituí el horario habitual de una fecha puntual por un esquema especial.',
    step1Title: 'Modificar horario de atención',
    step1Subtitle: 'Establecé el nuevo horario que reemplazará la jornada habitual en la fecha elegida.',
    step1Label: 'Datos de la modificación',
    step2Label: 'Impacto y resolución',
    initialType: 'MODIFICACION_HORARIO',
    successTitle: 'Modificación de horario cargada con éxito',
    successSubtitle: 'quedó registrada y ya se aplica sobre la agenda.',
    anotherActionLabel: 'Registrar otra modificación',
    anotherActionPath: '/profesional/ausencias/modificaciones',
    confirmButtonLabel: 'Confirmar modificación',
  },
};

const emptyGap = () => ({ horaInicio: '09:00', horaFin: '10:00' });

function datesInRange(start, end) {
  if (!start || !end || end < start) return [];
  const dates = [];
  const cursor = new Date(`${start}T12:00:00`);
  const limit = new Date(`${end}T12:00:00`);
  while (cursor <= limit && dates.length < 366) {
    dates.push(cursor.toISOString().slice(0, 10));
    cursor.setDate(cursor.getDate() + 1);
  }
  return dates;
}

function localToInstant(date, time) {
  return new Date(`${date}T${time}:00`).toISOString();
}

function mergeGaps(...groups) {
  const sorted = groups.flat().filter(Boolean).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
  return sorted.reduce((result, gap) => {
    const last = result.at(-1);
    if (last && gap.horaInicio <= last.horaFin) last.horaFin = last.horaFin > gap.horaFin ? last.horaFin : gap.horaFin;
    else result.push({ ...gap });
    return result;
  }, []);
}

export function AbsenceManagementPage({ section = 'register' }) {
  const navigate = useNavigate();
  const { success, error: showError } = useToast();
  const isWizard = ['register', 'habilitaciones', 'modificaciones'].includes(section);
  const config = SECTION_CONFIG[section] || SECTION_CONFIG.register;

  const [step, setStep] = useState(1);
  const [type, setType] = useState(config.initialType);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [reason, setReason] = useState('');
  const [gaps, setGaps] = useState([emptyGap()]);
  const [excludedDates, setExcludedDates] = useState([]);
  const [impact, setImpact] = useState(null);
  const [decisions, setDecisions] = useState({});
  const [result, setResult] = useState(null);
  const [rescheduling, setRescheduling] = useState(null);
  const [conflicts, setConflicts] = useState([]);
  const [editing, setEditing] = useState(null);
  const preview = usePreviewAbsence();
  const previewUpdate = usePreviewAbsenceUpdate();
  const create = useCreateAbsence();
  const update = useUpdateAbsence();

  const requiresGaps = ['BLOQUEO_HORARIO', 'HABILITACION_EXTRAORDINARIA', 'MODIFICACION_HORARIO', 'EXCEPCION_HORARIA'].includes(type);

  const payload = useMemo(() => ({
    fechaInicio: start,
    fechaFin: end || start,
    tipo: type,
    motivo: reason.trim(),
    brechas: requiresGaps ? gaps : [],
    fechasExcluidas: type === 'VACACIONES' ? [] : excludedDates,
  }), [end, excludedDates, gaps, reason, requiresGaps, start, type]);

  const rangeDates = useMemo(() => datesInRange(start, end || start), [start, end]);

  const canPreview = start && (end || start) >= start && reason.trim()
    && (type === 'VACACIONES' || excludedDates.length < rangeDates.length)
    && (!requiresGaps || (gaps.length > 0 && gaps.every((g) => g.horaInicio && g.horaFin && g.horaInicio < g.horaFin)));

  const review = async () => {
    try {
      const data = editing
        ? await previewUpdate.mutateAsync({ id: editing.id, payload })
        : await preview.mutateAsync(payload);
      setConflicts([]);
      setImpact(data);
      setDecisions(Object.fromEntries(data.turnosAfectados.map((turno) => [turno.turnoId, { decision: 'PENDIENTE' }])));
      setStep(2);
    } catch (error) {
      if (error.status === 409 && error.data?.codigo === 'EXCEPCION_AGENDA_SUPERPUESTA') {
        setConflicts(error.data.coincidencias || []);
        showError('Ya existe una excepción coincidente', 'Revisá la coincidencia y modificá la excepción vigente.');
      } else showError('No se pudo revisar el impacto', error.message);
    }
  };

  const confirm = async () => {
    try {
      const request = {
        ...payload,
        previewToken: impact.previewToken,
        decisiones: Object.entries(decisions).map(([turnoId, value]) => ({ turnoId: Number(turnoId), ...value })),
      };
      const data = editing
        ? await update.mutateAsync({ id: editing.id, payload: request })
        : await create.mutateAsync(request);
      const values = Object.values(decisions);
      setResult({
        ...data,
        resolutionSummary: {
          pendientes: values.filter((item) => item.decision === 'PENDIENTE').length,
          bajas: values.filter((item) => item.decision === 'DAR_DE_BAJA').length,
          reprogramados: values.filter((item) => item.decision === 'REPROGRAMAR').length,
        },
      });
      setStep(3);
      const toastTitle = section === 'habilitaciones'
        ? (editing ? 'Habilitación modificada' : 'Habilitación registrada')
        : section === 'modificaciones'
        ? (editing ? 'Modificación de horario actualizada' : 'Modificación de horario registrada')
        : (editing ? 'Excepción modificada' : 'Ausencia registrada');
      const toastDesc = section === 'habilitaciones'
        ? 'La agenda ya cuenta con la nueva disponibilidad.'
        : 'La agenda y los turnos se actualizaron correctamente.';
      success(toastTitle, toastDesc);
    } catch (error) {
      if (error.status === 409) {
        showError('El impacto cambió', 'Revisá nuevamente la lista antes de confirmar.');
        await review();
      } else {
        const errTitle = section === 'habilitaciones'
          ? 'No se pudo registrar la habilitación'
          : section === 'modificaciones'
          ? 'No se pudo registrar la modificación'
          : 'No se pudo registrar la ausencia';
        showError(errTitle, error.message);
      }
    }
  };

  const editConflict = ({ excepcion, fechasCoincidentes, accionSugerida }) => {
    const habilitar = accionSugerida === 'MODIFICAR_EXISTENTE_PARA_HABILITAR';
    setEditing(excepcion);
    setType(excepcion.tipo);
    setStart(habilitar ? excepcion.fechaInicio : [excepcion.fechaInicio, start].sort()[0]);
    setEnd(habilitar ? excepcion.fechaFin : [excepcion.fechaFin, end || start].sort().at(-1));
    const combinedGaps = mergeGaps(excepcion.brechas || [], requiresGaps ? gaps : []);
    setGaps(combinedGaps.length ? combinedGaps : [emptyGap()]);
    setExcludedDates(habilitar
      ? [...new Set([...(excepcion.fechasExcluidas || []), ...fechasCoincidentes])]
      : (excepcion.fechasExcluidas || []));
    setReason(`${excepcion.motivo}. Modificación: ${reason.trim()}`);
    setConflicts([]);
  };

  const updateDecision = (turnoId, decision) => setDecisions((current) => ({ ...current, [turnoId]: { decision } }));
  const resolveReschedule = (value) => {
    if (!value?.fecha || !value.horaInicio || !value.horaFin || value.horaInicio >= value.horaFin) return;
    setDecisions((current) => ({
      ...current,
      [rescheduling.turnoId]: {
        decision: 'REPROGRAMAR',
        nuevoDiaAgendaId: Number(value.diaAgendaId),
        nuevoInicio: localToInstant(value.fecha, value.horaInicio),
        nuevoFin: localToInstant(value.fecha, value.horaFin),
        observacion: value.observacion || '',
      },
    }));
    setRescheduling(null);
  };

  const reset = () => {
    setStep(1);
    setImpact(null);
    setResult(null);
    setDecisions({});
    setStart('');
    setEnd('');
    setReason('');
    setGaps([emptyGap()]);
    setExcludedDates([]);
    setConflicts([]);
    setEditing(null);
    setType(config.initialType);
  };

  const resultImpact = result?.impacto ?? impact;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow={section === 'affected' ? 'Agenda' : config.eyebrow}
        title={isWizard ? config.title : section === 'exceptions' ? 'Consultar excepciones' : 'Turnos afectados'}
        description={isWizard ? config.description : section === 'exceptions' ? 'Consultá el historial, vigencia y alcance de las excepciones registradas.' : 'Resolvé los turnos alcanzados por una excepción de agenda.'}
      />

      {isWizard && (
        <>
          <div className="flex items-center gap-2" aria-label={`Paso ${step} de 3`}>
            {[config.step1Label, config.step2Label, 'Resultado'].map((label, index) => (
              <div
                key={label}
                className={`flex-1 rounded-lg border px-3 py-2 text-xs font-semibold ${
                  step === index + 1
                    ? 'border-primary bg-primary/5 text-primary'
                    : step > index + 1
                    ? 'border-success/30 text-success'
                    : 'text-muted-foreground'
                }`}
              >
                {index + 1}. {label}
              </div>
            ))}
          </div>

          {step === 1 && (
            <Card>
              <CardHeader>
                <CardTitle>{config.step1Title}</CardTitle>
                <CardDescription>{config.step1Subtitle}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                {editing && (
                  <div className="rounded-xl border border-primary/40 bg-primary/5 p-4">
                    <strong>Editando la excepción #{editing.id}</strong>
                    <p className="mt-1 text-sm text-muted-foreground">
                      Los datos existentes y la nueva propuesta fueron consolidados. Revisalos antes de continuar.
                    </p>
                    <Button className="mt-3" size="sm" variant="outline" onClick={() => { setEditing(null); setConflicts([]); }}>
                      Descartar modificación
                    </Button>
                  </div>
                )}
                {conflicts.length > 0 && (
                  <div className="rounded-xl border-2 border-orange-400 bg-orange-50 p-5 text-orange-950">
                    <h3 className="text-lg font-bold">La excepción coincide con una configuración vigente</h3>
                    <p className="mt-1 text-sm">
                      No se creará una excepción duplicada. Modificá la existente para conservar un único historial y recalcular el impacto.
                    </p>
                    <div className="mt-4 space-y-3">
                      {conflicts.map((conflict) => (
                        <div key={conflict.excepcion.id} className="rounded-lg border border-orange-300 bg-white p-4">
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <div>
                              <strong>#{conflict.excepcion.id} · {TYPE_LABELS[conflict.excepcion.tipo] || conflict.excepcion.tipo}</strong>
                              <p className="text-sm">{conflict.excepcion.fechaInicio} a {conflict.excepcion.fechaFin} · {conflict.excepcion.motivo}</p>
                              <p className="mt-1 text-xs">Coincide en {conflict.fechasCoincidentes.length} día(s): {conflict.fechasCoincidentes.join(', ')}</p>
                            </div>
                            <Button onClick={() => editConflict(conflict)}>Modificar esta excepción</Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {section === 'register' && (
                  <div className="grid gap-3 md:grid-cols-3">
                    {TYPES.map(({ id, title, description, icon: Icon }) => (
                      <button
                        key={id}
                        type="button"
                        onClick={() => setType(id)}
                        className={`rounded-xl border p-4 text-left transition ${
                          type === id ? 'border-primary bg-primary/5 ring-1 ring-primary' : 'hover:border-primary/40'
                        }`}
                      >
                        <Icon className="mb-3 size-5 text-primary" />
                        <strong className="block text-sm">{title}</strong>
                        <span className="mt-2 block text-xs leading-5 text-muted-foreground">{description}</span>
                      </button>
                    ))}
                  </div>
                )}

                {section === 'habilitaciones' && (
                  <div className="rounded-xl border border-emerald-500/30 bg-emerald-50/70 p-4 text-emerald-950 dark:bg-emerald-950/20 dark:text-emerald-200">
                    <div className="flex items-start gap-3">
                      <CalendarPlus className="mt-0.5 size-5 shrink-0 text-emerald-600 dark:text-emerald-400" />
                      <div>
                        <strong className="block text-sm">Habilitación extraordinaria de disponibilidad</strong>
                        <p className="mt-1 text-xs leading-5 text-emerald-800 dark:text-emerald-300">
                          Esta excepción añade disponibilidad sobre la agenda en los días y horarios indicados, incluso si se trata de un feriado o día no laborable. No cancela turnos existentes.
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                {section === 'modificaciones' && (
                  <div className="rounded-xl border border-blue-500/30 bg-blue-50/70 p-4 text-blue-950 dark:bg-blue-950/20 dark:text-blue-200">
                    <div className="flex items-start gap-3">
                      <SlidersHorizontal className="mt-0.5 size-5 shrink-0 text-blue-600 dark:text-blue-400" />
                      <div>
                        <strong className="block text-sm">Modificación excepcional de horario</strong>
                        <p className="mt-1 text-xs leading-5 text-blue-800 dark:text-blue-300">
                          Reemplaza el horario base de los días seleccionados por las nuevas franjas indicadas. En el siguiente paso podrás revisar si algún turno existente queda fuera del nuevo horario y resolverlo.
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                <DateRangePickerField
                  start={start}
                  end={end}
                  onChange={(nextStart, nextEnd) => { setStart(nextStart); setEnd(nextEnd); }}
                />

                {type !== 'VACACIONES' && rangeDates.length > 1 && (
                  <div className="space-y-3">
                    <div>
                      <Label>Días incluidos</Label>
                      <p className="text-xs text-muted-foreground">Desmarcá los días en los que no debe aplicarse la excepción.</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {rangeDates.map((date) => {
                        const included = !excludedDates.includes(date);
                        return (
                          <Button
                            key={date}
                            type="button"
                            size="sm"
                            variant={included ? 'default' : 'outline'}
                            onClick={() => setExcludedDates((current) => included ? [...current, date] : current.filter((item) => item !== date))}
                          >
                            {date}
                          </Button>
                        );
                      })}
                    </div>
                  </div>
                )}

                {(type === 'BLOQUEO_HORARIO' || section === 'habilitaciones' || section === 'modificaciones') && (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <Label>
                        {section === 'habilitaciones'
                          ? 'Franjas horarias a habilitar'
                          : section === 'modificaciones'
                          ? 'Nuevas franjas horarias de atención'
                          : 'Franjas bloqueadas'}
                      </Label>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => setGaps((v) => [...v, emptyGap()])}
                      >
                        <Plus />
                        {section === 'habilitaciones' || section === 'modificaciones' ? 'Agregar franja horaria' : 'Agregar franja'}
                      </Button>
                    </div>
                    {gaps.map((gap, index) => (
                      <div key={index} className="flex items-end gap-2">
                        <div className="flex-1">
                          <Label className="sr-only">Inicio</Label>
                          <TimeWheelPicker
                            value={gap.horaInicio}
                            onChange={(val) => setGaps((v) => v.map((g, i) => i === index ? { ...g, horaInicio: val } : g))}
                            minuteStep={15}
                            aria-label={`Franja ${index + 1}: Hora de inicio`}
                          />
                        </div>
                        <div className="flex-1">
                          <Label className="sr-only">Fin</Label>
                          <TimeWheelPicker
                            value={gap.horaFin}
                            onChange={(val) => setGaps((v) => v.map((g, i) => i === index ? { ...g, horaFin: val } : g))}
                            minuteStep={15}
                            aria-label={`Franja ${index + 1}: Hora de fin`}
                          />
                        </div>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          disabled={gaps.length === 1}
                          onClick={() => setGaps((v) => v.filter((_, i) => i !== index))}
                          aria-label="Quitar franja"
                        >
                          <Trash2 />
                        </Button>
                      </div>
                    ))}
                  </div>
                )}

                <div className="space-y-2">
                  <Label htmlFor="absence-reason">Motivo</Label>
                  <Select onValueChange={(value) => setReason(value)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Elegí un motivo sugerido" />
                    </SelectTrigger>
                    <SelectContent>
                      {(SUGGESTED_REASONS[section] || SUGGESTED_REASONS.register).map((item) => (
                        <SelectItem key={item} value={item}>{item}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Textarea
                    id="absence-reason"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Detallá el motivo que quedará registrado en el historial"
                  />
                </div>

                <div className="flex justify-end">
                  <Button
                    disabled={!canPreview || preview.isPending || previewUpdate.isPending}
                    onClick={review}
                  >
                    {preview.isPending || previewUpdate.isPending
                      ? 'Analizando…'
                      : editing
                      ? 'Revisar modificación'
                      : section === 'habilitaciones'
                      ? 'Continuar'
                      : 'Revisar impacto'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}

          {step === 2 && section === 'habilitaciones' && (
            <Card>
              <CardHeader>
                <CardTitle>Confirmar habilitación extraordinaria</CardTitle>
                <CardDescription>
                  Esta habilitación añade disponibilidad y no cancela turnos previos. Podés confirmarla directamente.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-5">
                <div className="space-y-3 rounded-xl border bg-muted/40 p-4">
                  <div className="grid gap-3 sm:grid-cols-2">
                    <div>
                      <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Período</span>
                      <p className="mt-1 font-medium">{start} {end && end !== start ? `al ${end}` : ''}</p>
                    </div>
                    <div>
                      <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Franjas a habilitar</span>
                      <p className="mt-1 font-medium">{gaps.map((g) => `${g.horaInicio} – ${g.horaFin}`).join(', ')}</p>
                    </div>
                  </div>
                  <div>
                    <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Motivo</span>
                    <p className="mt-1 font-medium">{reason}</p>
                  </div>
                  {excludedDates.length > 0 && (
                    <div>
                      <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Fechas excluidas</span>
                      <p className="mt-1 text-sm">{excludedDates.join(', ')}</p>
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-between border-t pt-4">
                  <Button variant="ghost" onClick={() => setStep(1)}>Volver y editar</Button>
                  <Button onClick={confirm} disabled={create.isPending || update.isPending}>
                    {create.isPending || update.isPending ? 'Aplicando…' : editing ? 'Confirmar modificación' : 'Confirmar habilitación'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}

          {step === 2 && section !== 'habilitaciones' && (
            <Card>
              <CardHeader>
                <CardTitle>
                  {section === 'modificaciones' ? 'Turnos alcanzados por el cambio de horario' : 'Turnos afectados'}
                </CardTitle>
                <CardDescription>
                  Podés resolverlos ahora o dejarlos pendientes para tratarlos más adelante.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex flex-wrap gap-2">
                  <Badge variant="outline">{impact.cantidadTurnosAfectados} afectados</Badge>
                  <Badge variant="outline">{impact.cantidadNotificacionesWhatsApp} con WhatsApp</Badge>
                  <Badge variant="outline">{impact.cantidadSinNotificacion} contacto manual</Badge>
                </div>
                {impact.turnosAfectados.length === 0 ? (
                  <div className="rounded-xl border border-dashed p-8 text-center text-sm text-muted-foreground">
                    No existen turnos afectados. Podés confirmar {section === 'modificaciones' ? 'la modificación' : 'la ausencia'}.
                  </div>
                ) : (
                  impact.turnosAfectados.map((turno) => (
                    <div key={turno.turnoId} className="rounded-xl border p-4">
                      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                        <div>
                          <div className="flex items-center gap-2">
                            <strong>{turno.nombreCliente}</strong>
                            <Badge variant="outline">{turno.estado}</Badge>
                          </div>
                          <p className="mt-1 text-sm text-muted-foreground">
                            {turno.fecha} · {new Date(turno.inicioEstimado).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}–{new Date(turno.finEstimado).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} · {turno.telefono}
                          </p>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {turno.notificacionWhatsAppHabilitada ? 'WhatsApp habilitado' : 'Requiere contacto manual'}
                          </p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Button
                            size="sm"
                            variant={decisions[turno.turnoId]?.decision === 'PENDIENTE' ? 'default' : 'outline'}
                            onClick={() => updateDecision(turno.turnoId, 'PENDIENTE')}
                          >
                            Dejar pendiente
                          </Button>
                          <Button
                            size="sm"
                            variant={decisions[turno.turnoId]?.decision === 'DAR_DE_BAJA' ? 'destructive' : 'outline'}
                            onClick={() => updateDecision(turno.turnoId, 'DAR_DE_BAJA')}
                          >
                            <UserRoundX />
                            Dar de baja
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setRescheduling(turno)}
                          >
                            <Clock3 />
                            Reprogramar
                          </Button>
                        </div>
                      </div>
                    </div>
                  ))
                )}
                {impact.turnosAfectados.length > 0 && (
                  <div className="flex justify-end">
                    <Button
                      variant="outline"
                      onClick={() => setDecisions(Object.fromEntries(impact.turnosAfectados.map((t) => [t.turnoId, { decision: 'DAR_DE_BAJA' }])))}
                    >
                      Dar de baja todos
                    </Button>
                  </div>
                )}
                <div className="flex justify-between">
                  <Button variant="ghost" onClick={() => setStep(1)}>Volver y editar</Button>
                  <Button onClick={confirm} disabled={create.isPending || update.isPending}>
                    {create.isPending || update.isPending
                      ? 'Aplicando…'
                      : editing
                      ? 'Confirmar modificación'
                      : config.confirmButtonLabel}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}

          {step === 3 && (
            <Card className="overflow-hidden border-success/30">
              <div className="bg-success/10 px-6 py-10 text-center">
                <span className="mx-auto grid size-20 place-items-center rounded-full bg-success text-white shadow-lg">
                  <CheckCircle2 className="size-11" />
                </span>
                <h2 className="mt-6 font-heading text-3xl font-bold text-success">
                  {config.successTitle}
                </h2>
                <p className="mx-auto mt-2 max-w-2xl text-muted-foreground">
                  {TYPE_LABELS[result?.excepcion?.tipo] || 'La excepción'} {config.successSubtitle}
                </p>
              </div>
              <CardContent className="space-y-5 p-6">
                <div className="grid gap-3 sm:grid-cols-3 xl:grid-cols-5">
                  <Summary label="Turnos afectados" value={resultImpact?.cantidadTurnosAfectados ?? 0} />
                  <Summary label="Dados de baja" value={result.resolutionSummary.bajas} />
                  <Summary label="Reprogramados" value={result.resolutionSummary.reprogramados} />
                  <Summary label="Pendientes" value={result.resolutionSummary.pendientes} />
                  <Summary label="Contactos manuales" value={resultImpact?.cantidadSinNotificacion ?? 0} />
                </div>
                <div className="flex flex-wrap justify-center gap-3">
                  <Button onClick={() => navigate(`/profesional/ausencias/excepciones?excepcion=${result?.excepcion?.id || ''}`)}>
                    Ver todas las excepciones
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => {
                      reset();
                      navigate(config.anotherActionPath);
                    }}
                  >
                    {config.anotherActionLabel}
                  </Button>
                  {result.resolutionSummary.pendientes > 0 && (
                    <Button variant="secondary" onClick={() => navigate(`/profesional/turnos-afectados?excepcion=${result?.excepcion?.id || ''}`)}>
                      Resolver turnos pendientes
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}
      {section === 'exceptions' && <ExceptionsPanel onRegister={()=>navigate('/profesional/ausencias/registrar')}/>}
      {section === 'affected' && <AffectedAppointmentsPanel/>}

      <RescheduleDialog turno={rescheduling} onClose={()=>setRescheduling(null)} onConfirm={resolveReschedule}/>
    </div>
  );
}

function Summary({label,value}) { return <div className="rounded-xl border p-4"><span className="text-xs text-muted-foreground">{label}</span><strong className="mt-1 block text-2xl">{value}</strong></div>; }

function RescheduleDialog({ turno, onClose, onConfirm }) {
  const [form, setForm] = useState({ fecha:'', horaInicio:'', horaFin:'', observacion:'' });
  const { data: days } = useSelectableDays(form.fecha, form.fecha);
  const selectedDay = days?.find((day) => day.fecha === form.fecha && day.seleccionable);
  const complete = form.fecha && selectedDay && form.horaInicio && form.horaFin && form.horaInicio < form.horaFin;
  return <ConfirmDialog open={Boolean(turno)} onOpenChange={(open)=>!open&&onClose()} title="Reprogramar turno" description="Elegí libremente una fecha y hora. Se validarán la disponibilidad efectiva y la capacidad." confirmLabel="Usar nuevo horario" onConfirm={()=>onConfirm({...form,diaAgendaId:selectedDay?.diaAgendaId})} confirmDisabled={!complete}>
    <div className="grid gap-3 py-2 sm:grid-cols-2">
      <div className="sm:col-span-2">
        <Label>Fecha</Label>
        <Input type="date" value={form.fecha} onChange={(e)=>setForm({...form,fecha:e.target.value})}/>
        {form.fecha && days && !selectedDay && <p className="mt-1 text-xs text-destructive">Ese día no está habilitado para recibir turnos.</p>}
      </div>
      <div>
        <Label>Desde</Label>
        <TimeWheelPicker
          value={form.horaInicio}
          onChange={(val) => setForm({ ...form, horaInicio: val })}
          minuteStep={15}
          aria-label="Hora de inicio para reprogramar"
        />
      </div>
      <div>
        <Label>Hasta</Label>
        <TimeWheelPicker
          value={form.horaFin}
          onChange={(val) => setForm({ ...form, horaFin: val })}
          minuteStep={15}
          aria-label="Hora de fin para reprogramar"
        />
      </div>
      <div className="sm:col-span-2">
        <Label>Observación opcional</Label>
        <Textarea value={form.observacion} onChange={(e)=>setForm({...form,observacion:e.target.value})}/>
      </div>
    </div>
  </ConfirmDialog>;
}
