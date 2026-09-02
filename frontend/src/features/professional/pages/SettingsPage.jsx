import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Calendar,
  CalendarCheck,
  Clock,
  Ellipsis,
  ExternalLink,
  RotateCcw,
  ShieldAlert,
  UserRound,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { EmptyState } from '@/components/ui/EmptyState';
import { IntegrationNotice } from '@/components/ui/IntegrationNotice';
import { Skeleton } from '@/components/ui/skeleton';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { useToast } from '@/components/ui/ToastProvider';
import { professionalContext } from '@/config/professional';
import { getCurrentYearMonth, MONTH_NAMES } from '@/utils/dates';
import { reopenOnboarding } from '@/utils/onboarding';
import { isMonthConfigured, normalizeStatus } from '@/utils/status';
import { WEEKLY_TEMPLATE_KEY } from '../components/WeeklyEditor';
import { PageHeader } from '../components/PageHeader';
import {
  useActivateMonth,
  useAnnualAgendas,
  useCreateAnnualAgenda,
  useInactivateMonth,
  useMonthDetail,
  useMonths,
} from '../hooks/useAgenda';
import { useProfessionalConfig } from '../hooks/useProfessionalConfig';

export function SettingsPage() {
  const navigate = useNavigate();
  const { success, error: showError } = useToast();
  const { data: config } = useProfessionalConfig(professionalContext.id);
  const { year: currentYear } = getCurrentYearMonth(professionalContext.timezone);
  const { data: agendas, isLoading: isLoadingAgendas } = useAnnualAgendas();
  const hasCurrentYearAgenda = agendas?.some((agenda) => agenda.anio === currentYear);
  const { data: months, isLoading: isLoadingMonths } = useMonths(hasCurrentYearAgenda ? currentYear : null);
  const createAgenda = useCreateAnnualAgenda();
  const activateMonth = useActivateMonth();
  const inactivateMonth = useInactivateMonth();

  let savedWeeklyDraft = null;
  try {
    const raw = localStorage.getItem(WEEKLY_TEMPLATE_KEY);
    if (raw) savedWeeklyDraft = JSON.parse(raw);
  } catch {
    savedWeeklyDraft = null;
  }

  const handleGenerateAgenda = async () => {
    try {
      await createAgenda.mutateAsync(currentYear);
      success('Agenda generada', `Se generaron los 12 meses para el año ${currentYear}.`);
    } catch (error) {
      showError('Error al crear agenda', error.message);
    }
  };

  const handleActivateMonth = async (monthId, name) => {
    try {
      await activateMonth.mutateAsync(monthId);
      success('Mes activado', `${name} habilitado para turnos.`);
    } catch (error) {
      showError('Error al activar', error.message);
    }
  };

  const handleInactivateMonth = async (monthId, name) => {
    try {
      await inactivateMonth.mutateAsync(monthId);
      success('Mes inactivado', `${name} pausado.`);
    } catch (error) {
      showError('Error al inactivar', error.message);
    }
  };

  const handleReopenTutorial = () => {
    reopenOnboarding();
    success('Tutorial reabierto', 'El asistente de configuración está visible en la parte superior.');
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Ajustes de operación"
        title="Configuración"
        description="Gestioná agendas anuales, estados mensuales, horarios habituales y el contexto profesional."
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(20rem,0.65fr)]">
        <div className="space-y-6">
          <Card className="shadow-none">
            <CardHeader>
              <div className="flex items-center gap-2">
                <CalendarCheck className="size-4 text-info" />
                <CardTitle>Agendas anuales</CardTitle>
              </div>
              <CardDescription>Cada agenda inicializa los doce meses del profesional activo.</CardDescription>
            </CardHeader>
            <CardContent>
              {isLoadingAgendas ? (
                <div className="flex gap-2" aria-busy="true">
                  <Skeleton className="h-10 w-28" />
                  <Skeleton className="h-10 w-28" />
                </div>
              ) : agendas?.length ? (
                <div className="space-y-4">
                  <div className="flex flex-wrap gap-2">
                    {agendas.map((agenda) => (
                      <Badge key={agenda.id} variant="outline" className="h-9 gap-2 px-3 text-sm">
                        <Calendar className="size-3.5 text-info" />
                        Año {agenda.anio}
                        <span className="size-1.5 rounded-full bg-success" aria-hidden="true" />
                      </Badge>
                    ))}
                  </div>
                  {!hasCurrentYearAgenda && (
                    <Button type="button" onClick={handleGenerateAgenda} disabled={createAgenda.isPending}>
                      {createAgenda.isPending ? 'Generando...' : `Generar agenda ${currentYear}`}
                    </Button>
                  )}
                </div>
              ) : (
                <EmptyState
                  icon={Calendar}
                  className="border-0 bg-transparent"
                  title="Sin agendas anuales"
                  description={`No se encontraron agendas registradas para el profesional #${professionalContext.id}.`}
                  action={{
                    label: createAgenda.isPending ? 'Generando...' : `Generar agenda ${currentYear}`,
                    onClick: handleGenerateAgenda,
                  }}
                />
              )}
            </CardContent>
          </Card>

          <Card className="shadow-none">
            <CardHeader className="flex-row items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <Clock className="size-4 text-info" />
                  <CardTitle>Estado de los meses</CardTitle>
                </div>
                <CardDescription className="mt-1">Agenda operativa de {currentYear}.</CardDescription>
              </div>
              {months && <Badge variant="secondary">{months.length} meses</Badge>}
            </CardHeader>
            <CardContent>
              {isLoadingMonths ? (
                <div className="space-y-2" aria-busy="true">
                  {Array.from({ length: 5 }).map((_, index) => <Skeleton key={index} className="h-11 w-full" />)}
                </div>
              ) : months?.length ? (
                <div className="overflow-x-auto rounded-xl border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Mes</TableHead>
                        <TableHead>Estado</TableHead>
                        <TableHead>Disponibilidad</TableHead>
                        <TableHead>Repetición</TableHead>
                        <TableHead className="w-16 text-right">Acciones</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {months.map((monthSummary) => (
                        <MonthSettingsRow
                          key={monthSummary.id}
                          monthSummary={monthSummary}
                          year={currentYear}
                          onViewMonth={(monthNum) => navigate(`/profesional/mi-mes?mes=${currentYear}-${String(monthNum).padStart(2, '0')}`)}
                          onActivate={handleActivateMonth}
                          onInactivate={handleInactivateMonth}
                          activatePending={activateMonth.isPending}
                          inactivatePending={inactivateMonth.isPending}
                        />
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ) : (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  Generá la agenda anual de {currentYear} para visualizar la grilla de meses.
                </p>
              )}
            </CardContent>
          </Card>

          <Card className="shadow-none">
            <CardHeader className="flex-row items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <Clock className="size-4 text-info" />
                  <CardTitle>Plantilla semanal</CardTitle>
                </div>
                <CardDescription className="mt-1">Borrador local de horarios habituales.</CardDescription>
              </div>
              <Button type="button" variant="outline" size="sm" onClick={() => navigate('/profesional/mi-semana')}>
                Modificar <ExternalLink data-icon="inline-end" />
              </Button>
            </CardHeader>
            <CardContent>
              {savedWeeklyDraft?.diasSemana?.length ? (
                <div className="space-y-3 rounded-xl border bg-muted/25 p-4">
                  <p className="text-sm font-semibold">{savedWeeklyDraft.diasSemana.length} días de atención configurados</p>
                  <div className="flex flex-wrap gap-2">
                    {savedWeeklyDraft.diasSemana.map((day) => (
                      <Badge key={day.diaSemana} variant="outline" className="h-auto py-1 font-mono text-[11px]">
                        {day.diaSemana}: {day.brechas?.map((gap) => `${gap.horaInicio}-${gap.horaFin}`).join(', ')}
                      </Badge>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-start justify-between gap-3 rounded-xl border border-dashed p-4 sm:flex-row sm:items-center">
                  <span className="text-sm text-muted-foreground">No hay una plantilla semanal guardada en este navegador.</span>
                  <Button type="button" size="sm" onClick={() => navigate('/profesional/mi-semana')}>Crear plantilla</Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card id="perfil" className="shadow-none">
            <CardHeader>
              <div className="flex items-center gap-2">
                <UserRound className="size-4 text-info" />
                <CardTitle>Perfil profesional</CardTitle>
              </div>
              <CardDescription>Identidad utilizada por esta sesión del frontend.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <IntegrationNotice title="Contexto de prueba">
                Podés cambiar el profesional activo desde el selector del encabezado. La selección queda guardada solo en este navegador.
              </IntegrationNotice>
              <dl className="divide-y rounded-xl border">
                <ProfileRow label="ID profesional" value={`#${professionalContext.id}`} mono />
                <ProfileRow label="Nombre" value={professionalContext.name} />
                <ProfileRow label="Especialidad" value={professionalContext.specialty || 'General'} />
                <ProfileRow label="Zona horaria" value={professionalContext.timezone} mono />
                {config && (
                  <>
                    <ProfileRow label="Duración por turno" value={`${config.duracionAproximadaPorTurno} min`} />
                    <ProfileRow label="Capacidad simultánea" value={`${config.cantidadMaxTurnosALaVez} ${config.cantidadMaxTurnosALaVez === 1 ? 'turno' : 'turnos'}`} />
                    <ProfileRow label="Umbral cancelación" value={config.umbralCancelacionHoras === 0 ? 'Sin límite' : `${config.umbralCancelacionHoras} hs`} />
                    <ProfileRow label="Modalidad agenda" value={config.agendaSoloManejadaPorProfesional ? 'Solo profesional' : 'Autogestión permitida'} />
                  </>
                )}
              </dl>
            </CardContent>
          </Card>

          <Card className="shadow-none">
            <CardHeader>
              <div className="flex items-center gap-2">
                <RotateCcw className="size-4 text-info" />
                <CardTitle>Asistente inicial</CardTitle>
              </div>
              <CardDescription>Volvé a mostrar el recorrido para habilitar una agenda.</CardDescription>
            </CardHeader>
            <CardContent>
              <Button type="button" variant="outline" onClick={handleReopenTutorial}>
                <RotateCcw data-icon="inline-start" /> Reabrir primeros pasos
              </Button>
            </CardContent>
          </Card>

          <Card className="border-warning/25 shadow-none">
            <CardHeader>
              <div className="flex items-center gap-2 text-warning">
                <ShieldAlert className="size-4" />
                <CardTitle>Limitaciones de integración</CardTitle>
              </div>
              <CardDescription>Controles preventivos derivados de los contratos disponibles.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <IntegrationNotice title="Turnos por día o mes">
                La API no provee un listado ni conteo de turnos por fecha. Los indicadores muestran “—” para evitar información engañosa.
              </IntegrationNotice>
              <IntegrationNotice title="Impacto previo">
                La reconfiguración reemplaza brechas sin listar turnos afectados. El frontend bloquea la sobreescritura de meses con atención cargada.
              </IntegrationNotice>
              <IntegrationNotice title="Autenticación">
                No existen endpoints de sesión o perfil. La operativa usa el encabezado X-Usuario y variables de entorno provisionales.
              </IntegrationNotice>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

function ProfileRow({ label, value, mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4 px-4 py-3">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className={`text-right text-sm font-semibold ${mono ? 'font-mono text-xs' : ''}`}>{value}</dd>
    </div>
  );
}

function MonthSettingsRow({
  monthSummary,
  year,
  onViewMonth,
  onActivate,
  onInactivate,
  activatePending,
  inactivatePending,
}) {
  const [confirmOpen, setConfirmOpen] = useState(false);
  const { data: monthDetail, isLoading: isLoadingDetail, error: monthDetailError } = useMonthDetail(monthSummary.id);
  const monthNum = Number(monthSummary.nroMes ?? monthSummary.mes ?? 1);
  const name = MONTH_NAMES[monthNum - 1];
  const rawStatus = monthDetail ? monthDetail.estadoActual : monthSummary.estadoActual;
  const status = normalizeStatus(rawStatus);
  const hasVerifiedDetail = Boolean(monthDetail && Array.isArray(monthDetail.dias));
  const configured = hasVerifiedDetail && isMonthConfigured(monthDetail);
  const repeatEnabled = monthDetail ? monthDetail.repetirConfiguracion : monthSummary.repetirConfiguracion;

  return (
    <>
      <TableRow>
        <TableCell className="font-semibold">{name} {year}</TableCell>
        <TableCell><StatusBadge status={status} /></TableCell>
        <TableCell>
          {isLoadingDetail ? (
            <span className="text-xs text-muted-foreground">Verificando…</span>
          ) : monthDetailError || !hasVerifiedDetail ? (
            <span className="text-xs text-warning">No se pudo verificar</span>
          ) : configured ? (
            <span className="inline-flex items-center gap-1.5 text-xs font-medium text-success">
              <span className="size-1.5 rounded-full bg-success" /> Con franjas
            </span>
          ) : (
            <span className="text-xs text-muted-foreground">Sin franjas</span>
          )}
        </TableCell>
        <TableCell className="text-muted-foreground">{repeatEnabled ? 'Activa' : 'No'}</TableCell>
        <TableCell className="text-right">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button type="button" variant="ghost" size="icon-sm" aria-label={`Acciones de ${name}`}>
                <Ellipsis />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>{name} {year}</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={() => onViewMonth(monthNum)}>Ver mes</DropdownMenuItem>
              {status === 'INACTIVO' && configured && (
                <DropdownMenuItem disabled={activatePending} onSelect={() => onActivate(monthSummary.id, name)}>
                  Activar mes
                </DropdownMenuItem>
              )}
              {status === 'ACTIVO' && (
                <DropdownMenuItem variant="destructive" disabled={inactivatePending} onSelect={() => setConfirmOpen(true)}>
                  Inactivar mes
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </TableCell>
      </TableRow>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={`Inactivar ${name} ${year}`}
        description="El mes dejará de aceptar nuevos turnos. Sus franjas configuradas se conservarán."
        confirmLabel="Inactivar mes"
        variant="danger"
        loading={inactivatePending}
        onConfirm={async () => {
          await onInactivate(monthSummary.id, name);
          setConfirmOpen(false);
        }}
      />
    </>
  );
}
