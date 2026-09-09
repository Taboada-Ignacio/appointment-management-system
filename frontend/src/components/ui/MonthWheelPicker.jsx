import { useCallback, useEffect, useRef, useState } from 'react';
import { cn } from '@/lib/utils';
import { MONTH_NAMES } from '@/utils/dates';
import { ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';

const ITEM_HEIGHT = 40;
const VISIBLE_ITEMS = 5;

function clampMonth(value) {
  return Math.min(12, Math.max(1, Number(value) || 1));
}

export function MonthWheelPicker({
  value,
  onChange,
  disabled = false,
  className,
  triggerClassName,
  triggerVariant = 'default',
  role,
  ...props
}) {
  const selectedMonth = clampMonth(value);
  const [open, setOpen] = useState(false);
  const [visualIndex, setVisualIndex] = useState(selectedMonth - 1);
  const viewportRef = useRef(null);
  const scrollTimerRef = useRef(null);
  const dragRef = useRef(null);
  const isDraggingRef = useRef(false);
  const positionedRef = useRef(false);
  const wheelGestureRef = useRef(false);
  const wheelGestureTimerRef = useRef(null);

  const ariaLabel = props['aria-label'] || 'Seleccionar mes para visualizar';

  const positionSelectedOption = useCallback((node) => {
    if (!node || !open) return;

    const viewport = node.parentElement;
    if (!viewport) return;

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
    if (event.key in keys) {
      next = clampMonth(selectedMonth + keys[event.key]);
    } else if (event.key === 'Home') {
      next = 1;
    } else if (event.key === 'End') {
      next = 12;
    } else if (event.key === 'Enter' || event.key === ' ' || event.key === 'Escape') {
      event.preventDefault();
      setOpen(false);
      return;
    } else {
      return;
    }
    event.preventDefault();
    scrollToMonth(next);
    if (next !== selectedMonth) onChange?.(next);
    setOpen(false);
  };

  const handleTriggerKeyDown = (event) => {
    if (disabled) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      setOpen(true);
    } else if (event.key === 'Home') {
      event.preventDefault();
      onChange?.(1);
    } else if (event.key === 'End') {
      event.preventDefault();
      onChange?.(12);
    }
  };

  const isCompact = triggerVariant === 'ios-compact' || triggerVariant === 'compact';

  return (
    <div className={cn(isCompact ? 'inline-block' : 'w-full max-w-xs', className)}>
      <Popover open={open} onOpenChange={handleOpenChange}>
        <PopoverTrigger asChild>
          {isCompact ? (
            <button
              type="button"
              role={role || 'combobox'}
              disabled={disabled}
              aria-label={ariaLabel}
              aria-expanded={open}
              aria-haspopup="listbox"
              data-slot="select-trigger"
              data-size="sm"
              onKeyDown={handleTriggerKeyDown}
              className={cn(
                "group/trigger flex h-7 w-36 items-center justify-between gap-1.5 rounded-full border border-black/[0.06] bg-background/95 px-3 text-xs font-semibold tracking-tight text-foreground shadow-[0_1px_2px_rgba(0,0,0,0.06)] outline-none select-none transition-all hover:bg-background hover:shadow-xs active:scale-[0.97] focus-visible:ring-2 focus-visible:ring-primary/30 disabled:pointer-events-none disabled:opacity-50 dark:border-white/[0.1] dark:bg-card/90 dark:hover:bg-card",
                triggerClassName
              )}
            >
              <span data-slot="select-value" className="truncate">
                {MONTH_NAMES[selectedMonth - 1]}
              </span>
              <ChevronDown
                className={cn(
                  "size-3 text-muted-foreground/80 shrink-0 transition-transform duration-200 ease-out",
                  open && "rotate-180 text-foreground"
                )}
                aria-hidden="true"
              />
            </button>
          ) : (
            <Button
              type="button"
              role={role || 'button'}
              variant="outline"
              disabled={disabled}
              className={cn("w-full justify-between font-medium", triggerClassName)}
              aria-label={ariaLabel}
              aria-expanded={open}
              aria-haspopup="listbox"
              data-slot="select-trigger"
              onKeyDown={handleTriggerKeyDown}
            >
              <span data-slot="select-value">{MONTH_NAMES[selectedMonth - 1]}</span>
              <ChevronDown
                className={cn(
                  "size-4 text-muted-foreground shrink-0 transition-transform duration-200",
                  open && "rotate-180"
                )}
                aria-hidden="true"
              />
            </Button>
          )}
        </PopoverTrigger>
        <PopoverContent
          side="bottom"
          align="center"
          sideOffset={isCompact ? -114 : 6}
          avoidCollisions={!isCompact}
          className="relative w-[180px] overflow-hidden rounded-2xl border border-black/[0.08] bg-background/95 p-0 shadow-[0_12px_32px_rgba(0,0,0,0.14),0_2px_6px_rgba(0,0,0,0.04)] backdrop-blur-xl dark:border-white/[0.12] dark:bg-card/95 dark:shadow-[0_12px_32px_rgba(0,0,0,0.4)] select-none"
          onOpenAutoFocus={(event) => {
            event.preventDefault();
            viewportRef.current?.focus();
          }}
        >
          {/* Banda central fija estilo iOS */}
          <div
            className="pointer-events-none absolute inset-x-2 top-1/2 z-0 h-10 -translate-y-1/2 rounded-xl border border-black/[0.05] bg-muted/60 dark:border-white/[0.08] dark:bg-muted/40"
            aria-hidden="true"
          />

          {/* Gradientes superior e inferior para efecto de profundidad / fade iOS */}
          <div
            className="pointer-events-none absolute inset-x-0 top-0 z-20 h-12 bg-gradient-to-b from-background/95 via-background/60 to-transparent dark:from-card/95 dark:via-card/60"
            aria-hidden="true"
          />
          <div
            className="pointer-events-none absolute inset-x-0 bottom-0 z-20 h-12 bg-gradient-to-t from-background/95 via-background/60 to-transparent dark:from-card/95 dark:via-card/60"
            aria-hidden="true"
          />

          <div
            {...props}
            ref={viewportRef}
            role="listbox"
            aria-label={ariaLabel}
            aria-disabled={disabled}
            aria-activedescendant={`month-wheel-option-${selectedMonth}`}
            tabIndex={disabled ? -1 : 0}
            onKeyDown={handleKeyDown}
            onScroll={handleScroll}
            onWheel={handleWheel}
            onPointerDown={(event) => {
              if (disabled || event.pointerType !== 'mouse') return;
              dragRef.current = { y: event.clientY, scrollTop: event.currentTarget.scrollTop };
              isDraggingRef.current = false;
              event.currentTarget.setPointerCapture?.(event.pointerId);
            }}
            onPointerMove={(event) => {
              if (!dragRef.current) return;
              const delta = dragRef.current.y - event.clientY;
              if (Math.abs(delta) > 4) {
                isDraggingRef.current = true;
              }
              event.currentTarget.scrollTop = dragRef.current.scrollTop + delta;
            }}
            onPointerUp={(event) => {
              dragRef.current = null;
              event.currentTarget.releasePointerCapture?.(event.pointerId);
              setTimeout(() => {
                isDraggingRef.current = false;
              }, 50);
            }}
            onPointerCancel={() => {
              dragRef.current = null;
              isDraggingRef.current = false;
            }}
            className="relative z-10 h-[200px] snap-y snap-mandatory overflow-y-auto overscroll-contain scroll-smooth outline-none [scrollbar-width:none] focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring [&::-webkit-scrollbar]:hidden"
            style={{
              WebkitMaskImage: 'linear-gradient(to bottom, transparent 0%, black 25%, black 75%, transparent 100%)',
              maskImage: 'linear-gradient(to bottom, transparent 0%, black 25%, black 75%, transparent 100%)',
            }}
          >
            {/* Espaciador superior para centrar Enero (2 items * 40px = 80px) */}
            <div style={{ height: `${ITEM_HEIGHT * ((VISIBLE_ITEMS - 1) / 2)}px` }} aria-hidden="true" />
            {MONTH_NAMES.map((name, index) => {
              const month = index + 1;
              const distance = Math.abs(index - visualIndex);
              const isSelected = month === selectedMonth;
              const opacity = distance === 0 ? 1 : Math.max(0.2, 1 - distance * 0.32);
              const scale = distance === 0 ? 1 : Math.max(0.76, 1 - distance * 0.08);

              return (
                <button
                  ref={month === selectedMonth ? positionSelectedOption : undefined}
                  id={`month-wheel-option-${month}`}
                  key={month}
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  disabled={disabled}
                  tabIndex={-1}
                  onClick={() => {
                    if (isDraggingRef.current) return;
                    scrollToMonth(month, 'smooth');
                    if (month !== selectedMonth) onChange?.(month);
                    setOpen(false);
                  }}
                  className={cn(
                    'flex h-10 w-full snap-center items-center justify-center px-2 text-center text-sm transition-[opacity,transform,color,font-weight] duration-150',
                    isSelected
                      ? 'font-semibold text-foreground'
                      : 'font-medium text-muted-foreground hover:text-foreground/80'
                  )}
                  style={{
                    opacity,
                    transform: `scale(${scale})`,
                  }}
                >
                  {name}
                </button>
              );
            })}
            {/* Espaciador inferior para centrar Diciembre */}
            <div style={{ height: `${ITEM_HEIGHT * ((VISIBLE_ITEMS - 1) / 2)}px` }} aria-hidden="true" />
          </div>
        </PopoverContent>
      </Popover>
    </div>
  );
}
