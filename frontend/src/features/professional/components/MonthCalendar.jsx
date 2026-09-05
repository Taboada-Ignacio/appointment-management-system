import { useMemo, useRef, useState } from 'react';
import { getDaysInMonth, MONTH_NAMES, isToday, isPast, toDateString, getFirstDayOfWeek } from '../../../utils/dates';
import { normalizeStatus } from '../../../utils/status';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { SkeletonCalendar } from '../../../components/ui/LoadingSkeleton';
import { cn } from '@/lib/utils';
import { Card, CardContent } from '@/components/ui/card';

const WEEKDAYS = ['LUN.', 'MAR.', 'MIÉ.', 'JUE.', 'VIE.', 'SÁB.', 'DOM.'];

const EXCEPTION_LABELS = {
  VACACIONES: 'Vacaciones',
  DIA_NO_LABORABLE: 'No laborable',
  BLOQUEO_HORARIO: 'Bloqueo de Horario',
  HABILITACION_EXTRAORDINARIA: 'Habilitación extra',
  MODIFICACION_HORARIO: 'Horario modificado',
  EXCEPCION_HORARIA: 'Excepción horaria',
};

export function MonthCalendar({
  year,
  month,
  days = [],
  selectedDayId = null,
  onSelectDay = null,
  loading = false,
}) {
  const numDays = getDaysInMonth(year, month);
  const dayButtonRefs = useRef(new Map());
  const [focusState, setFocusState] = useState(null);

  const calendarCells = useMemo(() => {
    const cells = [];
    // 0 = Monday, 1 = Tuesday, ..., 6 = Sunday (matching the LUN.-DOM. template)
    const firstDayOffset = getFirstDayOfWeek(year, month);

    // Leading empty cells for days before the 1st of the month
    for (let i = 0; i < firstDayOffset; i++) {
      cells.push({ type: 'empty', id: `empty-lead-${i}` });
    }

    // Days of the month
    for (let dayNum = 1; dayNum <= numDays; dayNum++) {
      const dateStr = toDateString(year, month, dayNum);
      const dayData = days.find((d) => d.fecha === dateStr);
      cells.push({
        type: 'day',
        id: dayData?.id ? String(dayData.id) : dateStr,
        dayNumber: dayNum,
        dateStr,
        data: dayData,
      });
    }

    // Trailing empty cells to complete the last row of 7 columns
    const remainder = cells.length % 7;
    if (remainder !== 0) {
      const trailingCount = 7 - remainder;
      for (let i = 0; i < trailingCount; i++) {
        cells.push({ type: 'empty', id: `empty-trail-${i}` });
      }
    }

    return cells;
  }, [year, month, days, numDays]);

  const defaultFocusDate = useMemo(() => {
    const selectedCell = calendarCells.find(
      (cell) =>
        cell.type === 'day' &&
        (String(selectedDayId) === String(cell.id) ||
          String(selectedDayId) === String(cell.data?.id) ||
          String(selectedDayId) === cell.dateStr)
    );
    if (selectedCell) return selectedCell.dateStr;

    const todayCell = calendarCells.find(
      (cell) => cell.type === 'day' && isToday(cell.dateStr)
    );
    return todayCell?.dateStr || toDateString(year, month, 1);
  }, [calendarCells, month, selectedDayId, year]);

  const focusScope = `${year}-${month}-${selectedDayId ?? ''}`;

  const activeFocusDate = calendarCells.some(
    (cell) =>
      cell.type === 'day' &&
      focusState?.scope === focusScope &&
      cell.dateStr === focusState.date
  )
    ? focusState.date
    : defaultFocusDate;

  if (loading) {
    return <SkeletonCalendar className="rounded-xl border bg-card p-4 sm:p-6" />;
  }

  const handleKeyDown = (e, cell) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSelectDay?.(cell.data || { id: cell.id, fecha: cell.dateStr, empty: true });
      return;
    }

    const movement = {
      ArrowLeft: -1,
      ArrowRight: 1,
      ArrowUp: -7,
      ArrowDown: 7,
    }[e.key];
    if (!movement) return;

    e.preventDefault();
    const targetDay = cell.dayNumber + movement;
    if (targetDay < 1 || targetDay > numDays) return;

    const targetDate = toDateString(year, month, targetDay);
    setFocusState({ scope: focusScope, date: targetDate });
    dayButtonRefs.current.get(targetDate)?.focus();
  };

  return (
    <div className="w-full space-y-3">
      {/* Month title styled like reference template */}
      <div className="flex items-center justify-between">
        <h2 className="font-heading text-xl font-bold tracking-tight text-primary dark:text-foreground sm:text-2xl capitalize">
          {MONTH_NAMES[month - 1]} de {year}
        </h2>
      </div>

      <Card className="w-full overflow-hidden rounded-xl border border-border/90 bg-card p-0 shadow-xs">
        <CardContent className="p-0">
          {/* Header row: solid dark navy bar with vertical separators */}
          <div className="grid grid-cols-7 border-b border-primary/90 bg-[#16293d] text-white dark:bg-[#0d1c2b]">
            {WEEKDAYS.map((dayName, idx) => (
              <div
                key={idx}
                role="columnheader"
                className={cn(
                  'py-2.5 text-center text-[11px] font-bold uppercase tracking-wider text-white sm:text-xs',
                  idx < 6 && 'border-r border-white/20'
                )}
              >
                {dayName}
              </div>
            ))}
          </div>

          {/* Unified continuous calendar table grid */}
          <div
            className="grid grid-cols-7 border-l border-t border-border/80"
            role="grid"
            aria-label="Calendario mensual de disponibilidad"
          >
            {calendarCells.map((cell) => {
              if (cell.type === 'empty') {
                return (
                  <div
                    key={cell.id}
                    className="min-h-24 border-b border-r border-border/80 bg-[#dbe3eb]/85 dark:bg-[#182937]/70 sm:min-h-28"
                    role="gridcell"
                    aria-hidden="true"
                  />
                );
              }

              const isSelected =
                String(selectedDayId) === String(cell.id) ||
                String(selectedDayId) === String(cell.data?.id);
              const isTodayDate = isToday(cell.dateStr);
              const isPastDate = isPast(cell.dateStr);
              const gapCount = Number(cell.data?.cantidadBrechas ?? cell.data?.brechas?.length ?? 0);
              const appointmentCount = Number(cell.data?.cantidadTurnosAsignados ?? 0);
              const rawExceptions = cell.data?.tiposExcepcion ?? cell.data?.excepciones ?? [];
              const exceptionTypes = Array.isArray(rawExceptions)
                ? rawExceptions
                    .filter((item) => typeof item === 'string' || item?.activa !== false)
                    .map((item) => (typeof item === 'string' ? item : item?.tipo || String(item)))
                : [];
              const hasException =
                exceptionTypes.length > 0 ||
                Boolean(cell.data?.tieneExcepcion) ||
                Boolean(cell.data?.excepcion);
              const hasReducedDay = exceptionTypes.some((type) =>
                type === 'BLOQUEO_HORARIO' || type === 'EXCEPCION_HORARIA'
              );
              const hasModifiedDay = exceptionTypes.some((type) =>
                type === 'MODIFICACION_HORARIO'
              );
              const visibleExceptionTypes = exceptionTypes.filter(
                (type) =>
                  type !== 'BLOQUEO_HORARIO' &&
                  type !== 'EXCEPCION_HORARIA' &&
                  type !== 'MODIFICACION_HORARIO'
              );
              const hasExtraordinary = exceptionTypes.includes('HABILITACION_EXTRAORDINARIA');
              const paintsAbsenceBackground =
                exceptionTypes.some(
                  (type) =>
                    type !== 'BLOQUEO_HORARIO' &&
                    type !== 'EXCEPCION_HORARIA' &&
                    type !== 'HABILITACION_EXTRAORDINARIA' &&
                    type !== 'MODIFICACION_HORARIO'
                ) ||
                (hasException &&
                  !hasExtraordinary &&
                  !hasReducedDay &&
                  !hasModifiedDay &&
                  exceptionTypes.length === 0);
              const status = cell.data ? normalizeStatus(cell.data.estadoActual) : null;
              const isActive = status === 'ACTIVO';
              const isInactive = !cell.data || status === 'INACTIVO';
              const compactStatus = status
                ? {
                    ACTIVO: 'A',
                    INACTIVO: 'I',
                    EN_TRANSCURSO: 'E',
                    FINALIZADO: 'F',
                  }[status] || 'I'
                : null;
              const accessibleStatus = status ? ` Estado: ${status.toLowerCase().replace('_', ' ')}.` : ' Sin datos de agenda.';
              const accessibleSummary = cell.data
                ? ` ${gapCount} brechas horarias. ${appointmentCount} turnos asignados.${exceptionTypes.length ? ` Excepciones: ${exceptionTypes.map((type) => EXCEPTION_LABELS[type] || type).join(', ')}.` : ''}`
                : '';

              let dayThemeClasses;
              if (hasExtraordinary) {
                // Fondo verde agua (emerald) para Habilitación Extraordinaria (prevalece sobre inactivo y ausencias)
                dayThemeClasses = isSelected
                  ? 'border-emerald-500 bg-emerald-500/35 shadow-sm ring-2 ring-inset ring-ring z-10 text-foreground'
                  : 'bg-emerald-500/20 hover:bg-emerald-500/30 dark:bg-emerald-950/40 dark:hover:bg-emerald-950/60 text-foreground';
              } else if (paintsAbsenceBackground) {
                // Fondo naranja si el día posee una excepción de ausencia / cierre
                dayThemeClasses = isSelected
                  ? 'border-warning bg-warning/35 shadow-sm ring-2 ring-inset ring-ring z-10 text-foreground'
                  : 'bg-warning/25 hover:bg-warning/35 text-foreground';
              } else if (isInactive) {
                // Fondo gris si el estado del día es inactivo (mismo gris que las casillas fuera del mes)
                dayThemeClasses = isSelected
                  ? 'bg-[#dbe3eb] dark:bg-[#182937] shadow-sm ring-2 ring-inset ring-ring z-10 text-muted-foreground'
                  : 'bg-[#dbe3eb]/85 hover:bg-[#dbe3eb] dark:bg-[#182937]/70 dark:hover:bg-[#182937] text-muted-foreground';
              } else {
                // Todos los demás días (activos) en blanco
                dayThemeClasses = isSelected
                  ? 'bg-accent/40 shadow-sm ring-2 ring-inset ring-ring z-10 text-foreground'
                  : 'bg-white hover:bg-muted/30 dark:bg-card dark:hover:bg-muted/20 text-foreground';
              }

              return (
                <button
                  key={cell.id}
                  ref={(node) => {
                    if (node) dayButtonRefs.current.set(cell.dateStr, node);
                    else dayButtonRefs.current.delete(cell.dateStr);
                  }}
                  type="button"
                  role="gridcell"
                  tabIndex={cell.dateStr === activeFocusDate ? 0 : -1}
                  onKeyDown={(e) => handleKeyDown(e, cell)}
                  onFocus={() => setFocusState({ scope: focusScope, date: cell.dateStr })}
                  onClick={() => onSelectDay?.(cell.data || { id: cell.id, fecha: cell.dateStr, empty: true })}
                  aria-selected={isSelected}
                  aria-label={`${cell.dayNumber} de ${MONTH_NAMES[month - 1]} de ${year}.${accessibleStatus}${accessibleSummary}`}
                  data-status={status}
                  data-today={isTodayDate ? 'true' : 'false'}
                  data-has-exception={hasException ? 'true' : 'false'}
                  data-has-extraordinary={hasExtraordinary ? 'true' : 'false'}
                  data-has-modified={hasModifiedDay ? 'true' : 'false'}
                  className={cn(
                    'relative flex min-h-24 flex-col justify-between p-2 text-left outline-none transition-all sm:min-h-28',
                    'border-b border-r border-border/80',
                    dayThemeClasses,
                    isPastDate && 'opacity-70'
                  )}
                >
                  <div className="flex w-full items-start justify-between">
                    <span
                      className={cn(
                        'font-heading text-xs font-bold sm:text-sm',
                        hasExtraordinary
                          ? 'text-emerald-700 dark:text-emerald-300 font-black'
                          : paintsAbsenceBackground
                          ? 'text-warning font-black'
                          : isInactive
                          ? 'text-muted-foreground font-semibold'
                          : isTodayDate
                          ? 'text-info font-black'
                          : 'text-foreground font-bold'
                      )}
                    >
                      {cell.dayNumber}
                    </span>
                    {isTodayDate && (
                      <span className="rounded bg-info px-1.5 py-0.5 text-[8px] font-black uppercase text-info-foreground sm:text-[9px]">
                        Hoy
                      </span>
                    )}
                  </div>

                  <div className="mt-auto flex w-full flex-col gap-1">
                    {hasReducedDay && (
                      <span className="max-w-full self-start whitespace-normal rounded bg-orange-500 px-1 py-0.5 text-center text-[6px] font-black uppercase leading-none tracking-tight text-white sm:text-[8px]" title="La jornada tiene una o más franjas bloqueadas">
                        Jornada reducida
                      </span>
                    )}
                    {hasModifiedDay && (
                      <span className="max-w-full self-start whitespace-normal rounded bg-orange-500 px-1 py-0.5 text-center text-[6px] font-black uppercase leading-none tracking-tight text-white sm:text-[8px]" title="La jornada tiene un horario de atención modificado">
                        Horario Modificado
                      </span>
                    )}
                    {status && (
                      <>
                        <span
                          className={cn(
                            'grid size-4 place-items-center rounded-full border text-[8px] font-black sm:hidden',
                            isActive
                              ? 'border-info/40 bg-info/25 text-info'
                              : 'border-border bg-muted text-muted-foreground'
                          )}
                          aria-hidden="true"
                        >
                          {compactStatus}
                        </span>
                        <div className="hidden origin-left scale-90 sm:block">
                          <StatusBadge status={status} />
                        </div>
                      </>
                    )}

                    {cell.data && (
                      <div className="space-y-0.5 text-[9px] font-semibold text-foreground/75 sm:text-[10px]">
                        <span className="block">{gapCount} {gapCount === 1 ? 'brecha' : 'brechas'}</span>
                        <span className="block">{appointmentCount} {appointmentCount === 1 ? 'turno' : 'turnos'}</span>
                      </div>
                    )}
                    {visibleExceptionTypes.length > 0 && (
                      <div className="flex flex-wrap gap-1">
                        {visibleExceptionTypes.map((type) => {
                          const isExtraordinary = type === 'HABILITACION_EXTRAORDINARIA';
                          return (
                            <span
                              key={type}
                              className={cn(
                                'max-w-full truncate rounded px-1 py-0.5 text-[8px] font-bold sm:text-[9px]',
                                isExtraordinary
                                  ? 'border border-emerald-500/30 bg-emerald-50 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                                  : 'bg-warning/35 text-warning-foreground'
                              )}
                              title={EXCEPTION_LABELS[type] || type}
                            >
                              {EXCEPTION_LABELS[type] || type}
                            </span>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </button>
              );
            })}
          </div>

          {/* Bottom NOTAS and references bar styled after reference template */}
          <div className="border-t border-border/90 bg-muted/20 p-3 sm:p-4">
            <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
              <div className="flex items-center gap-2">
                <span className="font-heading text-xs font-black tracking-[0.2em] text-primary uppercase dark:text-primary-foreground">
                  NOTAS
                </span>
                <span className="hidden text-muted-foreground/50 sm:inline">|</span>
                <span className="font-semibold text-foreground text-[11px] uppercase tracking-wider">
                  Referencias:
                </span>
              </div>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-muted-foreground">
                <div className="flex items-center gap-1.5">
                  <span className="size-3 rounded-xs border border-border/80 bg-[#dbe3eb]/85 dark:bg-[#182937]/70" aria-hidden="true" />
                  <span>Día inactivo</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="size-3 rounded-xs border border-border/80 bg-white dark:bg-card" aria-hidden="true" />
                  <span>Día activo</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="size-3 rounded-xs border border-success/50 bg-success/25" aria-hidden="true" />
                  <span>Día actual</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="size-3 rounded-xs border border-warning/50 bg-warning/25" aria-hidden="true" />
                  <span>Ausencia / No laborable</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="size-3 rounded-xs border border-emerald-500/50 bg-emerald-500/25" aria-hidden="true" />
                  <span>Habilitación extraordinaria</span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
