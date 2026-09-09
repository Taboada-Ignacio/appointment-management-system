import { useEffect, useRef, useState } from 'react';
import { isToday, formatDateLong } from '../../../utils/dates';
import { formatTimeRange, timeToMinutes, subtractGaps } from '../../../utils/gaps';
import { deriveTemporalStatus } from '../../../utils/status';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { AvailabilitySignal } from '../../../components/ui/AvailabilitySignal';
import { IntegrationNotice } from '../../../components/ui/IntegrationNotice';
import { EmptyState } from '../../../components/ui/EmptyState';
import { professionalContext } from '../../../config/professional';
import { CalendarClock, Edit3, Maximize, Minus, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { ScrollArea } from '@/components/ui/scroll-area';

function currentMinutesInTimezone(timezone) {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date());
  const hour = Number(parts.find((part) => part.type === 'hour')?.value ?? 0);
  const minute = Number(parts.find((part) => part.type === 'minute')?.value ?? 0);
  return hour * 60 + minute;
}

const ZOOM_LEVELS = [50, 75, 100, 125, 150, 200, 250, 300];
const nextZoom = (zoom, direction) => {
  const currentIndex = ZOOM_LEVELS.findIndex((level) => level >= zoom);
  const nextIndex = direction > 0
    ? Math.min(ZOOM_LEVELS.length - 1, currentIndex + 1)
    : Math.max(0, (currentIndex < 0 ? ZOOM_LEVELS.length : currentIndex) - 1);
  return ZOOM_LEVELS[nextIndex];
};

function instantTime(value, timezone) {
  if (!value) return '';
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(value));
}

