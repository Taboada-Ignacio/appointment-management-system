import { useState, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { MonthCalendar } from '../components/MonthCalendar';
import { DailyTimeline } from '../components/DailyTimeline';
import { MonthConfigurator } from '../components/MonthConfigurator';
import {
  useAnnualAgendas,
  useMonths,
  useMonthDetail,
  useSelectableDays,
  useDayDetail,
} from '../hooks/useAgenda';
import { EmptyState } from '../../../components/ui/EmptyState';
import { SkeletonTimeline } from '../../../components/ui/LoadingSkeleton';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import {
  getCurrentYearMonth,
  formatMonthYear,
  getMonthRange,
  MONTH_NAMES,
} from '../../../utils/dates';
import { deriveTemporalStatus } from '../../../utils/status';
import { professionalContext } from '../../../config/professional';
import { ChevronLeft, ChevronRight, SlidersHorizontal, Calendar, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function MyMonthPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const timezone = professionalContext.timezone;
  const currentYM = getCurrentYearMonth(timezone);

  const ymParam = searchParams.get('mes');
  let targetYear = currentYM.year;
  let targetMonth = currentYM.month;

  if (ymParam && ymParam.includes('-')) {
    const [yStr, mStr] = ymParam.split('-');
    const parsedY = parseInt(yStr, 10);
    const parsedM = parseInt(mStr, 10);
    if (!isNaN(parsedY) && !isNaN(parsedM)) {
      targetYear = parsedY;
      targetMonth = parsedM;
    }
  }

  const monthKey = `${targetYear}-${targetMonth}`;
  const [selectedDay, setSelectedDay] = useState({ monthKey, id: null });
  const selectedDayId = selectedDay.monthKey === monthKey ? selectedDay.id : null;
  const setSelectedDayId = (id) => setSelectedDay({ monthKey, id });
  const [showConfigurator, setShowConfigurator] = useState(() => searchParams.get('configurar') === 'true');

  const { data: agendas, isLoading: isLoadingAgendas } = useAnnualAgendas();
  const yearAgenda = agendas?.find((agenda) => Number(agenda.anio) === targetYear);
  const { data: months, isLoading: isLoadingMonths } = useMonths(
    targetYear,
    { enabled: Boolean(yearAgenda) }
  );
  const currentMonthData = months?.find(
    (m) => Number(m.nroMes ?? m.mes) === targetMonth
  );

  const monthId = currentMonthData?.id;
  const {
    data: monthDetail,
    isLoading: isLoadingDetail,
    error: monthDetailError,
  } = useMonthDetail(monthId);

  const { firstDay, lastDay } = useMemo(() => getMonthRange(targetYear, targetMonth), [targetYear, targetMonth]);
  const { data: selectableDays, isLoading: isLoadingDays } = useSelectableDays(
    monthId ? firstDay : null,
    monthId ? lastDay : null
  );

  const handlePrevMonth = () => {
    let nextY = targetYear;
    let nextM = targetMonth - 1;
    if (nextM < 1) {
      nextM = 12;
      nextY--;
    }
    setSearchParams({ mes: `${nextY}-${String(nextM).padStart(2, '0')}` });
    setSelectedDayId(null);
  };

  const handleNextMonth = () => {
    let nextY = targetYear;
    let nextM = targetMonth + 1;
    if (nextM > 12) {
      nextM = 1;
      nextY++;
    }
    setSearchParams({ mes: `${nextY}-${String(nextM).padStart(2, '0')}` });
    setSelectedDayId(null);
  };

  const handleCurrentMonth = () => {
    setSearchParams({ mes: `${currentYM.year}-${String(currentYM.month).padStart(2, '0')}` });
    setSelectedDayId(null);
  };

  const handleSelectDay = (day) => {
    setSelectedDayId(day?.id ? String(day.id) : day?.fecha);
  };

  // Combine calendar day data from selectableDays + monthDetail.dias
  const combinedDays = useMemo(() => {
    const list = monthDetail?.dias || selectableDays || [];
    return list.map((d) => ({
      ...d,
      id: d.diaAgendaId || d.id,
      estadoActual: d.estadoActual ?? d.estado,
    }));
  }, [monthDetail?.dias, selectableDays]);

  const selectedDayInfo = combinedDays.find(
    (d) => String(d.id) === String(selectedDayId) || d.fecha === String(selectedDayId)
  );
  const selectedDayAgendaId = selectedDayInfo?.diaAgendaId ?? selectedDayInfo?.id;
  const {
    data: selectedDayDetail,
    isLoading: isLoadingSelectedDay,
    error: selectedDayError,
  } = useDayDetail(selectedDayAgendaId);

  const selectedDayRawStatus = selectedDayDetail
    ? selectedDayDetail.estadoActual
    : selectedDayInfo?.estadoActual;
  const selectedDayForTimeline = selectedDayInfo && selectedDayDetail
    ? {
        ...selectedDayInfo,
        ...selectedDayDetail,
        estadoActual: deriveTemporalStatus(
          selectedDayRawStatus,
          selectedDayDetail.fecha || selectedDayInfo.fecha,
          timezone
        ),
      }
    : null;

  const isLoading =
    isLoadingAgendas ||
    (Boolean(yearAgenda) && isLoadingMonths) ||
    (Boolean(monthId) && (isLoadingDetail || isLoadingDays));
  const rawStatus = monthDetail ? monthDetail.estadoActual : currentMonthData?.estadoActual;
  const temporalStatus = deriveTemporalStatus(
    rawStatus,
    `${targetYear}-${String(targetMonth).padStart(2, '0')}`,
    timezone
  );

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Vista mensual de atención"
        title="Mi mes"
        description={formatMonthYear(targetMonth, targetYear)}
        status={<StatusBadge status={temporalStatus} />}
        actions={
          <div className="flex items-center gap-2">
            <Button
              type="button"
              onClick={() => setShowConfigurator(!showConfigurator)}
              variant={showConfigurator ? 'secondary' : 'outline'}
            >
              <SlidersHorizontal className="size-3.5 text-info" />
              <span>{showConfigurator ? 'Ocultar configuración' : 'Configurar mes'}</span>
            </Button>

            <div className="flex items-center gap-1 rounded-xl border bg-card p-1 shadow-xs">
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                onClick={handlePrevMonth}
                aria-label="Mes anterior"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleCurrentMonth}
              >
                Actual
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                onClick={handleNextMonth}
                aria-label="Mes siguiente"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        }
      />

      {/* Empty state if no agenda */}
      {!isLoading && !currentMonthData && (
        <EmptyState
          icon={Calendar}
          title={`No hay agenda para ${formatMonthYear(targetMonth, targetYear)}`}
          description={`Para operar este mes debés generar la agenda anual correspondiente a ${targetYear}.`}
          action={{
            label: 'Ir a configuración de agendas',
            onClick: () => navigate('/profesional/configuracion'),
          }}
        />
      )}

      {currentMonthData && (
        <>
          {showConfigurator && isLoadingDetail && (
            <div className="rounded-xl border bg-card p-6 text-sm text-muted-foreground" role="status">
              Verificando la configuración real del mes…
            </div>
          )}

          {showConfigurator && monthDetailError && (
            <EmptyState
              icon={SlidersHorizontal}
              title="No se pudo verificar la configuración del mes"
              description="Las acciones de configuración permanecen bloqueadas hasta poder consultar el detalle real del mes."
            />
          )}

          {showConfigurator && monthDetail && (
            <Card className="border-ring/50 shadow-sm">
              <CardHeader className="flex-row items-start justify-between border-b">
                <div>
                  <CardTitle>Configurador de {MONTH_NAMES[targetMonth - 1]} {targetYear}</CardTitle>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Definí el modo de asignación de brechas horarias y activá el mes para recibir pacientes.
                  </p>
                </div>
                <Button
                  type="button"
                  onClick={() => setShowConfigurator(false)}
                  variant="ghost"
                  size="sm"
                >
                  Cerrar
                </Button>
              </CardHeader>
              <CardContent className="p-5 sm:p-6">
              <MonthConfigurator
                monthData={monthDetail}
                year={targetYear}
                onConfigured={() => {}}
              />
              </CardContent>
            </Card>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            <div className="lg:col-span-7 xl:col-span-8">
              <MonthCalendar
                year={targetYear}
                month={targetMonth}
                days={combinedDays}
                selectedDayId={selectedDayId}
                onSelectDay={handleSelectDay}
                loading={isLoading}
              />
            </div>

            <div className="lg:col-span-5 xl:col-span-4 space-y-4">
              {selectedDayInfo ? (
                <div className="space-y-3">
                  {isLoadingSelectedDay ? (
                    <SkeletonTimeline className="rounded-xl border bg-card p-6" />
                  ) : selectedDayError ? (
                    <EmptyState
                      icon={Calendar}
                      title="No se pudo cargar el detalle del día"
                      description="No se muestran horarios resumidos porque el detalle real del día no está disponible."
                    />
                  ) : selectedDayForTimeline ? (
                    <DailyTimeline
                      day={selectedDayForTimeline}
                      timezone={timezone}
                      onEditGaps={undefined}
                      canEdit={false}
                    />
                  ) : null}
                  <Button
                    type="button"
                    onClick={() => navigate(`/profesional/mi-dia?fecha=${selectedDayInfo.fecha}`)}
                    className="w-full"
                  >
                    <span>Abrir detalle completo de este día</span>
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </div>
              ) : (
                <Card className="min-h-[300px] shadow-none">
                  <CardContent className="flex min-h-[300px] flex-col items-center justify-center p-8 text-center">
                  <Calendar className="mb-3 size-9 text-info/35" />
                  <h3 className="font-heading text-sm font-semibold">Seleccioná un día</h3>
                  <p className="mt-1 max-w-xs text-xs leading-5 text-muted-foreground">
                    Hacé clic en cualquier fecha del calendario para visualizar sus horarios de atención.
                  </p>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
