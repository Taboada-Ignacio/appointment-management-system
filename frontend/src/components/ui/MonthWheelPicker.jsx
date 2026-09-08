import { useCallback, useEffect, useRef, useState } from 'react';
import { cn } from '@/lib/utils';
import { MONTH_NAMES } from '@/utils/dates';
import { ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';

const ITEM_HEIGHT = 40;
const VISIBLE_ITEMS = 7;

function clampMonth(value) {
  return Math.min(12, Math.max(1, Number(value) || 1));
}

export function MonthWheelPicker({ value, onChange, disabled = false, className, ...props }) {
  const selectedMonth = clampMonth(value);
  const [open, setOpen] = useState(false);
  const [visualIndex, setVisualIndex] = useState(selectedMonth - 1);
  const viewportRef = useRef(null);
  const scrollTimerRef = useRef(null);
  const dragRef = useRef(null);
  const positionedRef = useRef(false);
  const wheelGestureRef = useRef(false);
  const wheelGestureTimerRef = useRef(null);

  const positionSelectedOption = useCallback((node) => {
    if (!node || !open) return;

    const viewport = node.parentElement;
    if (!viewport) return;

    // Popover se monta con scroll-snap activo. Si se desplaza de forma diferida,
    // el navegador puede resolver el primer snap antes y dejar Enero centrado.
    viewport.style.scrollBehavior = 'auto';
    viewport.style.scrollSnapType = 'none';
    viewport.scrollTop = (selectedMonth - 1) * ITEM_HEIGHT;
    setVisualIndex(selectedMonth - 1);
    positionedRef.current = true;

    const restoreSnap = () => {
      viewport.style.scrollBehavior = '';
      viewport.style.scrollSnapType = '';
    };
    if (typeof requestAnimationFrame === 'function') requestAnimationFrame(restoreSnap);
    else setTimeout(restoreSnap, 0);
  }, [open, selectedMonth]);

  const scrollToMonth = (month, behavior = 'smooth') => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    const top = (clampMonth(month) - 1) * ITEM_HEIGHT;
    if (typeof viewport.scrollTo === 'function') viewport.scrollTo({ top, behavior });
    else viewport.scrollTop = top;
  };

  useEffect(() => {
    setVisualIndex(selectedMonth - 1);
  }, [selectedMonth]);

  useEffect(() => () => {
    clearTimeout(scrollTimerRef.current);
    clearTimeout(wheelGestureTimerRef.current);
  }, []);

  const commitClosestMonth = (index) => {
    const month = clampMonth(index + 1);
    scrollToMonth(month);
    if (month !== selectedMonth) onChange?.(month);
  };

  const handleOpenChange = (nextOpen) => {
    if (!nextOpen && open) {
      clearTimeout(scrollTimerRef.current);
      const index = positionedRef.current
        ? Math.min(11, Math.max(0, Math.round((viewportRef.current?.scrollTop || 0) / ITEM_HEIGHT)))
        : selectedMonth - 1;
      commitClosestMonth(index);
    }
    if (nextOpen) positionedRef.current = false;
    setOpen(nextOpen);
  };

  const handleScroll = (event) => {
    const index = Math.min(11, Math.max(0, Math.round(event.currentTarget.scrollTop / ITEM_HEIGHT)));
    setVisualIndex(index);
    clearTimeout(scrollTimerRef.current);
    scrollTimerRef.current = setTimeout(() => commitClosestMonth(index), 100);
  };

  const handleWheel = (event) => {
    if (disabled || event.deltaY === 0) return;
    event.preventDefault();

    clearTimeout(wheelGestureTimerRef.current);
    wheelGestureTimerRef.current = setTimeout(() => {
      wheelGestureRef.current = false;
    }, 140);

    if (wheelGestureRef.current) return;
    wheelGestureRef.current = true;

    const currentIndex = Math.min(11, Math.max(0, Math.round(event.currentTarget.scrollTop / ITEM_HEIGHT)));
    const nextIndex = Math.min(11, Math.max(0, currentIndex + Math.sign(event.deltaY)));
    event.currentTarget.scrollTop = nextIndex * ITEM_HEIGHT;
    setVisualIndex(nextIndex);

    clearTimeout(scrollTimerRef.current);
    scrollTimerRef.current = setTimeout(() => commitClosestMonth(nextIndex), 100);
  };

  const handleKeyDown = (event) => {
    if (disabled) return;
    const keys = { ArrowUp: -1, ArrowDown: 1, PageUp: -3, PageDown: 3 };
    let next = selectedMonth;
    if (event.key in keys) next = clampMonth(selectedMonth + keys[event.key]);
    else if (event.key === 'Home') next = 1;
    else if (event.key === 'End') next = 12;
    else return;
    event.preventDefault();
    scrollToMonth(next);
    if (next !== selectedMonth) onChange?.(next);
    setOpen(false);
  };

  return (
    <div className={cn('w-full max-w-xs', className)}>
      <Popover open={open} onOpenChange={handleOpenChange}>
        <PopoverTrigger asChild>
          <Button type="button" variant="outline" disabled={disabled} className="w-full justify-between" aria-label={props['aria-label'] || 'Seleccionar mes'} aria-expanded={open}>
            <span>{MONTH_NAMES[selectedMonth - 1]}</span><ChevronDown className="size-4 text-muted-foreground" />
          </Button>
        </PopoverTrigger>
        <PopoverContent
          className="w-72 overflow-hidden rounded-2xl border bg-popover p-0 shadow-xl"
          align="start"
          onOpenAutoFocus={(event) => {
            event.preventDefault();
            viewportRef.current?.focus();
          }}
        >
      <div className="pointer-events-none absolute inset-x-2 top-1/2 z-0 h-10 -translate-y-1/2 rounded-lg border-y border-border bg-accent/55" aria-hidden="true" />
      <div
        {...props}
        ref={viewportRef}
        role="listbox"
        aria-disabled={disabled}
        aria-activedescendant={`month-wheel-option-${selectedMonth}`}
        tabIndex={disabled ? -1 : 0}
        onKeyDown={handleKeyDown}
        onScroll={handleScroll}
        onWheel={handleWheel}
        onPointerDown={(event) => {
          if (disabled || event.pointerType !== 'mouse') return;
          dragRef.current = { y: event.clientY, scrollTop: event.currentTarget.scrollTop };
          event.currentTarget.setPointerCapture?.(event.pointerId);
        }}
        onPointerMove={(event) => {
          if (!dragRef.current) return;
          event.currentTarget.scrollTop = dragRef.current.scrollTop + dragRef.current.y - event.clientY;
        }}
        onPointerUp={(event) => {
          dragRef.current = null;
          event.currentTarget.releasePointerCapture?.(event.pointerId);
        }}
        onPointerCancel={() => { dragRef.current = null; }}
        className="relative z-10 h-[280px] snap-y snap-mandatory overflow-y-auto overscroll-contain scroll-smooth outline-none [scrollbar-width:none] focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring [&::-webkit-scrollbar]:hidden"
        style={{ WebkitMaskImage: 'linear-gradient(to bottom, transparent, black 25%, black 75%, transparent)' }}
      >
        <div style={{ height: `${ITEM_HEIGHT * ((VISIBLE_ITEMS - 1) / 2)}px` }} aria-hidden="true" />
        {MONTH_NAMES.map((name, index) => {
          const month = index + 1;
          const distance = Math.abs(index - visualIndex);
          return (
            <button
              ref={month === selectedMonth ? positionSelectedOption : undefined}
              id={`month-wheel-option-${month}`}
              key={month}
              type="button"
              role="option"
              aria-selected={month === selectedMonth}
              disabled={disabled}
              tabIndex={-1}
              onClick={() => {
                scrollToMonth(month);
                if (month !== selectedMonth) onChange?.(month);
                setOpen(false);
              }}
              className={cn('flex h-10 w-full snap-center items-center justify-center px-4 font-medium transition-[opacity,transform,color] duration-150', month === selectedMonth ? 'text-foreground' : 'text-muted-foreground')}
              style={{ opacity: Math.max(0.2, 1 - distance * 0.22), transform: `scale(${Math.max(0.88, 1 - distance * 0.035)})` }}
            >
              {name}
            </button>
          );
        })}
        <div style={{ height: `${ITEM_HEIGHT * ((VISIBLE_ITEMS - 1) / 2)}px` }} aria-hidden="true" />
      </div>
        </PopoverContent>
      </Popover>
    </div>
  );
}
