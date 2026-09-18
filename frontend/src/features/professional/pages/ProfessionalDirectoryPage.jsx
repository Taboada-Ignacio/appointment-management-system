import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { PageHeader } from '../components/PageHeader';
import { professionalContext } from '@/config/professional';
import { api } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Textarea } from '@/components/ui/textarea';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';

const clientStates = {
  PENDIENTE_DE_VERIFICACION: 'Pendiente de verificación',
  HABILITADO: 'Verificado (habilitado)',
  REQUIERE_APROBACION: 'Requiere aprobación',
  INHABILITADO: 'Inhabilitado',
  DADO_DE_BAJA: 'Dado de baja',
};

export function ProfessionalDirectoryPage({ clients = false, pendingClients = false }) {
  const queryClient = useQueryClient();
  const [editingClient, setEditingClient] = useState(null);
  const [nextState, setNextState] = useState('HABILITADO');
  const [selected, setSelected] = useState([]);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [bajaOpen, setBajaOpen] = useState(false);
  const [reason, setReason] = useState('');
  const actionsBase = `/api/profesionales/${professionalContext.id}/${pendingClients ? 'clientes' : 'turnos'}/pendientes-verificacion`;
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ estado: pendingClients ? 'PENDIENTE_DE_VERIFICACION' : 'todos', nombre: '', apellido: '', dni: '' });
  const title = clients ? pendingClients ? 'Clientes pendientes de verificación' : 'Mis clientes' : 'Turnos pendientes de verificación';
  const query = useQuery({
    queryKey: ['professional-directory', professionalContext.id, clients, page, filters],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (clients) {
        for (const [key, value] of Object.entries(filters)) {
          if (value.trim() && value !== 'todos') params.set(key, value.trim());
        }
      }
      return api.get(`/api/profesionales/${professionalContext.id}/${clients ? 'clientes' : 'turnos/pendientes-verificacion'}?${params}`);
    },
  });
  const data = query.data;
  const rows = data?.content || [];
  const toggle = id => { setSelected(current => current.includes(id) ? current.filter(value => value !== id) : [...current, id]); setNotice(''); };
  async function selectAll() {
    setBusy(true); setActionError(''); setNotice('');
    try { setSelected(await api.get(`${actionsBase}/ids`)); }
    catch (error) { setActionError(error.message); }
    finally { setBusy(false); }
  }
  async function applyAction(action) {
    if (busy || !selected.length || (action === 'baja' && !reason.trim())) return;
    setBusy(true); setActionError(''); setNotice('');
    try {
      await api.post(`${actionsBase}/${action}`, { turnoIds: selected, ...(action === 'baja' ? { motivo: reason.trim() } : {}) }, { headers: { 'X-Usuario': professionalContext.actor } });
      setSelected([]); setPage(0); setBajaOpen(false); setReason('');
      setNotice(action === 'baja' ? 'Los turnos seleccionados fueron dados de baja.' : 'Los turnos seleccionados fueron verificados y quedaron asignados.');
      await queryClient.invalidateQueries();
    } catch (error) { setActionError(error.message); }
    finally { setBusy(false); }
  }
  async function applyClientAction(estado) {
    if (busy || !selected.length) return;
    setBusy(true); setActionError(''); setNotice('');
    try {
      await api.post(`${actionsBase}/estado`, { clienteIds: selected, estado }, { headers: { 'X-Usuario': professionalContext.actor } });
      setSelected([]); setPage(0); setNotice('Los clientes seleccionados fueron actualizados.');
      await queryClient.invalidateQueries();
    } catch (error) { setActionError(error.message); }
    finally { setBusy(false); }
  }
  async function changeClientState(event) {
    event.preventDefault();
    if (busy || !editingClient) return;
    setBusy(true); setActionError('');
    try {
      await api.put(`/api/profesionales/${professionalContext.id}/clientes/${editingClient.id}/estado`, { estado: nextState }, { headers: { 'X-Usuario': professionalContext.actor } });
      setEditingClient(null); setPage(0); setNotice('El estado del cliente fue actualizado.');
      await queryClient.invalidateQueries();
    } catch (error) { setActionError(error.message); }
    finally { setBusy(false); }
  }
  const changeFilter = (name, value) => { setFilters(current => ({ ...current, [name]: value })); setPage(0); };
  const formatDate = value => new Intl.DateTimeFormat('es-AR', {
    timeZone: professionalContext.timezone, dateStyle: 'long', timeStyle: 'short',
  }).format(new Date(value));

  return <div className="flex flex-col gap-6">
    <PageHeader eyebrow={clients ? 'Cartera del profesional' : 'Solicitudes de turnos'} title={title}
      description={clients ? 'Consultá todos tus clientes y filtrá por su estado o datos.' : 'Turnos que requieren revisión y aprobación del profesional.'} />
    <Card>
      <CardHeader><CardTitle>{clients ? 'Clientes registrados' : 'Solicitudes pendientes'}</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-5">
        {pendingClients && <><div className="flex flex-wrap items-center gap-3"><Button variant="outline" disabled={busy || query.isPending || !data?.totalElements} onClick={selectAll}>Seleccionar todos los clientes</Button><Button variant="ghost" disabled={busy || !selected.length} onClick={() => setSelected([])}>Limpiar selección</Button><span className="text-sm">{selected.length} seleccionados</span>{[['HABILITADO', 'Verificar'], ['REQUIERE_APROBACION', 'Requiere aprobación manual'], ['INHABILITADO', 'Inhabilitar'], ['DADO_DE_BAJA', 'Dar de baja']].map(([estado, label]) => <Button key={estado} variant={estado === 'DADO_DE_BAJA' ? 'destructive' : 'outline'} disabled={busy || !selected.length} onClick={() => applyClientAction(estado)}>{label}</Button>)}</div>{busy && <p role="status">Procesando clientes…</p>}{actionError && <Alert variant="destructive" role="alert"><AlertTitle>No se pudo completar la acción</AlertTitle><AlertDescription>{actionError}</AlertDescription></Alert>}</>}
        {!clients && <>
          <div className="flex flex-wrap items-center gap-3">
            <Button variant="outline" disabled={busy || query.isPending || !data?.totalElements} onClick={selectAll}>Seleccionar todos los turnos</Button>
            <Button variant="ghost" disabled={busy || !selected.length} onClick={() => setSelected([])}>Limpiar selección</Button>
            <span className="text-sm">{selected.length} seleccionados</span>
            <Button disabled={busy || !selected.length} onClick={() => applyAction('verificar')}>{busy ? 'Procesando…' : 'Verificar turno'}</Button>
            <Button variant="destructive" disabled={busy || !selected.length} onClick={() => setBajaOpen(true)}>Dar de baja turno</Button>
          </div>
          {actionError && <Alert variant="destructive" role="alert"><AlertTitle>No se pudo completar la acción</AlertTitle><AlertDescription>{actionError}</AlertDescription></Alert>}
          {notice && <p role="status">{notice}</p>}
        </>}
        {clients && <>{notice && <p role="status">{notice}</p>}</>}
        {clients && <FieldGroup className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Field><FieldLabel htmlFor="clients-state">Estado del cliente</FieldLabel>
            <Select disabled={pendingClients} value={filters.estado} onValueChange={value => changeFilter('estado', value)}>
              <SelectTrigger id="clients-state"><SelectValue /></SelectTrigger>
              <SelectContent><SelectItem value="todos">Todos los estados</SelectItem>{Object.entries(clientStates).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent>
            </Select>
          </Field>
          {['nombre', 'apellido', 'dni'].map(name => <Field key={name}><FieldLabel htmlFor={`clients-${name}`}>{name === 'dni' ? 'DNI' : name === 'nombre' ? 'Nombre' : 'Apellido'}</FieldLabel><Input id={`clients-${name}`} value={filters[name]} onChange={event => changeFilter(name, event.target.value)} /></Field>)}
        </FieldGroup>}
        {query.isPending ? <p role="status">{clients ? 'Consultando clientes…' : 'Consultando turnos pendientes…'}</p> : query.isError ?
          <Alert variant="destructive"><AlertTitle>No se pudo consultar la lista</AlertTitle><AlertDescription>{query.error.message}<Button variant="outline" onClick={() => query.refetch()}>Volver a intentar</Button></AlertDescription></Alert> : <>
            <p className="text-sm text-muted-foreground">{data?.totalElements || 0} {clients ? 'clientes' : 'turnos pendientes'}</p>
            {rows.length ? <Table aria-label={title}>
              <TableHeader><TableRow>{pendingClients && <TableHead>Seleccionar</TableHead>}{!clients && <><TableHead>Seleccionar</TableHead><TableHead>Turno y horario</TableHead></>}<TableHead>Cliente</TableHead><TableHead>DNI</TableHead>{clients && <TableHead>Contacto</TableHead>}<TableHead>Estado</TableHead>{clients && !pendingClients && <TableHead>Acciones</TableHead>}</TableRow></TableHeader>
              <TableBody>{rows.map(row => {
                const client = clients ? row : row.cliente;
                return <TableRow key={row.id}>
                  {pendingClients && <TableCell><input type="checkbox" className="size-4 accent-primary" aria-label={`Seleccionar cliente ${client.nombre} ${client.apellido}`} checked={selected.includes(row.id)} disabled={busy} onChange={() => toggle(row.id)} /></TableCell>}
                  {!clients && <TableCell><input type="checkbox" className="size-4 accent-primary" aria-label={`Seleccionar turno #${row.id}`} checked={selected.includes(row.id)} disabled={busy} onChange={() => toggle(row.id)} /></TableCell>}
                  {!clients && <TableCell><p className="font-semibold">#{row.id}</p><p>{formatDate(row.inicioEstimado)}</p><p className="text-muted-foreground">Hasta {new Intl.DateTimeFormat('es-AR', { timeZone: professionalContext.timezone, hour: '2-digit', minute: '2-digit' }).format(new Date(row.finEstimado))}</p></TableCell>}
                  <TableCell>{client?.nombre} {client?.apellido}</TableCell>
                  <TableCell>{client?.numeroDocumento || '—'}</TableCell>
                  {clients && <TableCell><p>{row.email || '—'}</p><p>{row.telefono || '—'}</p></TableCell>}
                  <TableCell><Badge variant="outline">{clients ? clientStates[row.estadoActual] || row.estadoActual || 'Sin estado' : 'Pendiente de aprobación'}</Badge></TableCell>
                  {clients && !pendingClients && <TableCell><Button variant="outline" disabled={busy} aria-label={`Cambiar estado de ${client.nombre} ${client.apellido}`} onClick={() => { setEditingClient(client); setNextState(client.estadoActual === 'PENDIENTE_DE_VERIFICACION' ? 'HABILITADO' : client.estadoActual || 'HABILITADO'); setActionError(''); }}>Cambiar estado</Button></TableCell>}
                </TableRow>;
              })}</TableBody>
            </Table> : <p>{clients ? 'No hay clientes que coincidan con los filtros.' : 'No hay turnos pendientes de verificación.'}</p>}
            <div className="flex flex-wrap items-center justify-between gap-3"><Button variant="outline" disabled={page === 0 || query.isFetching} onClick={() => setPage(current => current - 1)}>Página anterior</Button><span className="text-sm">Página {page + 1} de {Math.max(1, data?.totalPages || 0)}</span><Button variant="outline" disabled={page + 1 >= (data?.totalPages || 0) || query.isFetching} onClick={() => setPage(current => current + 1)}>Página siguiente</Button></div>
          </>}
      </CardContent>
    </Card>
    {clients && !pendingClients && <ConfirmDialog open={Boolean(editingClient)} onOpenChange={open => { if (!busy && !open) setEditingClient(null); }} title="Cambiar estado del cliente" description={editingClient ? `${editingClient.nombre} ${editingClient.apellido}` : ''} confirmLabel="Guardar estado" loading={busy} confirmDisabled={!nextState || nextState === editingClient?.estadoActual} onConfirm={changeClientState}><Field><FieldLabel htmlFor="client-next-state">Nuevo estado</FieldLabel><Select value={nextState} onValueChange={setNextState} disabled={busy}><SelectTrigger id="client-next-state"><SelectValue /></SelectTrigger><SelectContent>{Object.entries(clientStates).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent></Select></Field>{actionError && <p role="alert">{actionError}</p>}</ConfirmDialog>}
    {!clients && <ConfirmDialog open={bajaOpen} onOpenChange={setBajaOpen} title="Dar de baja turnos" description={`Se darán de baja los ${selected.length} turnos seleccionados. Indicá el motivo.`} confirmLabel="Confirmar baja" variant="danger" loading={busy} confirmDisabled={!reason.trim()} onConfirm={event => { event.preventDefault(); applyAction('baja'); }}><Field><FieldLabel htmlFor="pending-baja-reason">Motivo de baja</FieldLabel><Textarea id="pending-baja-reason" value={reason} onChange={event => setReason(event.target.value)} maxLength={255} disabled={busy} /></Field>{actionError && <p role="alert">{actionError}</p>}</ConfirmDialog>}

  </div>;
}