export function DailyTimeline({
  day = null,
  timezone = professionalContext.timezone,
  onEditGaps = null,
  canEdit = false,
  showIntegrationNotice = true,
  candidateSlots = [],
  selectedCandidate = null,
  onSelectCandidate = null,
  zoom: controlledZoom,
  onZoomChange = null,
  appointments = [],
  selectedAppointmentId = null,
  onSelectAppointment = null,
}) {
  const [currentTimeMinutes, setCurrentTimeMinutes] = useState(() => currentMinutesInTimezone(timezone));
  const [internalZoom, setInternalZoom] = useState(100);
  const timelineRef = useRef(null);
  const zoom = controlledZoom ?? internalZoom;
  const setZoom = onZoomChange ?? setInternalZoom;

  const isTodayDay = Boolean(day?.fecha && isToday(day.fecha, timezone));

  useEffect(() => {
    if (!isTodayDay) return;

    const interval = setInterval(() => {
      setCurrentTimeMinutes(currentMinutesInTimezone(timezone));
    }, 60000);

    return () => clearInterval(interval);
  }, [isTodayDay, timezone]);

  useEffect(() => {
    const timeline = timelineRef.current;
    if (!timeline) return undefined;
    const handleWheel = (event) => {
      if (!event.ctrlKey) return;
      event.preventDefault();
      setZoom(nextZoom(zoom, event.deltaY < 0 ? 1 : -1));
    };
    timeline.addEventListener('wheel', handleWheel, { passive: false });
    return () => timeline.removeEventListener('wheel', handleWheel);
  }, [setZoom, zoom]);

  if (!day) {
    return (
      <EmptyState
        icon={CalendarClock}
        title="Sin día seleccionado"
        description="Elegí una fecha del calendario o navega para ver las brechas de atención."
      />
    );
  }

  const { fecha, estadoActual, brechas = [], bloqueosHorario = [] } = day;
  const rawExceptions = day.tiposExcepcion ?? day.excepciones ?? [];
  const exceptionTypes = Array.isArray(rawExceptions)
    ? rawExceptions
        .filter((item) => typeof item === 'string' || item?.activa !== false)
        .map((item) => (typeof item === 'string' ? item : item?.tipo || String(item)))
    : [];

  const rawHabilitaciones = day.habilitacionesExtraordinarias ?? day.habilitaciones ?? (
    Array.isArray(day.excepciones)
      ? day.excepciones
          .filter((e) => typeof e === 'object' && (e.tipo === 'HABILITACION_EXTRAORDINARIA' || e.tipoExcepcion === 'HABILITACION_EXTRAORDINARIA') && e.activa !== false)
          .flatMap((e) => e.intervalos || (e.horaInicio && e.horaFin ? [{ horaInicio: e.horaInicio, horaFin: e.horaFin }] : []))
      : []
  );
  const habilitaciones = Array.isArray(rawHabilitaciones) ? rawHabilitaciones : [];
  const hasExtraordinary = exceptionTypes.includes('HABILITACION_EXTRAORDINARIA') || habilitaciones.length > 0;

  const rawModificaciones = day.modificacionesHorarias ?? day.modificaciones ?? (
    Array.isArray(day.excepciones)
      ? day.excepciones
          .filter((e) => typeof e === 'object' && (e.tipo === 'MODIFICACION_HORARIO' || e.tipoExcepcion === 'MODIFICACION_HORARIO') && e.activa !== false)
          .flatMap((e) => e.intervalos || (e.horaInicio && e.horaFin ? [{ horaInicio: e.horaInicio, horaFin: e.horaFin }] : []))
      : []
  );
  const modificaciones = Array.isArray(rawModificaciones) ? rawModificaciones : [];

  const brechasBase = modificaciones.length > 0 ? modificaciones : brechas;
  const todasBrechas = brechasBase.length > 0 ? brechasBase : habilitaciones;
  const derivedStatus = deriveTemporalStatus(estadoActual, fecha, timezone);
  const brechasEfectivas = bloqueosHorario.length > 0
    ? subtractGaps(todasBrechas, bloqueosHorario)
    : todasBrechas;

  // Dynamic range calculation:
  // Starts at the first gap's hour and finishes one hour after the last gap's hour
  let startHour = 8;
  let endHour = 18;

  const appointmentSlots = appointments.map((appointment) => ({
    ...appointment,
    horaInicio: instantTime(appointment.inicioEstimado, timezone),
    horaFin: instantTime(appointment.finEstimado, timezone),
  })).filter((appointment) => appointment.horaInicio && appointment.horaFin);
  const intervalosVisibles = [...todasBrechas, ...bloqueosHorario, ...habilitaciones, ...candidateSlots, ...appointmentSlots];
  if (intervalosVisibles.length > 0) {
    const startMinutesList = intervalosVisibles.map((b) => timeToMinutes(b.horaInicio));
    const endMinutesList = intervalosVisibles.map((b) => timeToMinutes(b.horaFin));

    const minStartMinutes = Math.min(...startMinutesList);
    const maxEndMinutes = Math.max(...endMinutesList);

    // Comienza una hora antes de la primera franja horaria
    startHour = Math.max(0, Math.floor(minStartMinutes / 60) - 1);
    // Termina una hora después de la última franja
    endHour = Math.min(24, Math.ceil(maxEndMinutes / 60) + 1);

    if (endHour <= startHour) {
      endHour = Math.min(24, startHour + 1);
    }
  }

  const totalMinutes = (endHour - startHour) * 60;
  const displayStartHour = `${String(startHour).padStart(2, '0')}:00`;
  const displayEndHour = `${String(endHour).padStart(2, '0')}:00`;
  const timelineHeight = Math.max(440, (endHour - startHour) * 55) * (zoom / 100);
  const changeZoom = (direction) => {
    setZoom(nextZoom(zoom, direction));
  };

  const renderHourLines = () => {
    const lines = [];
    for (let h = startHour; h <= endHour; h++) {
      const topPerc = (((h - startHour) * 60) / totalMinutes) * 100;
      lines.push(
        <div
          key={h}
          className="absolute w-full flex items-center"
          style={{ top: `${topPerc}%` }}
        >
          <span className="z-10 -mt-2 w-12 bg-card pr-2 text-right text-[11px] font-semibold text-muted-foreground">
            {String(h).padStart(2, '0')}:00
          </span>
          <div className="flex-1 border-t border-dashed" />
        </div>
      );
    }
    return lines;
  };

  const renderGaps = () => {
    return brechasEfectivas.map((gap, idx) => {
      const startMin = timeToMinutes(gap.horaInicio);
      const endMin = timeToMinutes(gap.horaFin);

      const adjustedStart = Math.max(startHour * 60, startMin);
      const adjustedEnd = Math.min(endHour * 60, endMin);

      if (adjustedEnd <= startHour * 60 || adjustedStart >= endHour * 60) return null;

      const topPerc = ((adjustedStart - startHour * 60) / totalMinutes) * 100;
      const heightPerc = ((adjustedEnd - adjustedStart) / totalMinutes) * 100;

      const isExtraordinaryGap = habilitaciones.some((h) => {
        const hStart = timeToMinutes(h.horaInicio);
        const hEnd = timeToMinutes(h.horaFin);
        return Math.max(startMin, hStart) < Math.min(endMin, hEnd);
      });

      if (isExtraordinaryGap) {
        return (
          <div
            key={idx}
            className="absolute left-14 right-2 flex items-center overflow-hidden rounded-lg border border-emerald-500/40 bg-emerald-50/90 dark:bg-emerald-950/40 px-4 text-xs font-semibold text-emerald-950 dark:text-emerald-100 shadow-xs transition hover:bg-emerald-100/80"
            style={{
              top: `${topPerc}%`,
              height: `${heightPerc}%`,
              minHeight: '28px',
              borderLeft: '3px solid #10b981',
            }}
          >
            <div className="flex items-center gap-2 truncate">
              <span className="size-2 shrink-0 rounded-full bg-emerald-500" />
              <span className="font-heading tracking-tight">
                {formatTimeRange(gap.horaInicio, gap.horaFin)}
              </span>
              <span className="hidden text-[11px] font-normal text-emerald-700 dark:text-emerald-300 sm:inline">
                · Habilitación extraordinaria disponible
              </span>
            </div>
          </div>
        );
      }

      return (
        <div
          key={idx}
          className="absolute left-14 right-2 flex items-center overflow-hidden rounded-lg border border-ring/20 bg-accent/80 px-4 text-xs font-semibold text-accent-foreground shadow-xs transition hover:bg-accent"
          style={{
            top: `${topPerc}%`,
            height: `${heightPerc}%`,
            minHeight: '28px',
            borderLeft: '3px solid var(--ring)',
          }}
        >
          <div className="flex items-center gap-2 truncate">
            <span className="size-2 shrink-0 rounded-full bg-ring" />
            <span className="font-heading tracking-tight">
              {formatTimeRange(gap.horaInicio, gap.horaFin)}
            </span>
            <span className="hidden text-[11px] font-normal text-muted-foreground sm:inline">
              · Franja de atención disponible
            </span>
          </div>
        </div>
      );
    });
  };

  const renderBlockedGaps = () => bloqueosHorario.map((gap, idx) => {
    const startMin = timeToMinutes(gap.horaInicio);
    const endMin = timeToMinutes(gap.horaFin);
    const adjustedStart = Math.max(startHour * 60, startMin);
    const adjustedEnd = Math.min(endHour * 60, endMin);
    if (adjustedEnd <= startHour * 60 || adjustedStart >= endHour * 60) return null;
    return (
      <div key={`blocked-${idx}`} className="absolute left-14 right-2 z-10 flex items-center overflow-hidden rounded-lg border border-orange-600 bg-orange-500/90 px-4 text-xs font-semibold text-white shadow-sm" style={{top:`${((adjustedStart-startHour*60)/totalMinutes)*100}%`,height:`${((adjustedEnd-adjustedStart)/totalMinutes)*100}%`,minHeight:'28px',borderLeft:'3px solid #c2410c'}}>
        <div className="flex items-center gap-2 truncate"><span className="size-2 shrink-0 rounded-full bg-white"/><span className="font-heading tracking-tight">{formatTimeRange(gap.horaInicio,gap.horaFin)}</span><span className="hidden text-[11px] font-normal text-white/90 sm:inline">· Horario bloqueado</span></div>
      </div>
    );
  });

  const renderCandidates = () => candidateSlots.map((slot, idx) => {
    const startMin = timeToMinutes(slot.horaInicio);
    const endMin = timeToMinutes(slot.horaFin);
    const adjustedStart = Math.max(startHour * 60, startMin);
    const adjustedEnd = Math.min(endHour * 60, endMin);
    if (adjustedEnd <= startHour * 60 || adjustedStart >= endHour * 60) return null;
    const selected = selectedCandidate?.horaInicio === slot.horaInicio
      && selectedCandidate?.horaFin === slot.horaFin;
    const warnings = slot.advertencias || [];
    const warningText = warnings.map((warning) => {
      if (warning === 'HORARIO_FUERA_DE_BRECHA') return 'El horario está fuera de las franjas horarias configuradas.';
      if (warning === 'CAPACIDAD_SUPERADA') return 'Se superará la capacidad simultánea.';
      return warning;
    }).join(' ');
    const label = `${formatTimeRange(slot.horaInicio, slot.horaFin)}. Ocupación ${slot.turnosConcurrentes}/${slot.capacidadSimultanea}.${warningText ? ` ${warningText}` : ''}`;
    return (
      <button
        type="button"
        key={`candidate-${slot.horaInicio}-${slot.horaFin}-${idx}`}
        aria-label={label}
        aria-pressed={selected}
        onClick={() => onSelectCandidate?.(slot)}
        className={`absolute left-16 right-3 z-20 flex flex-col items-start justify-center overflow-hidden rounded-md border px-2 text-left text-[10px] font-semibold shadow-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${selected ? 'border-primary bg-primary text-primary-foreground' : warnings.length ? 'border-amber-500 bg-amber-100 text-amber-950 hover:bg-amber-200 dark:bg-amber-950 dark:text-amber-100' : 'border-primary/50 bg-background/95 text-foreground hover:bg-accent'}`}
        style={{ top: `${((adjustedStart - startHour * 60) / totalMinutes) * 100}%`, height: `${((adjustedEnd - adjustedStart) / totalMinutes) * 100}%`, minHeight: '30px' }}
      >
        <span>{formatTimeRange(slot.horaInicio, slot.horaFin)}</span>
        <span className="font-normal">Ocupación {slot.turnosConcurrentes}/{slot.capacidadSimultanea}</span>
      </button>
    );
  });

  const renderAppointments = () => appointmentSlots.map((appointment, idx) => {
    const startMin = timeToMinutes(appointment.horaInicio);
    const endMin = timeToMinutes(appointment.horaFin);
    const adjustedStart = Math.max(startHour * 60, startMin);
    const adjustedEnd = Math.min(endHour * 60, endMin);
    if (adjustedEnd <= startHour * 60 || adjustedStart >= endHour * 60) return null;
    const patient = appointment.cliente
      ? `${appointment.cliente.nombre || ''} ${appointment.cliente.apellido || ''}`.trim()
      : 'Cliente';
    const selected = String(selectedAppointmentId) === String(appointment.id);
    const AppointmentElement = onSelectAppointment ? 'button' : 'div';
    return (
      <AppointmentElement
        type={onSelectAppointment ? 'button' : undefined}
        key={`appointment-${appointment.id ?? idx}`}
        onClick={onSelectAppointment ? () => onSelectAppointment(appointment) : undefined}
        aria-pressed={onSelectAppointment ? selected : undefined}
        className={`absolute left-16 right-3 z-15 flex flex-col justify-center overflow-hidden rounded-md border px-2 text-left text-[10px] font-semibold shadow-sm transition ${selected ? 'border-primary bg-primary text-primary-foreground ring-2 ring-primary/30' : 'border-sky-600 bg-sky-100 text-sky-950 dark:bg-sky-950 dark:text-sky-100'} ${onSelectAppointment ? 'cursor-pointer hover:brightness-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring' : ''}`}
        style={{ top: `${((adjustedStart - startHour * 60) / totalMinutes) * 100}%`, height: `${((adjustedEnd - adjustedStart) / totalMinutes) * 100}%`, minHeight: '30px' }}
        aria-label={`Turno asignado de ${patient}, ${formatTimeRange(appointment.horaInicio, appointment.horaFin)}`}
      >
        <span>{formatTimeRange(appointment.horaInicio, appointment.horaFin)}</span>
        <span className="truncate font-normal">{patient} · Asignado</span>
      </AppointmentElement>
    );
  });

  return (
    <Card className="shadow-none">
      <CardContent className="flex flex-col gap-5 p-5 sm:p-6">
      <div className="flex flex-col items-start justify-between gap-3 border-b pb-4 sm:flex-row sm:items-center">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-info">Cronograma del día</p>
          <div className="flex items-center gap-3 mt-1 flex-wrap">
            <h3 className="font-heading text-lg font-semibold tracking-tight">
              {formatDateLong(fecha, timezone)}
            </h3>
            <StatusBadge status={derivedStatus} />
          </div>
        </div>

        {canEdit && onEditGaps && (
          <Button
            type="button"
            onClick={() => onEditGaps(day)}
            variant="outline"
          >
            <Edit3 className="h-3.5 w-3.5" />
            <span>Editar brechas del día</span>
          </Button>
        )}
      </div>

      {hasExtraordinary && (
        <div className="flex items-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-50 px-3.5 py-2.5 text-xs font-semibold text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300">
          <span className="size-2 shrink-0 rounded-full bg-emerald-500" aria-hidden="true" />
          <span>Día con habilitación extraordinaria de atención</span>
        </div>
      )}

      {showIntegrationNotice && <IntegrationNotice title="Consulta de turnos no disponible">
        El backend no expone un endpoint para listar los turnos asignados a este día. La visualización se limita a las brechas horarias de atención configuradas.
      </IntegrationNotice>}

      <section aria-labelledby="daily-availability-title" className="rounded-xl border bg-muted/25 p-4">
        <div className="mb-2 flex items-center justify-between gap-3">
          <h4 id="daily-availability-title" className="text-xs font-semibold">
            Señal de disponibilidad
          </h4>
          <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-muted-foreground">
            {displayStartHour}—{displayEndHour}
          </span>
        </div>
        <AvailabilitySignal
          brechas={todasBrechas}
          bloqueos={bloqueosHorario}
          habilitaciones={habilitaciones}
          asignados={appointmentSlots}
          dayStart={displayStartHour}
          dayEnd={displayEndHour}
          variant="detailed"
        />
        <div className="mt-3 flex items-center justify-end gap-1" aria-label={`Zoom del cronograma: ${zoom}%`}>
          <Button type="button" variant="outline" size="icon" aria-label="Reducir zoom" title="Reducir zoom" disabled={zoom <= 50} onClick={() => changeZoom(-1)}><Minus /></Button>
          <Button type="button" variant="outline" size="icon" aria-label="Restablecer zoom" title="Restablecer zoom" disabled={zoom === 100} onClick={() => setZoom(100)}><Maximize /></Button>
          <Button type="button" variant="outline" size="icon" aria-label="Aumentar zoom" title="Aumentar zoom" disabled={zoom >= 300} onClick={() => changeZoom(1)}><Plus /></Button>
        </div>
        {(bloqueosHorario.length > 0 || habilitaciones.length > 0) && (
          <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
            <div className="flex items-center gap-1.5">
              <span className="size-2.5 rounded-full bg-ring" aria-hidden="true" />
              <span>Disponible para turnos</span>
            </div>
            {habilitaciones.length > 0 && (
              <div className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-full bg-emerald-500" aria-hidden="true" />
                <span>Habilitación extraordinaria</span>
              </div>
            )}
            {bloqueosHorario.length > 0 && (
              <div className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-full bg-orange-500" aria-hidden="true" />
                <span>Bloqueo de horario</span>
              </div>
            )}
            {habilitaciones.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {habilitaciones.map((gap, index) => (
                  <span
                    key={`hab-${gap.horaInicio}-${gap.horaFin}-${index}`}
                    className="rounded-md bg-emerald-600 px-2 py-1 text-[10px] font-bold text-white"
                  >
                    Habilitado {formatTimeRange(gap.horaInicio, gap.horaFin)}
                  </span>
                ))}
              </div>
            )}
            {bloqueosHorario.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {bloqueosHorario.map((gap, index) => (
                  <span
                    key={`${gap.horaInicio}-${gap.horaFin}-${index}`}
                    className="rounded-md bg-orange-500 px-2 py-1 text-[10px] font-bold text-white"
                  >
                    Bloqueado {formatTimeRange(gap.horaInicio, gap.horaFin)}
                  </span>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      <ScrollArea className="relative mt-2 h-[min(68vh,44rem)] min-h-[440px] pr-3">
        {intervalosVisibles.length === 0 ? (
          <div className="py-12">
            <EmptyState
              icon={CalendarClock}
              title="Sin brechas de atención"
              description="No hay horarios de atención configurados para esta fecha."
              action={
                canEdit && onEditGaps
                  ? { label: 'Configurar horarios', onClick: () => onEditGaps(day) }
                  : null
              }
            />
          </div>
        ) : (
          <div
            data-testid="daily-timeline-grid"
            ref={timelineRef}
            className="relative min-h-[440px] rounded-xl bg-card"
            style={{ height: `${timelineHeight}px` }}
          >
            {renderHourLines()}
            {renderGaps()}
            {renderBlockedGaps()}
            {renderAppointments()}
            {renderCandidates()}

            {isTodayDay &&
              currentTimeMinutes >= startHour * 60 &&
              currentTimeMinutes <= endHour * 60 && (
                <div
                  className="absolute w-full flex items-center z-20 pointer-events-none"
                  style={{
                    top: `${((currentTimeMinutes - startHour * 60) / totalMinutes) * 100}%`,
                  }}
                >
                  <div className="w-12 flex justify-end pr-1">
                    <span className="-mt-0.5 size-2.5 rounded-full bg-destructive shadow-sm" />
                  </div>
                  <div className="flex-1 border-t-2 border-destructive shadow-xs" />
                </div>
              )}
          </div>
        )}
      </ScrollArea>
      </CardContent>
    </Card>
  );
}
