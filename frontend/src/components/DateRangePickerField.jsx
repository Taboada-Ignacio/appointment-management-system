import { useState } from 'react';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import { CalendarDays } from 'lucide-react';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverAnchor, PopoverContent } from '@/components/ui/popover';
import { cn } from '@/lib/utils';

function parseIsoDate(value) {
  if (!value) return undefined;
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function displayDate(value) {
  const date = parseIsoDate(value);
  return date ? format(date, 'EEE d MMM', { locale: es }) : 'Elegí una fecha';
}

export function DateRangePickerField({ start, end, onChange, min, max, disabled = false }) {
  const [open, setOpen] = useState(false);
  const [activeField, setActiveField] = useState('start');
  const startDate = parseIsoDate(start);
  const endDate = parseIsoDate(end);
  const minDate = parseIsoDate(min);
  const maxDate = parseIsoDate(max);

  const openFor = (field) => {
    setActiveField(field);
    setOpen(true);
  };

  const selectDate = (date) => {
    const selected = toIsoDate(date);

    if (activeField === 'start') {
      onChange(selected, end && end >= selected ? end : '');
      setActiveField('end');
      return;
    }

    if (!start || selected < start) {
      onChange(selected, '');
      setActiveField('end');
      return;
    }

    onChange(start, selected);
    setOpen(false);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverAnchor asChild>
        <div className="grid overflow-hidden rounded-xl border bg-background shadow-xs transition focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/20 sm:grid-cols-[1fr_1px_1fr_auto]">
          <button type="button" disabled={disabled} onClick={() => openFor('start')} className={cn('px-4 py-3 text-left hover:bg-muted/60 focus-visible:outline-none', activeField === 'start' && open && 'bg-primary/5')} aria-label={`Desde: ${displayDate(start)}`}>
            <span className="block text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">Desde</span>
            <span className={cn('mt-1 block text-sm font-semibold capitalize', !start && 'font-normal text-muted-foreground')}>{displayDate(start)}</span>
          </button>
          <div className="hidden bg-border sm:block" />
          <button type="button" disabled={disabled} onClick={() => openFor('end')} className={cn('border-t px-4 py-3 text-left hover:bg-muted/60 focus-visible:outline-none sm:border-0', activeField === 'end' && open && 'bg-primary/5')} aria-label={`Hasta: ${displayDate(end)}`}>
            <span className="block text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">Hasta</span>
            <span className={cn('mt-1 block text-sm font-semibold capitalize', !end && 'font-normal text-muted-foreground')}>{displayDate(end)}</span>
          </button>
          <button type="button" disabled={disabled} onClick={() => openFor(start ? 'end' : 'start')} className="hidden items-center justify-center border-l px-4 text-primary hover:bg-muted/60 focus-visible:outline-none sm:flex" aria-label="Abrir calendario de fechas">
            <CalendarDays className="size-5" />
          </button>
        </div>
      </PopoverAnchor>
      <PopoverContent align="start" className="w-auto p-0">
        <div className="border-b px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-primary">{activeField === 'start' ? 'Elegí la fecha desde' : 'Ahora elegí la fecha hasta'}</p>
        </div>
        <Calendar
          mode="range"
          selected={{ from: startDate, to: endDate }}
          onDayClick={selectDate}
          defaultMonth={startDate || minDate || new Date()}
          startMonth={minDate}
          endMonth={maxDate}
          disabled={{ before: minDate, after: maxDate }}
          locale={es}
          weekStartsOn={1}
        />
      </PopoverContent>
    </Popover>
  );
}
