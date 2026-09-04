import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CalendarDays, CheckCircle2, Clock3, Eye, RefreshCw, UserRoundX } from 'lucide-react';
import { DateRangePickerField } from '@/components/DateRangePickerField';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
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

function Pager({ page, setPage, total, size, setSize }) {
  const pages = Math.max(1, Math.ceil(total / size));
  return <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-4"><span className="text-xs text-muted-foreground">{total} resultados · Página {page} de {pages}</span><div className="flex items-center gap-2"><Select value={String(size)} onValueChange={(v)=>{setSize(Number(v));setPage(1);}}><SelectTrigger className="w-20"><SelectValue/></SelectTrigger><SelectContent>{PAGE_SIZES.map(v=><SelectItem key={v} value={String(v)}>{v}</SelectItem>)}</SelectContent></Select><Button size="sm" variant="outline" disabled={page===1} onClick={()=>setPage(p=>p-1)}>Anterior</Button><Button size="sm" variant="outline" disabled={page===pages} onClick={()=>setPage(p=>p+1)}>Siguiente</Button></div></div>;
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

export function TypeBadge({ tipo }) {
  const label = TYPE_LABELS[tipo] || tipo;
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
  return <Badge variant="outline" className={colorClasses}>{label}</Badge>;
}

function ExceptionState({ item }) {
  const today = new Date().toISOString().slice(0,10);
  const state = !item.activa ? 'Cancelada' : item.fechaInicio > today ? 'Futura' : item.fechaFin < today ? 'Finalizada' : 'Vigente';
  return <Badge variant="outline">{state}</Badge>;
}

export function ExceptionsPanel({ onRegister }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { success, error } = useToast();
  const query = useAbsences();
  const cancel = useCancelAbsence();
  const [selected, setSelected] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [category, setCategory] = useState('TODAS');
  const [search, setSearch] = useState('');
  const [state, setState] = useState('TODAS');
  const [range, setRange] = useState({ start:'', end:'' });
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const requestedId = searchParams.get('excepcion');
  const requestedException = requestedId && query.data?.find(item => String(item.id) === requestedId);
  const visibleException = selected || requestedException || null;
  const filtered = useMemo(() => (query.data || []).filter(item => {
    const today = new Date().toISOString().slice(0,10);
    const temporal = !item.activa ? 'CANCELADA' : item.fechaInicio > today ? 'FUTURA' : item.fechaFin < today ? 'FINALIZADA' : 'VIGENTE';
    const text = `${item.id} ${item.motivo} ${TYPE_LABELS[item.tipo] || item.tipo}`.toLowerCase();
    return (!search || text.includes(search.toLowerCase()))
      && (state === 'TODAS' || temporal === state)
      && matchesCategory(item.tipo, category)
      && (!range.start || item.fechaFin >= range.start)
      && (!range.end || item.fechaInicio <= range.end);
  }).sort((a, b) => {
    const dateComp = (b.fechaInicio || '').localeCompare(a.fechaInicio || '');
    if (dateComp !== 0) return dateComp;
    const endComp = (b.fechaFin || '').localeCompare(a.fechaFin || '');
    if (endComp !== 0) return endComp;
    const timeB = b.fechaCreacion ? new Date(b.fechaCreacion).getTime() : 0;
    const timeA = a.fechaCreacion ? new Date(a.fechaCreacion).getTime() : 0;
    if (timeB !== timeA) return timeB - timeA;
    return (b.id || 0) - (a.id || 0);
  }), [category, query.data, range, search, state]);
  const rows = filtered.slice((page-1)*size, page*size);
  const doCancel = async () => { try { await cancel.mutateAsync(cancelTarget.id); success('Excepción cancelada', 'Se conservó su historial y sus turnos resueltos.'); setCancelTarget(null); setSelected(null); } catch(e) { error('No se pudo cancelar', e.message); } };
  if (query.isLoading) return <Card><CardContent className="p-8" role="status">Cargando excepciones…</CardContent></Card>;
  return <div className="space-y-4">
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
    <div className="grid gap-3 rounded-xl border bg-card p-4 lg:grid-cols-[1fr_180px_2fr_auto]"><Input value={search} onChange={e=>{setSearch(e.target.value);setPage(1);}} placeholder="Buscar por motivo, tipo o identificador"/><Select value={state} onValueChange={v=>{setState(v);setPage(1);}}><SelectTrigger><SelectValue/></SelectTrigger><SelectContent>{['TODAS','VIGENTE','FUTURA','FINALIZADA','CANCELADA'].map(v=><SelectItem key={v} value={v}>{v.charAt(0)+v.slice(1).toLowerCase()}</SelectItem>)}</SelectContent></Select><DateRangePickerField start={range.start} end={range.end} onChange={(start,end)=>{setRange({start,end});setPage(1);}}/><Button variant="outline" onClick={()=>query.refetch()}><RefreshCw/>Actualizar</Button></div>
    {!filtered.length ? <EmptyState icon={CalendarDays} title="No hay excepciones para mostrar" description="Probá cambiar los filtros o registrá una nueva excepción." action={{label:'Registrar una excepción',onClick:onRegister}}/> : <Card><CardContent className="space-y-4 p-4"><div className="hidden md:block"><Table><TableHeader><TableRow><TableHead>Tipo</TableHead><TableHead>Período</TableHead><TableHead>Motivo</TableHead><TableHead>Estado</TableHead><TableHead className="text-right">Acciones</TableHead></TableRow></TableHeader><TableBody>{rows.map(item=><TableRow key={item.id}><TableCell><TypeBadge tipo={item.tipo} /></TableCell><TableCell>{dateLabel(item.fechaInicio)} – {dateLabel(item.fechaFin)}</TableCell><TableCell className="max-w-64 truncate">{item.motivo}</TableCell><TableCell><ExceptionState item={item}/></TableCell><TableCell className="text-right"><Button size="sm" variant="ghost" onClick={()=>setSelected(item)}><Eye/>Ver</Button></TableCell></TableRow>)}</TableBody></Table></div><div className="grid gap-3 md:hidden">{rows.map(item=><button key={item.id} className="rounded-xl border p-4 text-left" onClick={()=>setSelected(item)}><div className="flex justify-between gap-2"><TypeBadge tipo={item.tipo} /><ExceptionState item={item}/></div><strong className="mt-3 block">{item.motivo}</strong><span className="mt-1 block text-xs text-muted-foreground">{dateLabel(item.fechaInicio)} – {dateLabel(item.fechaFin)}</span></button>)}</div><Pager page={page} setPage={setPage} total={filtered.length} size={size} setSize={setSize}/></CardContent></Card>}
    <Sheet open={Boolean(visibleException)} onOpenChange={open=>{if(!open){setSelected(null);if(requestedId)navigate('/profesional/ausencias/excepciones',{replace:true});}}}><SheetContent className="w-full overflow-y-auto sm:max-w-xl"><SheetHeader><SheetTitle>Detalle de la excepción #{visibleException?.id}</SheetTitle><SheetDescription>Configuración registrada y trazabilidad disponible.</SheetDescription></SheetHeader>{visibleException&&<div className="space-y-5 px-4"><div className="flex gap-2"><TypeBadge tipo={visibleException.tipo} /><ExceptionState item={visibleException}/></div><Detail label="Período" value={`${dateLabel(visibleException.fechaInicio)} – ${dateLabel(visibleException.fechaFin)}`}/><Detail label="Motivo" value={visibleException.motivo}/><Detail label="Franjas" value={visibleException.brechas?.length ? visibleException.brechas.map(b=>`${b.horaInicio}–${b.horaFin}`).join(', ') : 'Día completo'}/><Detail label="Fechas excluidas" value={visibleException.fechasExcluidas?.length ? visibleException.fechasExcluidas.map(dateLabel).join(', ') : 'Ninguna'}/><Detail label="Creada" value={visibleException.fechaCreacion ? new Date(visibleException.fechaCreacion).toLocaleString('es-AR') : '—'}/><div className="flex flex-wrap gap-2 border-t pt-4"><Button onClick={()=>navigate(`/profesional/mi-mes?mes=${visibleException.fechaInicio.slice(0,7)}&excepcion=${visibleException.id}`)}>Ver en Mi mes</Button>{visibleException.activa&&<Button variant="destructive" onClick={()=>setCancelTarget(visibleException)}>Cancelar excepción</Button>}</div></div>}</SheetContent></Sheet>
    <ConfirmDialog open={Boolean(cancelTarget)} onOpenChange={open=>!open&&setCancelTarget(null)} title="Cancelar excepción" description="La excepción quedará cancelada. Los turnos dados de baja no se reactivarán automáticamente." confirmLabel="Confirmar cancelación" variant="destructive" onConfirm={doCancel}/>
  </div>;
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
