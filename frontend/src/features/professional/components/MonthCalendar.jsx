import { useMemo, useRef, useState } from 'react';
import { getDaysInMonth, getFirstDayOfWeek, DAY_NAMES_SHORT, MONTH_NAMES, isToday, isPast, toDateString } from '../../../utils/dates';
import { normalizeStatus } from '../../../utils/status';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { SkeletonCalendar } from '../../../components/ui/LoadingSkeleton';
import { cn } from '@/lib/utils';
import { Card, CardContent } from '@/components/ui/card';

export function MonthCalendar({
  year,
  month,
  days = [],
  selectedDayId = null,
  onSelectDay = null,
  loading = false,
}) {
  const numDays = getDaysInMonth(year, month);
  const firstDayOffset = getFirstDayOfWeek(year, month); // 0 = Monday, 6 = Sunday
  const dayButtonRefs = useRef(new Map());
  const [focusState, setFocusState] = useState(null);

  const calendarCells = useMemo(() => {
    const cells = [];
    // Add empty cells for offset
    for (let i = 0; i < firstDayOffset; i++) {
      cells.push({ type: 'empty', id: `empty-${i}` });
    }

    // Add days
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
    return cells;
  }, [year, month, days, numDays, firstDayOffset]);

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
    <Card className="w-full shadow-none">
      <CardContent className="p-4 sm:p-6">
      <div className="grid grid-cols-7 gap-1.5 mb-2.5">
        {DAY_NAMES_SHORT.map((dayName, idx) => (
          <div
            key={idx}
            role="columnheader"
            className="py-1 text-center text-[10px] font-bold uppercase tracking-[0.12em] text-info"
          >
            {dayName}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1.5" role="grid" aria-label="Calendario mensual de disponibilidad">
        {calendarCells.map((cell) => {
          if (cell.type === 'empty') {
            return <div key={cell.id} className="min-h-16 p-1 bg-transparent rounded-lg" role="gridcell" aria-hidden="true" />;
          }

          const isSelected =
            String(selectedDayId) === String(cell.id) ||
            String(selectedDayId) === String(cell.data?.id);
          const isTodayDate = isToday(cell.dateStr);
          const isPastDate = isPast(cell.dateStr);
          const gapCount = Number(cell.data?.cantidadBrechas ?? cell.data?.brechas?.length ?? 0);
          const hasGaps = gapCount > 0;
          const status = cell.data ? normalizeStatus(cell.data.estadoActual) : null;
          const compactStatus = {
            ACTIVO: 'A',
            INACTIVO: 'I',
            EN_TRANSCURSO: 'E',
            FINALIZADO: 'F',
          }[status];
          const accessibleStatus = status ? ` Estado: ${status.toLowerCase().replace('_', ' ')}.` : ' Sin datos de agenda.';

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
              aria-label={`${cell.dayNumber} de ${MONTH_NAMES[month - 1]} de ${year}.${accessibleStatus}`}
              className={cn(
                'flex min-h-20 flex-col justify-between rounded-lg border p-2 text-left outline-none transition-all focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 sm:min-h-24',
                isSelected
                  ? 'border-ring bg-accent/65 shadow-sm ring-1 ring-ring/25'
                  : 'bg-card hover:border-ring/60 hover:bg-muted/35',
                isTodayDate && !isSelected && 'border-info/55 bg-info/5 font-bold',
                isPastDate && 'bg-muted/25 opacity-65'
              )}
            >
              <div className="flex items-center justify-between w-full">
                <span
                  className={cn(
                    'font-heading text-xs font-semibold sm:text-sm',
                    isTodayDate ? 'text-info' : 'text-foreground'
                  )}
                >
                  {cell.dayNumber}
                </span>
                {isTodayDate && (
                  <>
                    <span className="hidden rounded bg-info px-1.5 py-0.5 text-[9px] font-bold uppercase text-info-foreground sm:inline">
                      Hoy
                    </span>
                    <span className="size-2 rounded-full bg-info sm:hidden" aria-hidden="true" />
                  </>
                )}
              </div>

              <div className="flex flex-col gap-1 mt-auto w-full">
                {status && (
                  <>
                    <span className="grid size-4 place-items-center rounded-full border border-info/25 bg-info/10 text-[8px] font-black text-info sm:hidden" aria-hidden="true">
                      {compactStatus}
                    </span>
                    <div className="hidden origin-left scale-90 sm:block">
                      <StatusBadge status={status} />
                    </div>
                  </>
                )}

                {hasGaps && (
                  <div className="space-y-1" aria-label={`${gapCount} ${gapCount === 1 ? 'franja configurada' : 'franjas configuradas'}`}>
                    <div className="flex h-1.5 gap-0.5" aria-hidden="true">
                      {Array.from({ length: Math.min(gapCount, 4) }).map((_, index) => (
                        <span key={index} className="min-w-1 flex-1 rounded-full bg-ring" />
                      ))}
                    </div>
                    <span className="block text-[9px] font-semibold text-accent-foreground sm:text-[10px]">
                      {gapCount} {gapCount === 1 ? 'franja' : 'franjas'}
                    </span>
                  </div>
                )}
              </div>
            </button>
          );
        })}
      </div>
      </CardContent>
    </Card>
  );
}
