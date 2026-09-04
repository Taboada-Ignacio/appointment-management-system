import { CalendarIcon } from 'lucide-react';
import { es } from 'date-fns/locale';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';

function parseIsoDate(value) {
  if (!value) return undefined;
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toIsoDate(date) {
  if (!date) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function DatePickerField({ id, label, value, onChange, min, max, disabled = false }) {
  const minDate = parseIsoDate(min);
  const maxDate = parseIsoDate(max);

  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="flex max-w-sm gap-2">
        <Input
          id={id}
          type="date"
          value={value}
          min={min}
          max={max}
          disabled={disabled}
          onChange={(event) => onChange(event.target.value)}
          className="tabular-nums"
        />
        <Popover>
          <PopoverTrigger asChild>
            <Button type="button" variant="outline" size="icon" disabled={disabled} aria-label="Abrir calendario">
              <CalendarIcon />
            </Button>
          </PopoverTrigger>
          <PopoverContent align="end" className="w-auto p-0">
            <Calendar
              mode="single"
              selected={parseIsoDate(value)}
              onSelect={(date) => date && onChange(toIsoDate(date))}
              defaultMonth={parseIsoDate(value) || minDate}
              startMonth={minDate}
              endMonth={maxDate}
              disabled={{ before: minDate, after: maxDate }}
              captionLayout="dropdown"
              locale={es}
              weekStartsOn={1}
            />
          </PopoverContent>
        </Popover>
      </div>
    </div>
  );
}
