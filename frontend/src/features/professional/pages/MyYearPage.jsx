import { useSearchParams, useNavigate } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { YearMonthCard } from '../components/YearMonthCard';
import { useMonths, useCreateAnnualAgenda, useAnnualAgendas } from '../hooks/useAgenda';
import { EmptyState } from '../../../components/ui/EmptyState';
import { SkeletonCard } from '../../../components/ui/LoadingSkeleton';
import { useToast } from '../../../components/ui/ToastProvider';
import { getCurrentYearMonth } from '../../../utils/dates';
import { professionalContext } from '../../../config/professional';
import { ChevronLeft, ChevronRight, CalendarPlus } from 'lucide-react';
import { Button } from '@/components/ui/button';

export function MyYearPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const timezone = professionalContext.timezone;
  const currentYM = getCurrentYearMonth(timezone);

  const yearParam = searchParams.get('anio') || searchParams.get('year');
  const year = yearParam ? parseInt(yearParam, 10) || currentYM.year : currentYM.year;

  const { success, error: showError } = useToast();
  const { data: agendas } = useAnnualAgendas();
  const hasAgenda = agendas?.some((a) => a.anio === year);

  const { data: months, isLoading: isLoadingMonths } = useMonths(hasAgenda ? year : null);
  const createAgenda = useCreateAnnualAgenda();

  const handlePrevYear = () => setSearchParams({ anio: String(year - 1) });
  const handleNextYear = () => setSearchParams({ anio: String(year + 1) });
  const handleCurrentYear = () => setSearchParams({ anio: String(currentYM.year) });

  const handleGenerateAgenda = async () => {
    try {
      await createAgenda.mutateAsync(year);
      success('Agenda anual generada', `Se crearon los 12 meses para el año ${year}.`);
    } catch (err) {
      showError('Error al crear agenda', err.message);
    }
  };

  const handleViewMonth = (monthData) => {
    const monthNum = Number(monthData?.nroMes ?? monthData?.mes ?? 1);
    navigate(`/profesional/mi-mes?mes=${year}-${String(monthNum).padStart(2, '0')}`);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Panorama anual de agenda"
        title="Mi año"
        description={`Resumen de los 12 meses correspondientes a ${year}.`}
        actions={
          <div className="flex items-center gap-1 rounded-xl border bg-card p-1 shadow-xs">
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={handlePrevYear}
              aria-label="Año anterior"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleCurrentYear}
            >
              {currentYM.year}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={handleNextYear}
              aria-label="Año siguiente"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        }
      />

      {isLoadingMonths && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 12 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      )}

      {!isLoadingMonths && !hasAgenda && (
        <EmptyState
          icon={CalendarPlus}
          title={`No existe agenda para el año ${year}`}
          description="Para poder configurar los días de atención, es necesario generar la estructura de los 12 meses de este año."
          action={{
            label: createAgenda.isPending ? 'Generando...' : `Generar agenda ${year}`,
            onClick: handleGenerateAgenda,
          }}
        />
      )}

      {!isLoadingMonths && hasAgenda && months && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {months.map((m) => (
            <YearMonthCard
              key={m.id || m.nroMes}
              monthData={m}
              year={year}
              onViewMonth={handleViewMonth}
            />
          ))}
        </div>
      )}
    </div>
  );
}
