import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle,
  Calendar,
  CalendarDays,
  CalendarOff,
  CalendarPlus,
  CheckCircle2,
  ChevronDown,
  Clock3,
  ExternalLink,
  Eye,
  Filter,
  Palmtree,
  Plus,
  RefreshCw,
  SlidersHorizontal,
  UserRoundX,
  X,
} from 'lucide-react';
import { DateRangePickerField } from '@/components/DateRangePickerField';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { EmptyState } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { TimeWheelPicker } from '@/components/ui/TimeWheelPicker';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/ToastProvider';
import { useSelectableDays } from '../hooks/useAgenda';
import { useAbsences, useAffectedAppointments, useCancelAbsence, useResolveAffectedBulkCancellation, useResolveAffectedCancellation, useResolveAffectedReschedule } from '../hooks/useAbsences';
import { TYPE_LABELS } from '../utils/exceptionLabels';

const RESOLUTION_LABELS = { PENDIENTE: 'Pendiente', DADO_DE_BAJA: 'Dado de baja', REPROGRAMADO: 'Reprogramado' };
const PAGE_SIZES = [10, 20, 50];
const dateLabel = (value) => value ? new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`)) : '—';
const timeLabel = (value) => value ? new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : '—';

function durationLabel(start, end) {
  if (!start || !end) return '';
  const d1 = new Date(`${start}T00:00:00`);
  const d2 = new Date(`${end}T00:00:00`);
  const diffTime = d2.getTime() - d1.getTime();
  const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24)) + 1;
  return diffDays <= 1 ? '1 día' : `${diffDays} días`;
}

function Pager({ page, setPage, total, size, setSize }) {
  const pages = Math.max(1, Math.ceil(total / size));
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-4">
      <span className="text-xs text-muted-foreground">{total} resultados · Página {page} de {pages}</span>
      <div className="flex items-center gap-2">
        <Select value={String(size)} onValueChange={(v) => { setSize(Number(v)); setPage(1); }}>
          <SelectTrigger className="w-20"><SelectValue /></SelectTrigger>
          <SelectContent>{PAGE_SIZES.map(v => <SelectItem key={v} value={String(v)}>{v}</SelectItem>)}</SelectContent>
        </Select>
        <Button size="sm" variant="outline" disabled={page === 1} onClick={() => setPage(p => p - 1)}>Anterior</Button>
        <Button size="sm" variant="outline" disabled={page === pages} onClick={() => setPage(p => p + 1)}>Siguiente</Button>
      </div>
    </div>
  );
}

const CATEGORIES = [
  { id: 'TODAS', label: 'Todas' },
  { id: 'AUSENCIAS', label: 'Ausencias' },
  { id: 'HABILITACIONES', label: 'Habilitaciones Extraordinarias' },
  { id: 'MODIFICACIONES', label: 'Modificaciones de Horario' },
];

function matchesCategory(tipo, cat) {
  if (cat === 'TODAS') return true;
  if (cat === 'HABILITACIONES') return tipo === 'HABILITACION_EXTRAORDINARIA';
  if (cat === 'MODIFICACIONES') return tipo === 'MODIFICACION_HORARIO' || tipo === 'EXCEPCION_HORARIA';
  if (cat === 'AUSENCIAS') return ['VACACIONES', 'DIA_NO_LABORABLE', 'BLOQUEO_HORARIO', 'FERIADO', 'DIA_DADO_DE_BAJA', 'OTRO'].includes(tipo);
  return true;
}

function getTypeIcon(tipo) {
  if (tipo === 'HABILITACION_EXTRAORDINARIA') return CalendarPlus;
  if (tipo === 'MODIFICACION_HORARIO' || tipo === 'EXCEPCION_HORARIA') return SlidersHorizontal;
  if (tipo === 'BLOQUEO_HORARIO') return Clock3;
  if (tipo === 'VACACIONES') return Palmtree;
  return CalendarOff;
}

export function TypeBadge({ tipo, showIcon = true }) {
  const label = TYPE_LABELS[tipo] || tipo;
  const Icon = getTypeIcon(tipo);
  let colorClasses = 'border-muted text-muted-foreground';
  if (tipo === 'HABILITACION_EXTRAORDINARIA') {
    colorClasses = 'border-emerald-500/30 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300';
  } else if (tipo === 'MODIFICACION_HORARIO' || tipo === 'EXCEPCION_HORARIA') {
    colorClasses = 'border-blue-500/30 bg-blue-50 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300';
  } else if (tipo === 'BLOQUEO_HORARIO') {
    colorClasses = 'border-orange-500/30 bg-orange-50 text-orange-700 dark:bg-orange-950/40 dark:text-orange-300';
  } else if (tipo === 'VACACIONES') {
    colorClasses = 'border-purple-500/30 bg-purple-50 text-purple-700 dark:bg-purple-950/40 dark:text-purple-300';
  } else {
    colorClasses = 'border-amber-500/30 bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300';
  }
  return (
    <Badge variant="outline" className={`inline-flex items-center gap-1.5 font-medium ${colorClasses}`}>
      {showIcon && <Icon className="size-3.5 shrink-0" />}
      <span>{label}</span>
    </Badge>
  );
}

function ExceptionStateBadge({ item }) {
  const today = new Date().toISOString().slice(0, 10);
  if (!item.activa) {
    return (
      <Badge variant="outline" className="border-destructive/30 bg-destructive/10 text-destructive font-medium inline-flex items-center gap-1">
        <span className="size-1.5 rounded-full bg-destructive" />
        Cancelada
      </Badge>
    );
  }
  if (item.fechaInicio > today) {
    return (
      <Badge variant="outline" className="border-sky-500/30 bg-sky-50 text-sky-700 dark:bg-sky-950/40 dark:text-sky-300 font-medium inline-flex items-center gap-1">
        <span className="size-1.5 rounded-full bg-sky-500" />
        Futura
      </Badge>
    );
  }
  if (item.fechaFin < today) {
    return (
      <Badge variant="outline" className="border-muted text-muted-foreground font-medium inline-flex items-center gap-1">
        <span className="size-1.5 rounded-full bg-muted-foreground" />
        Finalizada
      </Badge>
    );
  }
  return (
    <Badge variant="outline" className="border-emerald-500/30 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300 font-medium inline-flex items-center gap-1">
      <span className="size-1.5 rounded-full bg-emerald-500 animate-pulse" />
      Vigente
    </Badge>
  );
}

function getCardBorderClass(tipo) {
  if (tipo === 'HABILITACION_EXTRAORDINARIA') return 'border-l-emerald-500';
  if (tipo === 'MODIFICACION_HORARIO' || tipo === 'EXCEPCION_HORARIA') return 'border-l-blue-500';
  if (tipo === 'BLOQUEO_HORARIO') return 'border-l-orange-500';
  if (tipo === 'VACACIONES') return 'border-l-purple-500';
  return 'border-l-amber-500';
}

function getDatePresetRange(preset) {
  const now = new Date();
  const today = now.toISOString().slice(0, 10);
  if (preset === 'HOY') {
    return { start: today, end: today };
  }
  if (preset === 'ESTE_MES') {
    const year = now.getFullYear();
    const month = now.getMonth();
    const firstDay = new Date(year, month, 1).toISOString().slice(0, 10);
    const lastDay = new Date(year, month + 1, 0).toISOString().slice(0, 10);
    return { start: firstDay, end: lastDay };
  }
  if (preset === '30_DIAS') {
    const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    const next30 = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    return { start: tomorrow, end: next30 };
  }
  if (preset === 'ANIO_ACTUAL') {
    const year = now.getFullYear();
    return { start: `${year}-01-01`, end: `${year}-12-31` };
  }
  return { start: '', end: '' };
}

function KpiCard({ icon: Icon, label, value, subtext, active, onClick, colorClass = 'text-primary' }) {
  return (
    <Card
      onClick={onClick}
      className={`cursor-pointer transition border hover:shadow-sm ${
        active ? 'ring-2 ring-primary border-primary bg-primary/5' : 'hover:border-primary/40'
      }`}
    >
      <CardHeader className="flex flex-row items-center justify-between pb-1 space-y-0">
        <CardTitle className="text-xs font-semibold text-muted-foreground">{label}</CardTitle>
        <Icon className={`size-4 ${colorClass}`} />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold tracking-tight">{value}</div>
        {subtext && <p className="text-[11px] text-muted-foreground mt-0.5">{subtext}</p>}
      </CardContent>
    </Card>
  );
}

export function ExceptionsPanel({ onRegister }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { success, error } = useToast();
  const query = useAbsences();
  const affectedQuery = useAffectedAppointments();
  const cancel = useCancelAbsence();

  const [selected, setSelected] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [category, setCategory] = useState('TODAS');
  const [search, setSearch] = useState('');
  const [state, setState] = useState('TODAS');
  const [datePreset, setDatePreset] = useState('TODOS');
  const [range, setRange] = useState({ start: '', end: '' });
  const [onlyPendingImpact, setOnlyPendingImpact] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);

  const requestedId = searchParams.get('excepcion');
  const requestedException = requestedId && query.data?.find(item => String(item.id) === requestedId);
  const visibleException = selected || requestedException || null;

  // Mapa de afectaciones por ID de excepción
  const affectedByException = useMemo(() => {
    const map = {};
    (affectedQuery.data || []).forEach(app => {
      if (!app.excepcionId) return;
      const exId = String(app.excepcionId);
      if (!map[exId]) map[exId] = { total: 0, pendientes: 0, bajas: 0, reprogramados: 0 };
      map[exId].total += 1;
      if (app.resolucion === 'PENDIENTE') map[exId].pendientes += 1;
      else if (app.resolucion === 'DADO_DE_BAJA') map[exId].bajas += 1;
      else if (app.resolucion === 'REPROGRAMADO') map[exId].reprogramados += 1;
    });
    return map;
  }, [affectedQuery.data]);

  // KPIs superiores (1c)
  const metrics = useMemo(() => {
    const list = query.data || [];
    const today = new Date().toISOString().slice(0, 10);
    const in30 = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

    const vigentesHoy = list.filter(item => item.activa && item.fechaInicio <= today && item.fechaFin >= today).length;
    // Solo excepciones en estado FUTURA que inicien en los próximos 30 días
    const proximos30 = list.filter(item => item.activa && item.fechaInicio > today && item.fechaInicio <= in30).length;
    const conPendientes = list.filter(item => {
      const aff = affectedByException[String(item.id)];
      return aff && aff.pendientes > 0;
    }).length;

    return {
      total: list.length,
      vigentesHoy,
      proximos30,
      conPendientes,
    };
  }, [affectedByException, query.data]);

  const filtered = useMemo(() => (query.data || []).filter(item => {
    const today = new Date().toISOString().slice(0, 10);
    const temporal = !item.activa ? 'CANCELADA' : item.fechaInicio > today ? 'FUTURA' : item.fechaFin < today ? 'FINALIZADA' : 'VIGENTE';
    const text = `${item.id} ${item.motivo} ${TYPE_LABELS[item.tipo] || item.tipo}`.toLowerCase();
    const aff = affectedByException[String(item.id)];

    const matchesSearch = !search || text.includes(search.toLowerCase());
    const matchesState = state === 'TODAS' || temporal === state;
    const matchesCat = matchesCategory(item.tipo, category);
    const matchesStart = !range.start || item.fechaFin >= range.start;
    const matchesEnd = !range.end || item.fechaInicio <= range.end;
    const matchesPending = !onlyPendingImpact || (aff && aff.pendientes > 0);
    const matchesFuturePreset = datePreset !== '30_DIAS' || temporal === 'FUTURA';

    return matchesSearch && matchesState && matchesCat && matchesStart && matchesEnd && matchesPending && matchesFuturePreset;
  }).sort((a, b) => {
    // Ordenar de la más reciente a la más antigua desde cuándo se registró en sí (fechaCreacion / id)
    const timeB = b.fechaCreacion ? new Date(b.fechaCreacion).getTime() : 0;
    const timeA = a.fechaCreacion ? new Date(a.fechaCreacion).getTime() : 0;
    if (timeB !== timeA) return timeB - timeA;
    return (b.id || 0) - (a.id || 0);
  }), [affectedByException, category, datePreset, onlyPendingImpact, query.data, range, search, state]);

  const rows = filtered.slice((page - 1) * size, page * size);

  const doCancel = async () => {
    try {
      await cancel.mutateAsync(cancelTarget.id);
      success('Excepción cancelada', 'Se conservó su historial y sus turnos resueltos.');
      setCancelTarget(null);
      setSelected(null);
    } catch (e) {
      error('No se pudo cancelar', e.message);
    }
  };

  const handleDatePresetChange = (preset) => {
    setDatePreset(preset);
    setPage(1);
    if (preset === '30_DIAS') {
      setState('FUTURA');
      setRange(getDatePresetRange('30_DIAS'));
    } else if (preset !== 'CUSTOM') {
      if (state === 'FUTURA' && datePreset === '30_DIAS') {
        setState('TODAS');
      }
      setRange(getDatePresetRange(preset));
    }
  };

  const activeAdvancedCount = (state !== 'TODAS' ? 1 : 0)
    + (datePreset !== 'TODOS' ? 1 : 0)
    + (onlyPendingImpact ? 1 : 0);

  const hasAnyFilter = category !== 'TODAS'
    || search.trim() !== ''
    || state !== 'TODAS'
    || datePreset !== 'TODOS'
    || Boolean(range.start || range.end)
    || onlyPendingImpact;

  const resetFilters = () => {
    setCategory('TODAS');
    setSearch('');
    setState('TODAS');
    setDatePreset('TODOS');
    setRange({ start: '', end: '' });
    setOnlyPendingImpact(false);
    setPage(1);
  };

  if (query.isLoading) {
    return <Card><CardContent className="p-8" role="status">Cargando excepciones…</CardContent></Card>;
  }

  return (
    <div className="space-y-5">
      {/* 1c: Métricas de resumen superior */}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          icon={CheckCircle2}
          label="Vigentes hoy"
          value={metrics.vigentesHoy}
          subtext="Excepciones activas ahora"
          active={state === 'VIGENTE'}
          colorClass="text-emerald-600"
          onClick={() => {
            if (state === 'VIGENTE') {
              setState('TODAS');
            } else {
              setState('VIGENTE');
            }
            setPage(1);
          }}
        />
        <KpiCard
          icon={CalendarDays}
          label="Próximos 30 días"
          value={metrics.proximos30}
          subtext="Con impacto a corto plazo"
          active={datePreset === '30_DIAS'}
          colorClass="text-sky-600"
          onClick={() => {
            if (datePreset === '30_DIAS') {
              handleDatePresetChange('TODOS');
            } else {
              handleDatePresetChange('30_DIAS');
            }
          }}
        />
        <KpiCard
          icon={AlertTriangle}
          label="Con turnos pendientes"
          value={metrics.conPendientes}
          subtext="Requieren resolución"
          active={onlyPendingImpact}
          colorClass="text-amber-600"
          onClick={() => {
            setOnlyPendingImpact(v => !v);
            setPage(1);
          }}
        />
        <KpiCard
          icon={Calendar}
          label="Total registradas"
          value={metrics.total}
          subtext="Histórico en la agenda"
          active={!hasAnyFilter}
          colorClass="text-primary"
          onClick={resetFilters}
        />
      </div>

      {/* Cabecera de Categorías y Botón de Creación (2c) */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-2">
          {CATEGORIES.map(cat => (
            <Button
              key={cat.id}
              size="sm"
              variant={category === cat.id ? 'default' : 'outline'}
              onClick={() => { setCategory(cat.id); setPage(1); }}
            >
              {cat.label}
            </Button>
          ))}
        </div>

        {/* 2c: DropdownMenu Crear Nueva Excepción */}
        <div className="flex items-center gap-2">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button className="gap-2 font-semibold">
                <Plus className="size-4" />
                Nueva excepción
                <ChevronDown className="size-4 opacity-70" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-64">
              <DropdownMenuItem
                onClick={() => onRegister ? onRegister() : navigate('/profesional/ausencias/registrar')}
                className="cursor-pointer py-2"
              >
                <Palmtree className="size-4 mr-2 text-purple-600" />
                <div>
                  <div className="font-semibold text-sm">Ausencia / Vacaciones</div>
                  <p className="text-xs text-muted-foreground">Bloqueo continuo o por franjas</p>
                </div>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => navigate('/profesional/ausencias/habilitaciones')}
                className="cursor-pointer py-2"
              >
                <CalendarPlus className="size-4 mr-2 text-emerald-600" />
                <div>
                  <div className="font-semibold text-sm">Habilitación Extraordinaria</div>
                  <p className="text-xs text-muted-foreground">Atención en días no habituales</p>
                </div>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => navigate('/profesional/ausencias/modificaciones')}
                className="cursor-pointer py-2"
              >
                <SlidersHorizontal className="size-4 mr-2 text-blue-600" />
                <div>
                  <div className="font-semibold text-sm">Modificación de Horario</div>
                  <p className="text-xs text-muted-foreground">Reemplazo de jornada puntual</p>
                </div>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* 3c: Barra Rápida + Panel Colapsable de Filtros Avanzados */}
      <div className="space-y-3">
        <div className="flex flex-col gap-2 rounded-xl border bg-card p-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Input
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(1); }}
              placeholder="Buscar por motivo, tipo o identificador..."
              className="pr-8"
            />
            {search && (
              <button
                type="button"
                onClick={() => { setSearch(''); setPage(1); }}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                <X className="size-4" />
              </button>
            )}
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant={showAdvanced ? 'secondary' : 'outline'}
              size="sm"
              onClick={() => setShowAdvanced(v => !v)}
              className="gap-1.5"
            >
              <SlidersHorizontal className="size-4" />
              <span>Filtros avanzados</span>
              {activeAdvancedCount > 0 && (
                <Badge variant="default" className="size-5 p-0 justify-center text-[10px] rounded-full">
                  {activeAdvancedCount}
                </Badge>
              )}
            </Button>

            {hasAnyFilter && (
              <Button
                variant="ghost"
                size="sm"
                onClick={resetFilters}
                className="text-muted-foreground hover:text-foreground gap-1"
              >
                <X className="size-4" />
                Limpiar
              </Button>
            )}

            <Button variant="outline" size="sm" onClick={() => query.refetch()} title="Actualizar datos">
              <RefreshCw className="size-4" />
              <span className="hidden sm:inline">Actualizar</span>
            </Button>
          </div>
        </div>

        {/* Panel Colapsable de Filtros Avanzados */}
        {showAdvanced && (
          <div className="rounded-xl border bg-muted/20 p-4 space-y-4 animate-in fade-in-50 slide-in-from-top-1 duration-150">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <Label className="text-xs font-semibold mb-1.5 block">Estado de la excepción</Label>
                <Select value={state} onValueChange={v => { setState(v); setPage(1); }}>
                  <SelectTrigger><SelectValue placeholder="Estado" /></SelectTrigger>
                  <SelectContent>
                    {['TODAS', 'VIGENTE', 'FUTURA', 'FINALIZADA', 'CANCELADA'].map(v => (
                      <SelectItem key={v} value={v}>{v.charAt(0) + v.slice(1).toLowerCase()}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-xs font-semibold mb-1.5 block">Período temporal rápido</Label>
                <Select value={datePreset} onValueChange={handleDatePresetChange}>
                  <SelectTrigger><SelectValue placeholder="Período" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="TODOS">Cualquier fecha</SelectItem>
                    <SelectItem value="HOY">Hoy</SelectItem>
                    <SelectItem value="ESTE_MES">Este mes</SelectItem>
                    <SelectItem value="30_DIAS">Próximos 30 días</SelectItem>
                    <SelectItem value="ANIO_ACTUAL">Año en curso</SelectItem>
                    <SelectItem value="CUSTOM">Rango personalizado…</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="flex flex-col justify-end">
                <label className="flex items-center gap-2 cursor-pointer text-sm font-medium p-2 rounded-lg border bg-card hover:bg-muted/40 transition">
                  <input
                    type="checkbox"
                    checked={onlyPendingImpact}
                    onChange={e => { setOnlyPendingImpact(e.target.checked); setPage(1); }}
                    className="size-4 rounded text-primary"
                  />
                  <span>Solo con turnos pendientes</span>
                </label>
              </div>
            </div>

            {datePreset === 'CUSTOM' && (
              <div className="pt-3 border-t">
                <Label className="text-xs font-semibold mb-1.5 block">Seleccionar rango personalizado</Label>
                <DateRangePickerField
                  start={range.start}
                  end={range.end}
                  onChange={(start, end) => { setRange({ start, end }); setPage(1); }}
                />
              </div>
            )}
          </div>
        )}
      </div>

      {/* Listado / Tabla (4b, 5c, 8c) */}
      {!filtered.length ? (
        <EmptyState
          icon={CalendarDays}
          title="No hay excepciones para mostrar"
          description="Probá cambiar los filtros o registrá una nueva excepción."
          action={{
            label: 'Registrar una excepción',
            onClick: onRegister ? onRegister : () => navigate('/profesional/ausencias/registrar'),
          }}
        />
      ) : (
        <Card>
          <CardContent className="space-y-4 p-4">
            {/* Desktop Table (4b) */}
            <div className="hidden md:block overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tipo</TableHead>
                    <TableHead>Período</TableHead>
                    <TableHead>Motivo</TableHead>
                    <TableHead>Impacto en turnos</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead className="text-right">Acciones</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map(item => {
                    const affected = affectedByException[String(item.id)];
                    return (
                      <TableRow key={item.id} className="hover:bg-muted/30">
                        <TableCell>
                          <div className="space-y-1">
                            <TypeBadge tipo={item.tipo} />
                            <span className="block text-[11px] font-mono text-muted-foreground">#{item.id}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div>
                            <span className="font-medium">{dateLabel(item.fechaInicio)} – {dateLabel(item.fechaFin)}</span>
                            <div className="flex flex-wrap items-center gap-1.5 mt-1">
                              <Badge variant="secondary" className="text-[10px] font-medium h-5 px-1.5">
                                {durationLabel(item.fechaInicio, item.fechaFin)}
                              </Badge>
                              {item.brechas?.length > 0 ? (
                                <span className="text-xs text-muted-foreground">
                                  {item.brechas.map(b => `${b.horaInicio}–${b.horaFin}`).join(', ')}
                                </span>
                              ) : (
                                <span className="text-xs text-muted-foreground">Día completo</span>
                              )}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="max-w-64">
                          <div className="truncate font-medium" title={item.motivo}>
                            {item.motivo}
                          </div>
                          {item.fechasExcluidas?.length > 0 && (
                            <span className="block text-[11px] text-muted-foreground mt-0.5">
                              {item.fechasExcluidas.length} día(s) excluido(s)
                            </span>
                          )}
                        </TableCell>
                        <TableCell>
                          {affected && affected.total > 0 ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => navigate(`/profesional/turnos-afectados?excepcion=${item.id}`)}
                              className={`h-7 px-2 text-xs font-semibold rounded-md border ${
                                affected.pendientes > 0
                                  ? 'border-amber-400/60 bg-amber-50 text-amber-900 hover:bg-amber-100 dark:bg-amber-950/40 dark:text-amber-300'
                                  : 'border-muted bg-muted/40 text-foreground hover:bg-muted'
                              }`}
                              title="Ver turnos afectados por esta excepción"
                            >
                              <UserRoundX className="size-3 mr-1 text-amber-600 dark:text-amber-400" />
                              {affected.total} turnos ({affected.pendientes} pend.)
                            </Button>
                          ) : (
                            <span className="text-xs text-muted-foreground/80">Sin turnos afectados</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <ExceptionStateBadge item={item} />
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-1">
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-8 px-2.5"
                              onClick={() => setSelected(item)}
                            >
                              <Eye className="size-3.5 mr-1" />
                              Ver
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-8 w-8 p-0"
                              title="Ver en Mi mes"
                              onClick={() => navigate(`/profesional/mi-mes?mes=${item.fechaInicio.slice(0, 7)}&excepcion=${item.id}`)}
                            >
                              <CalendarDays className="size-4 text-muted-foreground hover:text-foreground" />
                            </Button>
                            {item.activa && (
                              <Button
                                size="sm"
                                variant="ghost"
                                className="h-8 w-8 p-0 text-destructive hover:bg-destructive/10 hover:text-destructive"
                                title="Cancelar excepción"
                                onClick={() => setCancelTarget(item)}
                              >
                                <UserRoundX className="size-4" />
                              </Button>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>

            {/* Mobile Cards (8c) */}
            <div className="grid gap-3 md:hidden">
              {rows.map(item => {
                const affected = affectedByException[String(item.id)];
                const borderClass = getCardBorderClass(item.tipo);
                return (
                  <div
                    key={item.id}
                    className={`rounded-xl border border-l-4 ${borderClass} bg-card p-4 space-y-3 shadow-xs`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1">
                        <TypeBadge tipo={item.tipo} />
                        <span className="block text-[11px] font-mono text-muted-foreground">#{item.id}</span>
                      </div>
                      <ExceptionStateBadge item={item} />
                    </div>

                    <div>
                      <strong className="block text-sm font-semibold">{item.motivo}</strong>
                      <span className="mt-1 block text-xs text-muted-foreground">
                        {dateLabel(item.fechaInicio)} – {dateLabel(item.fechaFin)} · {durationLabel(item.fechaInicio, item.fechaFin)}
                      </span>
                      {item.brechas?.length > 0 && (
                        <span className="mt-1 block text-xs font-mono text-muted-foreground">
                          Franjas: {item.brechas.map(b => `${b.horaInicio}–${b.horaFin}`).join(', ')}
                        </span>
                      )}
                    </div>

                    {affected && affected.total > 0 && (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => navigate(`/profesional/turnos-afectados?excepcion=${item.id}`)}
                        className="w-full justify-start h-7 px-2 text-xs font-medium border border-amber-300/50 bg-amber-50/50 text-amber-900 dark:bg-amber-950/30 dark:text-amber-300"
                      >
                        <UserRoundX className="size-3 mr-1 text-amber-600" />
                        {affected.total} turnos afectados ({affected.pendientes} pendientes)
                      </Button>
                    )}

                    <div className="flex items-center justify-between gap-2 border-t pt-2">
                      <Button size="sm" variant="outline" className="flex-1 h-8" onClick={() => setSelected(item)}>
                        <Eye className="size-3.5 mr-1" />
                        Ver detalle
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="h-8 px-2"
                        title="Ver en Mi mes"
                        onClick={() => navigate(`/profesional/mi-mes?mes=${item.fechaInicio.slice(0, 7)}&excepcion=${item.id}`)}
                      >
                        <CalendarDays className="size-4" />
                      </Button>
                      {item.activa && (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-8 px-2 text-destructive hover:bg-destructive/10"
                          title="Cancelar excepción"
                          onClick={() => setCancelTarget(item)}
                        >
                          <UserRoundX className="size-4" />
                        </Button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            <Pager page={page} setPage={setPage} total={filtered.length} size={size} setSize={setSize} />
          </CardContent>
        </Card>
      )}

      {/* 6b: Sheet lateral modular */}
      <Sheet
        open={Boolean(visibleException)}
        onOpenChange={open => {
          if (!open) {
            setSelected(null);
            if (requestedId) navigate('/profesional/ausencias/excepciones', { replace: true });
          }
        }}
      >
        <SheetContent className="w-full overflow-y-auto sm:max-w-xl">
          <SheetHeader>
            <SheetTitle className="text-lg flex items-center gap-2">
              <span>Detalle de la excepción #{visibleException?.id}</span>
            </SheetTitle>
            <SheetDescription>Configuración registrada, alcance temporal e impacto en la agenda.</SheetDescription>
          </SheetHeader>
          {visibleException && (
            <div className="space-y-5 px-4 mt-4">
              {/* Encabezado con Badges */}
              <div className="flex flex-wrap items-center gap-2 pb-2 border-b">
                <TypeBadge tipo={visibleException.tipo} />
                <ExceptionStateBadge item={visibleException} />
              </div>

              {/* Bloque 1: Período y Franjas */}
              <div className="rounded-xl border bg-muted/20 p-4 space-y-3">
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                  <Calendar className="size-3.5 text-primary" />
                  Período y Horarios
                </h4>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Fechas:</span>
                    <span className="font-semibold">{dateLabel(visibleException.fechaInicio)} – {dateLabel(visibleException.fechaFin)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Duración:</span>
                    <span className="font-semibold">{durationLabel(visibleException.fechaInicio, visibleException.fechaFin)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Franjas horarias:</span>
                    <span className="font-semibold">
                      {visibleException.brechas?.length
                        ? visibleException.brechas.map(b => `${b.horaInicio}–${b.horaFin}`).join(', ')
                        : 'Día completo'}
                    </span>
                  </div>
                  {visibleException.fechasExcluidas?.length > 0 && (
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Fechas excluidas:</span>
                      <span className="font-semibold">{visibleException.fechasExcluidas.map(dateLabel).join(', ')}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Bloque 2: Motivo */}
              <div className="rounded-xl border bg-muted/20 p-4 space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Motivo registrado</h4>
                <p className="text-sm font-medium leading-relaxed">{visibleException.motivo}</p>
              </div>

              {/* Bloque 3: Impacto en Turnos (7b) */}
              {(() => {
                const aff = affectedByException[String(visibleException.id)];
                return (
                  <div className="rounded-xl border bg-muted/20 p-4 space-y-3">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                      <UserRoundX className="size-3.5 text-primary" />
                      Impacto en turnos de pacientes
                    </h4>
                    {aff && aff.total > 0 ? (
                      <div className="space-y-3">
                        <div className="grid grid-cols-3 gap-2 text-center">
                          <div className="rounded-lg border bg-card p-2">
                            <span className="text-[10px] uppercase font-semibold text-muted-foreground">Total</span>
                            <strong className="block text-lg font-bold">{aff.total}</strong>
                          </div>
                          <div className="rounded-lg border bg-amber-50 dark:bg-amber-950/30 p-2 text-amber-900 dark:text-amber-200">
                            <span className="text-[10px] uppercase font-semibold">Pendientes</span>
                            <strong className="block text-lg font-bold">{aff.pendientes}</strong>
                          </div>
                          <div className="rounded-lg border bg-card p-2">
                            <span className="text-[10px] uppercase font-semibold text-muted-foreground">Resueltos</span>
                            <strong className="block text-lg font-bold">{aff.bajas + aff.reprogramados}</strong>
                          </div>
                        </div>
                        <Button
                          size="sm"
                          className="w-full gap-2 font-semibold"
                          onClick={() => navigate(`/profesional/turnos-afectados?excepcion=${visibleException.id}`)}
                        >
                          <ExternalLink className="size-4" />
                          Gestionar turnos afectados (#{visibleException.id})
                        </Button>
                      </div>
                    ) : (
                      <p className="text-xs text-muted-foreground">Esta excepción no tuvo turnos afectados o ya no hay turnos registrados en ese período.</p>
                    )}
                  </div>
                );
              })()}

              {/* Bloque 4: Trazabilidad */}
              <div className="rounded-xl border bg-muted/20 p-4 space-y-2 text-xs">
                <h4 className="font-bold uppercase tracking-wider text-muted-foreground">Trazabilidad</h4>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Creada el:</span>
                  <span>{visibleException.fechaCreacion ? new Date(visibleException.fechaCreacion).toLocaleString('es-AR') : '—'}</span>
                </div>
                {visibleException.fechaModificacion && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Última modificación:</span>
                    <span>{new Date(visibleException.fechaModificacion).toLocaleString('es-AR')}</span>
                  </div>
                )}
              </div>

              {/* Bloque 5: Acciones */}
              <div className="flex flex-wrap gap-2 border-t pt-4">
                <Button
                  variant="outline"
                  className="gap-2"
                  onClick={() => navigate(`/profesional/mi-mes?mes=${visibleException.fechaInicio.slice(0, 7)}&excepcion=${visibleException.id}`)}
                >
                  <CalendarDays className="size-4" />
                  Ver en Mi mes
                </Button>
                {visibleException.activa && (
                  <Button
                    variant="destructive"
                    className="gap-2"
                    onClick={() => setCancelTarget(visibleException)}
                  >
                    <UserRoundX className="size-4" />
                    Cancelar excepción
                  </Button>
                )}
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        onOpenChange={open => !open && setCancelTarget(null)}
        title="Cancelar excepción"
        description="La excepción quedará cancelada. Los turnos dados de baja no se reactivarán automáticamente."
        confirmLabel="Confirmar cancelación"
        variant="destructive"
        onConfirm={doCancel}
      />
    </div>
  );
}

export function AffectedAppointmentsPanel() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { success, error } = useToast();
  const query = useAffectedAppointments();
  const cancelOne = useResolveAffectedCancellation();
  const cancelMany = useResolveAffectedBulkCancellation();
  const reprogram = useResolveAffectedReschedule();
  const [search,setSearch]=useState(''); const [resolution,setResolution]=useState('PENDIENTE'); const [type,setType]=useState('TODOS');
  const [range,setRange]=useState({start:'',end:''}); const [page,setPage]=useState(1); const [size,setSize]=useState(20); const [selected,setSelected]=useState(null); const [checked,setChecked]=useState([]); const [cancelTarget,setCancelTarget]=useState(null); const [rescheduleTarget,setRescheduleTarget]=useState(null);
  const exceptionFilter = searchParams.get('excepcion');
  const filtered=useMemo(()=>(query.data||[]).filter(item=>{const text=`${item.turnoId} ${item.excepcionId} ${item.nombreCliente} ${item.telefono}`.toLowerCase();return(!exceptionFilter||String(item.excepcionId)===exceptionFilter)&&(!search||text.includes(search.toLowerCase()))&&(resolution==='TODOS'||item.resolucion===resolution)&&(type==='TODOS'||item.tipoExcepcion===type)&&(!range.start||item.fechaOriginal>=range.start)&&(!range.end||item.fechaOriginal<=range.end);}).sort((a,b)=>(a.resolucion==='PENDIENTE'?0:1)-(b.resolucion==='PENDIENTE'?0:1)||a.fechaOriginal.localeCompare(b.fechaOriginal)),[exceptionFilter,query.data,range,resolution,search,type]);
  const rows=filtered.slice((page-1)*size,page*size); const pending=(query.data||[]).filter(x=>x.resolucion==='PENDIENTE').length; const low=(query.data||[]).filter(x=>x.resolucion==='DADO_DE_BAJA').length; const moved=(query.data||[]).filter(x=>x.resolucion==='REPROGRAMADO').length;
  const doCancel=async()=>{try{if(cancelTarget==='bulk'){await cancelMany.mutateAsync({ids:checked,observacion:'Resolución masiva desde Turnos afectados'});setChecked([]);}else await cancelOne.mutateAsync({id:cancelTarget.afectacionId,observacion:''});success('Resolución completada','Los turnos fueron dados de baja y se generaron las notificaciones correspondientes.');setCancelTarget(null);setSelected(null);}catch(e){error('No se pudo resolver',e.message);}};
  const doReschedule=async value=>{try{await reprogram.mutateAsync({id:rescheduleTarget.afectacionId,nuevoDiaAgendaId:Number(value.diaAgendaId),nuevoInicio:localToInstant(value.fecha,value.horaInicio),nuevoFin:localToInstant(value.fecha,value.horaFin),observacion:value.observacion});success('Turno reprogramado','El cambio quedó registrado y se generó la notificación.');setRescheduleTarget(null);setSelected(null);}catch(e){error('No se pudo reprogramar',e.message);}};
  return <div className="space-y-4">{exceptionFilter&&<div className="flex items-center justify-between rounded-xl border border-info/30 bg-info/10 p-3"><span className="text-sm font-semibold">Filtrando por la excepción #{exceptionFilter}</span><Button variant="ghost" size="sm" onClick={()=>navigate('/profesional/turnos-afectados')}>Quitar filtro</Button></div>}<div className="grid gap-3 sm:grid-cols-3 xl:grid-cols-4"><Metric label="Total" value={(query.data||[]).length}/><Metric label="Pendientes" value={pending}/><Metric label="Dados de baja" value={low}/><Metric label="Reprogramados" value={moved}/></div><div className="grid gap-3 rounded-xl border bg-card p-4 lg:grid-cols-[1fr_170px_220px_2fr_auto]"><Input placeholder="Paciente, teléfono, turno o excepción" value={search} onChange={e=>setSearch(e.target.value)}/><Select value={resolution} onValueChange={setResolution}><SelectTrigger><SelectValue/></SelectTrigger><SelectContent>{['TODOS','PENDIENTE','DADO_DE_BAJA','REPROGRAMADO'].map(v=><SelectItem key={v} value={v}>{RESOLUTION_LABELS[v]||'Todas'}</SelectItem>)}</SelectContent></Select><Select value={type} onValueChange={setType}><SelectTrigger><SelectValue/></SelectTrigger><SelectContent><SelectItem value="TODOS">Todos los tipos</SelectItem>{Object.entries(TYPE_LABELS).map(([v,l])=><SelectItem key={v} value={v}>{l}</SelectItem>)}</SelectContent></Select><DateRangePickerField start={range.start} end={range.end} onChange={(start,end)=>setRange({start,end})}/><Button variant="outline" onClick={()=>query.refetch()}><RefreshCw/>Actualizar</Button></div>
  {checked.length>0&&<div className="flex items-center justify-between rounded-xl border border-warning/30 bg-warning/10 p-3"><span className="text-sm font-semibold">{checked.length} pendientes seleccionados</span><Button variant="destructive" size="sm" onClick={()=>setCancelTarget('bulk')}><UserRoundX/>Dar de baja seleccionados</Button></div>}
  {!query.isLoading&&!filtered.length?<EmptyState icon={CheckCircle2} title="No hay turnos afectados para estos filtros" description="Los pendientes aparecerán aquí cuando una excepción alcance turnos existentes."/>:<Card><CardContent className="space-y-4 p-4"><div className="hidden lg:block"><Table><TableHeader><TableRow><TableHead className="w-10"></TableHead><TableHead>Paciente</TableHead><TableHead>Fecha y hora original</TableHead><TableHead>Excepción</TableHead><TableHead>Estado</TableHead><TableHead>Resolución</TableHead><TableHead></TableHead></TableRow></TableHeader><TableBody>{rows.map(item=><TableRow key={item.afectacionId}><TableCell>{item.resolucion==='PENDIENTE'&&<input type="checkbox" aria-label={`Seleccionar turno ${item.turnoId}`} checked={checked.includes(item.afectacionId)} onChange={()=>setChecked(v=>v.includes(item.afectacionId)?v.filter(id=>id!==item.afectacionId):[...v,item.afectacionId])}/>}</TableCell><TableCell><strong>{item.nombreCliente}</strong><span className="block text-xs text-muted-foreground">{item.telefono||'Contacto manual'}</span></TableCell><TableCell>{dateLabel(item.fechaOriginal)}<span className="block text-xs text-muted-foreground">{timeLabel(item.inicioOriginal)}–{timeLabel(item.finOriginal)}</span></TableCell><TableCell><TypeBadge tipo={item.tipoExcepcion} /><span className="block text-xs">#{item.excepcionId}</span></TableCell><TableCell>{item.estadoTurnoAnterior||'—'} → {item.estadoTurno||'—'}</TableCell><TableCell><Badge>{RESOLUTION_LABELS[item.resolucion]||item.resolucion}</Badge></TableCell><TableCell><Button size="sm" variant="ghost" onClick={()=>setSelected(item)}><Eye/>Ver</Button></TableCell></TableRow>)}</TableBody></Table></div><div className="grid gap-3 lg:hidden">{rows.map(item=><button key={item.afectacionId} onClick={()=>setSelected(item)} className="rounded-xl border p-4 text-left"><div className="flex justify-between"><strong>{item.nombreCliente}</strong><Badge>{RESOLUTION_LABELS[item.resolucion]}</Badge></div><span className="mt-2 block text-xs">{dateLabel(item.fechaOriginal)} · {timeLabel(item.inicioOriginal)}</span><span className="mt-1 block text-xs text-muted-foreground">{TYPE_LABELS[item.tipoExcepcion]}</span></button>)}</div><Pager page={page} setPage={setPage} total={filtered.length} size={size} setSize={setSize}/></CardContent></Card>}
  <Sheet open={Boolean(selected)} onOpenChange={open=>!open&&setSelected(null)}><SheetContent className="w-full overflow-y-auto sm:max-w-xl"><SheetHeader><SheetTitle>Turno afectado #{selected?.turnoId}</SheetTitle><SheetDescription>Origen, estado y resolución del impacto.</SheetDescription></SheetHeader>{selected&&<div className="space-y-4 px-4"><Detail label="Paciente" value={`${selected.nombreCliente} · ${selected.telefono||'Contacto manual'}`}/><Detail label="Excepción" value={`#${selected.excepcionId} · ${TYPE_LABELS[selected.tipoExcepcion]} · ${selected.motivoExcepcion}`}/><Detail label="Horario original" value={`${dateLabel(selected.fechaOriginal)} · ${timeLabel(selected.inicioOriginal)}–${timeLabel(selected.finOriginal)}`}/><Detail label="Estado" value={`${selected.estadoTurnoAnterior||'—'} → ${selected.estadoTurno||'—'}`}/>{selected.resolucion==='REPROGRAMADO'&&<Detail label="Nuevo horario" value={`${dateLabel(selected.fechaActual)} · ${timeLabel(selected.inicioActual)}–${timeLabel(selected.finActual)}`}/>}<Detail label="Resolución" value={RESOLUTION_LABELS[selected.resolucion]}/><Detail label="Notificación" value={selected.telefono?'WhatsApp generado al resolver':'Requiere contacto manual'}/>{selected.resolucion==='PENDIENTE'&&<div className="flex gap-2 border-t pt-4"><Button variant="destructive" onClick={()=>setCancelTarget(selected)}>Dar de baja</Button><Button onClick={()=>setRescheduleTarget(selected)}><Clock3/>Reprogramar</Button></div>}</div>}</SheetContent></Sheet>
  <ConfirmDialog open={Boolean(cancelTarget)} onOpenChange={open=>!open&&setCancelTarget(null)} title="Confirmar baja" description={cancelTarget==='bulk'?`Se darán de baja ${checked.length} turnos, se registrará el historial y se generarán las notificaciones.`:'El turno será dado de baja, con historial y notificación asociada.'} confirmLabel="Dar de baja" variant="destructive" onConfirm={doCancel}/><AffectedRescheduleDialog item={rescheduleTarget} onClose={()=>setRescheduleTarget(null)} onConfirm={doReschedule}/></div>;
}

