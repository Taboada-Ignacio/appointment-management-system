import * as React from 'react';
import { Clock, Check, X } from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { WheelPicker, WheelPickerWrapper } from '@/components/ui/wheel-picker';
import { cn } from '@/lib/utils';

/**
 * Parses "HH:mm" into { hour: string, minute: string }
 * @param {string} val
 * @returns {{ hour: string, minute: string } | null}
 */
function parseTime(val) {
  if (!val || typeof val !== 'string') return null;
  const parts = val.split(':');
  if (parts.length < 2) return null;
  const h = parts[0].padStart(2, '0');
  const m = parts[1].padStart(2, '0');
  const hNum = Number(h);
  const mNum = Number(m);
  if (Number.isNaN(hNum) || Number.isNaN(mNum)) return null;
  if (hNum < 0 || hNum > 23 || mNum < 0 || mNum > 59) return null;
  return { hour: h, minute: m };
}

/**
 * Format hours/minutes to "HH:mm"
 */
function formatTimeStr(hour, minute) {
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

/**
 * Clamp a "HH:mm" time string between minTime and maxTime
 */
function clampTime(timeStr, minTime, maxTime) {
  if (!timeStr) return timeStr;
  if (minTime && timeStr < minTime) return minTime;
  if (maxTime && timeStr > maxTime) return maxTime;
  return timeStr;
}

export function TimeWheelPicker({
  value = '',
  onChange,
  minuteStep = 15,
  minTime = null,
  maxTime = null,
  disabled = false,
  placeholder = '--:--',
  className,
  id,
  name,
  'aria-label': ariaLabel = 'Seleccionar hora',
  inline = false,
  clearable = true,
}) {
  const [open, setOpen] = React.useState(false);
  const [digitBuffer, setDigitBuffer] = React.useState('');
  const bufferTimeoutRef = React.useRef(null);

  React.useEffect(() => {
    return () => {
      if (bufferTimeoutRef.current) {
        clearTimeout(bufferTimeoutRef.current);
      }
    };
  }, []);

  const parsed = React.useMemo(() => parseTime(value), [value]);

  // Default fallback when value is empty
  const defaultTime = React.useMemo(() => {
    if (minTime) {
      const pMin = parseTime(minTime);
      if (pMin) return pMin;
    }
    return { hour: '09', minute: '00' };
  }, [minTime]);

  const previewHour = React.useMemo(() => {
    if (digitBuffer.length >= 2) {
      return digitBuffer.slice(0, 2);
    }
    return null;
  }, [digitBuffer]);

  const previewMinute = React.useMemo(() => {
    if (digitBuffer.length === 4) {
      return digitBuffer.slice(2, 4);
    }
    return null;
  }, [digitBuffer]);

  const currentHour = previewHour || parsed?.hour || defaultTime.hour;
  const currentMinute = previewMinute || parsed?.minute || defaultTime.minute;

  const minParsed = React.useMemo(() => parseTime(minTime), [minTime]);
  const maxParsed = React.useMemo(() => parseTime(maxTime), [maxTime]);

  // Generate 24 hours (00-23)
  const hourOptions = React.useMemo(() => {
    return Array.from({ length: 24 }, (_, i) => {
      const hStr = String(i).padStart(2, '0');
      let isDisabled = false;
      if (minParsed && i < Number(minParsed.hour)) {
        isDisabled = true;
      }
      if (maxParsed && i > Number(maxParsed.hour)) {
        isDisabled = true;
      }
      return {
        value: hStr,
        label: hStr,
        disabled: isDisabled,
      };
    });
  }, [minParsed, maxParsed]);

  // Generate minute options based on minuteStep (and include current minute if irregular)
  const minuteOptions = React.useMemo(() => {
    const step = Math.max(1, Math.min(60, Number(minuteStep) || 15));
    const set = new Set();

    for (let m = 0; m < 60; m += step) {
      set.add(m);
    }
    // If active minute is defined and not yet in set, include it
    const activeM = previewMinute !== null ? previewMinute : parsed?.minute;
    if (activeM !== undefined) {
      const curM = Number(activeM);
      if (!Number.isNaN(curM) && curM >= 0 && curM < 60) {
        set.add(curM);
      }
    }

    const sortedMinutes = Array.from(set).sort((a, b) => a - b);

    return sortedMinutes.map((m) => {
      const mStr = String(m).padStart(2, '0');
      let isDisabled = false;
      const curHNum = Number(currentHour);

      if (minParsed && curHNum === Number(minParsed.hour) && m < Number(minParsed.minute)) {
        isDisabled = true;
      }
      if (maxParsed && curHNum === Number(maxParsed.hour) && m > Number(maxParsed.minute)) {
        isDisabled = true;
      }

      return {
        value: mStr,
        label: mStr,
        disabled: isDisabled,
      };
    });
  }, [minuteStep, previewMinute, parsed, currentHour, minParsed, maxParsed]);

  const commitBuffer = React.useCallback(
    (buf) => {
      if (!buf) return null;
      let h = '00';
      let m = '00';

      if (buf.length === 1) {
        h = buf.padStart(2, '0');
        m = currentMinute || '00';
      } else if (buf.length === 2) {
        h = buf;
        m = currentMinute || '00';
      } else if (buf.length === 3) {
        h = buf.slice(0, 2);
        m = buf[2].padEnd(2, '0');
      } else if (buf.length >= 4) {
        h = buf.slice(0, 2);
        m = buf.slice(2, 4);
      }

      const hNum = Math.min(23, Math.max(0, parseInt(h, 10) || 0));
      const mNum = Math.min(59, Math.max(0, parseInt(m, 10) || 0));
      const formatted = formatTimeStr(hNum, mNum);
      const clamped = clampTime(formatted, minTime, maxTime);

      onChange?.(clamped);
      return clamped;
    },
    [currentMinute, minTime, maxTime, onChange]
  );

  const handleHourChange = React.useCallback(
    (newHour) => {
      if (disabled) return;
      if (bufferTimeoutRef.current) clearTimeout(bufferTimeoutRef.current);
      setDigitBuffer('');
      const newTime = clampTime(formatTimeStr(newHour, currentMinute), minTime, maxTime);
      onChange?.(newTime);
    },
    [disabled, currentMinute, minTime, maxTime, onChange]
  );

  const handleMinuteChange = React.useCallback(
    (newMinute) => {
      if (disabled) return;
      if (bufferTimeoutRef.current) clearTimeout(bufferTimeoutRef.current);
      setDigitBuffer('');
      const newTime = clampTime(formatTimeStr(currentHour, newMinute), minTime, maxTime);
      onChange?.(newTime);
    },
    [disabled, currentHour, minTime, maxTime, onChange]
  );

  const handleClear = React.useCallback(
    (e) => {
      e?.stopPropagation?.();
      if (bufferTimeoutRef.current) clearTimeout(bufferTimeoutRef.current);
      setDigitBuffer('');
      onChange?.('');
      setOpen(false);
    },
    [onChange]
  );

  const handleDone = React.useCallback(() => {
    if (bufferTimeoutRef.current) clearTimeout(bufferTimeoutRef.current);
    if (digitBuffer) {
      commitBuffer(digitBuffer);
      setDigitBuffer('');
    } else if (!value) {
      onChange?.(clampTime(formatTimeStr(currentHour, currentMinute), minTime, maxTime));
    }
    setOpen(false);
  }, [digitBuffer, commitBuffer, value, currentHour, currentMinute, minTime, maxTime, onChange]);

  const handleKeyDown = React.useCallback(
    (e) => {
      if (disabled) return;

      // Digits '0' - '9'
      if (e.key >= '0' && e.key <= '9') {
        e.preventDefault();

        if (bufferTimeoutRef.current) {
          clearTimeout(bufferTimeoutRef.current);
        }

        let currentBuf = digitBuffer;
        if (currentBuf.length >= 4) {
          currentBuf = '';
        }

        let nextBuf = currentBuf + e.key;

        // Smart shortcut: if 1st digit is >= '3' (e.g. '8'), in 24h format it must be '08'
        if (nextBuf.length === 1 && nextBuf >= '3') {
          nextBuf = '0' + nextBuf;
        }

        // Clamp hour if 2 digits entered
        if (nextBuf.length === 2) {
          const h = parseInt(nextBuf, 10);
          if (h > 23) {
            nextBuf = '23';
          }
        }

        // Smart shortcut: if 3rd digit is >= '6' (e.g. '207'), minute tens digit cannot exceed 5
        if (nextBuf.length === 3 && nextBuf[2] >= '6') {
          nextBuf = nextBuf.slice(0, 2) + '0' + nextBuf[2];
        }

        // Check if 4 digits completed
        if (nextBuf.length >= 4) {
          commitBuffer(nextBuf);
          setDigitBuffer('');
          return;
        }

        setDigitBuffer(nextBuf);

        bufferTimeoutRef.current = setTimeout(() => {
          commitBuffer(nextBuf);
          setDigitBuffer('');
        }, 1500);

        return;
      }

      // Colon ':' separator
      if (e.key === ':') {
        e.preventDefault();
        if (digitBuffer.length === 1) {
          setDigitBuffer('0' + digitBuffer);
        }
        return;
      }

      // Backspace
      if (e.key === 'Backspace') {
        if (bufferTimeoutRef.current) {
          clearTimeout(bufferTimeoutRef.current);
        }
        if (digitBuffer.length > 0) {
          e.preventDefault();
          const next = digitBuffer.slice(0, -1);
          setDigitBuffer(next);
          if (next.length > 0) {
            bufferTimeoutRef.current = setTimeout(() => {
              commitBuffer(next);
              setDigitBuffer('');
            }, 1500);
          }
        } else if (clearable && value) {
          e.preventDefault();
          onChange?.('');
        }
        return;
      }

      // Enter
      if (e.key === 'Enter') {
        if (digitBuffer.length > 0) {
          e.preventDefault();
          if (bufferTimeoutRef.current) {
            clearTimeout(bufferTimeoutRef.current);
          }
          commitBuffer(digitBuffer);
          setDigitBuffer('');
          setOpen(false);
        }
        return;
      }

      // Escape
      if (e.key === 'Escape') {
        if (bufferTimeoutRef.current) {
          clearTimeout(bufferTimeoutRef.current);
        }
        setDigitBuffer('');
        setOpen(false);
      }
    },
    [disabled, digitBuffer, clearable, value, commitBuffer, onChange]
  );

  const displayText = React.useMemo(() => {
    if (digitBuffer) {
      if (digitBuffer.length === 1) {
        return `${digitBuffer}_:--`;
      }
      if (digitBuffer.length === 2) {
        return `${digitBuffer}:--`;
      }
      if (digitBuffer.length === 3) {
        return `${digitBuffer.slice(0, 2)}:${digitBuffer[2]}_`;
      }
      return `${digitBuffer.slice(0, 2)}:${digitBuffer.slice(2, 4)}`;
    }
    return parsed ? `${parsed.hour}:${parsed.minute}` : placeholder;
  }, [digitBuffer, parsed, placeholder]);

  const renderWheels = () => (
    <div className="flex flex-col items-center gap-3 p-3">
      <div className="flex items-center justify-between w-full px-1">
        <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          Hora y Minutos
        </span>
        <span className="text-sm font-bold tabular-nums text-foreground">
          {displayText}
        </span>
      </div>

      <WheelPickerWrapper className="w-44 bg-background shadow-inner">
        <WheelPicker
          options={hourOptions}
          value={currentHour}
          onValueChange={handleHourChange}
          aria-label="Horas"
        />
        <div className="flex items-center justify-center select-none font-bold text-base text-muted-foreground px-0.5">
          :
        </div>
        <WheelPicker
          options={minuteOptions}
          value={currentMinute}
          onValueChange={handleMinuteChange}
          aria-label="Minutos"
        />
      </WheelPickerWrapper>

      <div className="flex items-center justify-between w-full pt-2 border-t border-border gap-2">
        {clearable ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleClear}
            disabled={!value && !digitBuffer}
            className="text-xs text-muted-foreground hover:text-destructive h-8 px-2"
          >
            <X className="size-3.5 mr-1" />
            Limpiar
          </Button>
        ) : (
          <div />
        )}
        <Button
          type="button"
          size="sm"
          onClick={handleDone}
          className="text-xs h-8 px-3"
        >
          <Check className="size-3.5 mr-1" />
          Listo
        </Button>
      </div>
    </div>
  );

  if (inline) {
    return (
      <div
        className={cn('inline-block outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-md', className)}
        tabIndex={disabled ? -1 : 0}
        onKeyDown={handleKeyDown}
        aria-label={ariaLabel}
      >
        {renderWheels()}
      </div>
    );
  }

  return (
    <div
      className={cn('relative inline-flex items-center', className)}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget)) {
          if (digitBuffer) {
            if (bufferTimeoutRef.current) clearTimeout(bufferTimeoutRef.current);
            commitBuffer(digitBuffer);
            setDigitBuffer('');
          }
        }
      }}
    >
      <input
        type="text"
        id={id}
        name={name}
        disabled={disabled}
        aria-label={ariaLabel}
        value={value || ''}
        onChange={(e) => onChange?.(e.target.value)}
        className="sr-only pointer-events-none"
        tabIndex={-1}
        data-testid="time-wheel-picker-input"
      />
      <Popover
        open={open}
        onOpenChange={(newOpen) => {
          if (!newOpen && digitBuffer) {
            if (bufferTimeoutRef.current) clearTimeout(bufferTimeoutRef.current);
            commitBuffer(digitBuffer);
            setDigitBuffer('');
          }
          setOpen(newOpen);
        }}
      >
        <PopoverTrigger asChild>
          <button
            type="button"
            disabled={disabled}
            aria-label={`${ariaLabel}: ${displayText}`}
            aria-haspopup="dialog"
            aria-expanded={open}
            data-testid="time-wheel-picker-trigger"
            onKeyDown={handleKeyDown}
            className="flex h-9 w-full min-w-[100px] items-center justify-between rounded-md border border-input bg-background px-3 py-1 text-sm shadow-xs transition-[color,box-shadow] outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 font-medium tabular-nums text-foreground"
          >
            <span className="flex items-center gap-1.5 truncate">
              <Clock className="size-3.5 shrink-0 text-muted-foreground" aria-hidden="true" />
              <span className={cn(!parsed && !digitBuffer && 'text-muted-foreground')}>
                {displayText}
              </span>
            </span>
          </button>
        </PopoverTrigger>
        <PopoverContent
          className="w-auto p-0 z-50"
          align="start"
          onKeyDown={handleKeyDown}
        >
          {renderWheels()}
        </PopoverContent>
      </Popover>
    </div>
  );
}
