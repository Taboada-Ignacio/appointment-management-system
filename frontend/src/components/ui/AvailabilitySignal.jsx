import { useMemo } from 'react';
import { cn } from '@/lib/utils';
import { gapsToSignalSegments, timeToMinutes } from '../../utils/gaps';

export const AvailabilitySignal = ({ 
  brechas = [], 
  bloqueos = [],
  bloqueosHorario = [],
  habilitaciones = [],
  habilitacionesExtraordinarias = [],
  asignados = [],
  dayStart = '07:00', 
  dayEnd = '21:00', 
  variant = 'compact',
  totalMinutes = undefined,
  maxMinutes = 0,
  dayCount = 0,
  configuredDayCount = 0,
  className = ''
}) => {
  const isSummary = totalMinutes !== undefined;
  const effectiveBloqueos = (bloqueos && bloqueos.length > 0) ? bloqueos : (bloqueosHorario || []);
  const effectiveHabilitaciones = (habilitaciones && habilitaciones.length > 0)
    ? habilitaciones
    : (habilitacionesExtraordinarias || []);

  const segments = useMemo(() => 
    isSummary ? [] : gapsToSignalSegments(brechas, dayStart, dayEnd, effectiveBloqueos, effectiveHabilitaciones),
  [isSummary, brechas, dayStart, dayEnd, effectiveBloqueos, effectiveHabilitaciones]);

  if (isSummary) {
    const percentage = maxMinutes > 0 ? (totalMinutes / maxMinutes) * 100 : 0;
    return (
      <div 
        className={cn("w-full flex flex-col gap-1", className)}
        role="img"
        aria-label={`Disponibilidad: ${Math.round(percentage)}% (${dayCount} de ${configuredDayCount} días consultados tienen brechas)`}
      >
        <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted">
          <div 
            className="h-full rounded-full bg-ring transition-[width] duration-300"
            style={{ width: `${Math.min(100, Math.max(0, percentage))}%` }}
          />
        </div>
      </div>
    );
  }

  const startMin = timeToMinutes(dayStart);
  const endMin = timeToMinutes(dayEnd);
  const totalMin = endMin - startMin;
  const assignedSegments = (asignados || [])
    .filter((turno) => turno?.horaInicio && turno?.horaFin)
    .map((turno) => ({
      start: Math.max(startMin, timeToMinutes(turno.horaInicio)),
      end: Math.min(endMin, timeToMinutes(turno.horaFin)),
    }))
    .filter((turno) => turno.end > turno.start);

  return (
    <div 
      className={cn("w-full flex flex-col", className)}
      role="img"
      aria-label="Escala de disponibilidad del día"
      tabIndex={variant === 'detailed' ? 0 : undefined}
    >
      <div className={cn(
        "relative flex w-full overflow-hidden rounded-full bg-muted",
        variant === 'compact' ? "h-2.5" : "h-5"
      )}>
        {segments.map((seg, idx) => {
          const segStart = timeToMinutes(seg.start);
          const segEnd = timeToMinutes(seg.end);
          const width = ((segEnd - segStart) / totalMin) * 100;
          
          let segColorClass = "bg-muted";
          let segTitle = `Ocupado/Inactivo: ${seg.start} - ${seg.end}`;

          if (seg.isBlocked || seg.status === 'blocked') {
            segColorClass = "bg-orange-500 hover:brightness-95";
            segTitle = `Bloqueado: ${seg.start} - ${seg.end}`;
          } else if (seg.isExtraordinary || seg.status === 'extraordinary') {
            segColorClass = "bg-emerald-500 hover:brightness-95";
            segTitle = `Habilitación extraordinaria: ${seg.start} - ${seg.end}`;
          } else if (seg.isAvailable || seg.status === 'available') {
            segColorClass = "bg-ring hover:brightness-95";
            segTitle = `Disponible: ${seg.start} - ${seg.end}`;
          }

          return (
            <div
              key={idx}
              style={{ width: `${width}%` }}
              className={cn(
                "h-full transition-colors group relative",
                segColorClass
              )}
              title={segTitle}
            />
          );
        })}
        {assignedSegments.map((turno, index) => (
          <div
            key={`assigned-${turno.start}-${turno.end}-${index}`}
            className="absolute inset-y-0 z-10 bg-sky-600 hover:brightness-95"
            style={{
              left: `${((turno.start - startMin) / totalMin) * 100}%`,
              width: `${((turno.end - turno.start) / totalMin) * 100}%`,
            }}
            title={`Turno asignado: ${minutesToLabel(turno.start)} - ${minutesToLabel(turno.end)}`}
          />
        ))}
      </div>
      {variant === 'detailed' && (
        <div className="mt-1.5 flex justify-between px-1 text-[10px] font-medium text-muted-foreground" aria-hidden="true">
          <span>{dayStart}</span>
          <span>{dayEnd}</span>
        </div>
      )}
    </div>
  );
};

function minutesToLabel(minutes) {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return `${String(hours).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`;
}
