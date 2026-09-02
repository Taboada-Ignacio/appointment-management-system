import { MONTH_NAMES } from '../../../utils/dates';
import { normalizeStatus } from '../../../utils/status';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { AvailabilitySignal } from '../../../components/ui/AvailabilitySignal';
import { useMonthDetail } from '../hooks/useAgenda';
import { ChevronRight, Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';

export function YearMonthCard({ monthData = null, year, onViewMonth = null }) {
  const { data: monthDetail, isLoading } = useMonthDetail(monthData?.id);
  if (!monthData) return null;

  const effectiveMonth = monthDetail || monthData;
  const monthNum = Number(effectiveMonth.nroMes ?? effectiveMonth.mes ?? 1);
  const status = normalizeStatus(effectiveMonth.estadoActual);
  const monthName = MONTH_NAMES[monthNum - 1] || `Mes ${monthNum}`;

  const daysList = Array.isArray(effectiveMonth.dias) ? effectiveMonth.dias : [];
  const configuredDays = daysList.filter((d) => Number(d.cantidadBrechas ?? d.brechas?.length ?? 0) > 0).length;
  const hasDailyDetail = daysList.length > 0;
  const configurationCoverage = hasDailyDetail
    ? Math.round((configuredDays / daysList.length) * 100)
    : null;

  return (
    <Card className="group h-full gap-0 shadow-none transition hover:-translate-y-0.5 hover:border-ring/50 hover:shadow-md">
      <CardHeader className="flex-row items-start justify-between gap-2 pb-0">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-[0.15em] text-info">Mes {String(monthNum).padStart(2, '0')}</span>
            <h3 className="mt-0.5 font-heading text-lg font-semibold capitalize tracking-tight">
              {monthName} {year}
            </h3>
          </div>
          <StatusBadge status={status} />
      </CardHeader>
      <CardContent className="flex-1 pt-4">
        <div className="space-y-3 my-4">
          <div className="space-y-1.5">
            {hasDailyDetail ? (
              <AvailabilitySignal
                totalMinutes={configuredDays}
                maxMinutes={daysList.length}
                dayCount={configuredDays}
                configuredDayCount={daysList.length}
              />
            ) : (
              <div
                className="w-full"
                role="img"
                aria-label={
                  isLoading
                    ? 'Consultando la configuración diaria del mes'
                    : 'Configuración diaria no disponible en el resumen anual'
                }
              >
                <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted" />
              </div>
            )}
            <div className="flex items-center justify-between text-[10px] font-semibold text-muted-foreground">
              <span>Cobertura de configuración</span>
              <span>{isLoading ? 'Consultando…' : configurationCoverage !== null ? `${configurationCoverage}%` : '—'}</span>
            </div>
          </div>

          <div className="space-y-2 rounded-lg border bg-muted/30 p-3 text-xs">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Días con atención:</span>
              <span className="font-bold data-number">
                {hasDailyDetail ? `${configuredDays} de ${daysList.length} consultados` : '—'}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Turnos registrados:</span>
              <span className="font-medium text-muted-foreground" title="El backend no expone conteo de turnos">
                —
              </span>
            </div>
          </div>
        </div>
      </CardContent>
      <CardFooter className="border-t pt-4">
        <Button
          type="button"
          onClick={() => onViewMonth?.(effectiveMonth)}
          variant="outline"
          className="w-full justify-between"
        >
          <span className="flex items-center gap-2">
            <Calendar className="size-3.5 text-info" />
            <span>Ver mes</span>
          </span>
          <ChevronRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
        </Button>
      </CardFooter>
    </Card>
  );
}
