import { useEffect, useState } from 'react';
import { isToday, formatDateLong } from '../../../utils/dates';
import { formatTimeRange, timeToMinutes } from '../../../utils/gaps';
import { deriveTemporalStatus } from '../../../utils/status';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { AvailabilitySignal } from '../../../components/ui/AvailabilitySignal';
import { IntegrationNotice } from '../../../components/ui/IntegrationNotice';
import { EmptyState } from '../../../components/ui/EmptyState';
import { professionalContext } from '../../../config/professional';
import { CalendarClock, Edit3 } from 'lucide-react';
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

export function DailyTimeline({
  day = null,
  timezone = professionalContext.timezone,
  onEditGaps = null,
  canEdit = false,
}) {
  const [currentTimeMinutes, setCurrentTimeMinutes] = useState(() => currentMinutesInTimezone(timezone));

  const isTodayDay = Boolean(day?.fecha && isToday(day.fecha, timezone));

  useEffect(() => {
    if (!isTodayDay) return;

    const interval = setInterval(() => {
      setCurrentTimeMinutes(currentMinutesInTimezone(timezone));
    }, 60000);

    return () => clearInterval(interval);
  }, [isTodayDay, timezone]);

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
  const derivedStatus = deriveTemporalStatus(estadoActual, fecha, timezone);

  // Dynamic range calculation:
  // Starts at the first gap's hour and finishes one hour after the last gap's hour
  let startHour = 8;
  let endHour = 18;

  const intervalosVisibles = [...brechas, ...bloqueosHorario];
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
  const timelineHeight = Math.max(440, (endHour - startHour) * 55);

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
    return brechas.map((gap, idx) => {
      const startMin = timeToMinutes(gap.horaInicio);
      const endMin = timeToMinutes(gap.horaFin);

      const adjustedStart = Math.max(startHour * 60, startMin);
      const adjustedEnd = Math.min(endHour * 60, endMin);

      if (adjustedEnd <= startHour * 60 || adjustedStart >= endHour * 60) return null;

      const topPerc = ((adjustedStart - startHour * 60) / totalMinutes) * 100;
      const heightPerc = ((adjustedEnd - adjustedStart) / totalMinutes) * 100;

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

      <IntegrationNotice title="Consulta de turnos no disponible">
        El backend no expone un endpoint para listar los turnos asignados a este día. La visualización se limita a las brechas horarias de atención configuradas.
      </IntegrationNotice>

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
          brechas={brechas}
          dayStart={displayStartHour}
          dayEnd={displayEndHour}
          variant="detailed"
        />
        {bloqueosHorario.length > 0 && <div className="mt-3 flex flex-wrap gap-2">{bloqueosHorario.map((gap,index)=><span key={`${gap.horaInicio}-${gap.horaFin}-${index}`} className="rounded-md bg-orange-500 px-2 py-1 text-[10px] font-bold text-white">Bloqueado {formatTimeRange(gap.horaInicio,gap.horaFin)}</span>)}</div>}
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
            className="relative min-h-[440px] rounded-xl bg-card"
            style={{ height: `${timelineHeight}px` }}
          >
            {renderHourLines()}
            {renderGaps()}
            {renderBlockedGaps()}

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