function AffectedRescheduleDialog({item,onClose,onConfirm}) {
  const [form,setForm]=useState({fecha:'',horaInicio:'',horaFin:'',observacion:''});
  const {data:days}=useSelectableDays(form.fecha,form.fecha);
  const day=days?.find(d=>d.fecha===form.fecha&&d.seleccionable);
  const valid=day&&form.horaInicio<form.horaFin;
  return (
    <ConfirmDialog
      open={Boolean(item)}
      onOpenChange={open=>!open&&onClose()}
      title="Reprogramar turno afectado"
      description="Solo se aceptará un día habilitado con disponibilidad y capacidad."
      confirmLabel="Confirmar reprogramación"
      confirmDisabled={!valid}
      onConfirm={()=>onConfirm({...form,diaAgendaId:day?.diaAgendaId})}
    >
      <div className="grid gap-3 py-2 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <Label>Fecha</Label>
          <Input type="date" value={form.fecha} onChange={e=>setForm({...form,fecha:e.target.value})}/>
        </div>
        <div>
          <Label>Desde</Label>
          <TimeWheelPicker
            value={form.horaInicio}
            onChange={val=>setForm({...form,horaInicio:val})}
            minuteStep={15}
            aria-label="Hora desde"
          />
        </div>
        <div>
          <Label>Hasta</Label>
          <TimeWheelPicker
            value={form.horaFin}
            onChange={val=>setForm({...form,horaFin:val})}
            minuteStep={15}
            aria-label="Hora hasta"
          />
        </div>
        <div className="sm:col-span-2">
          <Label>Observación</Label>
          <Textarea value={form.observacion} onChange={e=>setForm({...form,observacion:e.target.value})}/>
        </div>
      </div>
    </ConfirmDialog>
  );
}
function Detail({label,value}) { return <div><span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</span><p className="mt-1 leading-6">{value||'—'}</p></div>; }
function Metric({label,value}) { return <Card><CardHeader className="pb-1"><CardTitle className="text-xs text-muted-foreground">{label}</CardTitle></CardHeader><CardContent><strong className="text-2xl">{value}</strong></CardContent></Card>; }
function localToInstant(date,time){return new Date(`${date}T${time}:00`).toISOString();}
