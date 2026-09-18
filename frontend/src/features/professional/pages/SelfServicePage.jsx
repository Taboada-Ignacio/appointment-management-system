import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { ArrowUpRight, Copy, Link2, LoaderCircle, QrCode, RefreshCw, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { selfServiceLinkApi, bookingUrl } from '@/features/selfService/api';
import { PageHeader } from '../components/PageHeader';

export function SelfServiceShortcut() {
  return <Card>
    <CardHeader><CardTitle className="flex items-center gap-2"><Link2 data-icon="inline-start" />Reservas desde tu enlace</CardTitle>
      <CardDescription>Compartí tu agenda para que tus clientes soliciten un turno.</CardDescription></CardHeader>
    <CardFooter><Button variant="outline" asChild><Link to="/profesional/autogestion">Administrar autogestión<ArrowUpRight data-icon="inline-end" /></Link></Button></CardFooter>
  </Card>;
}

export function SelfServicePage() {
  const [link, setLink] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [dialog, setDialog] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showQr, setShowQr] = useState(false);
  async function load() {
    setLoading(true); setError('');
    try { setLink(await selfServiceLinkApi.get()); }
    catch (e) { setError(e.status === 401 ? 'Iniciá sesión como profesional para administrar el enlace.' : e.message); }
    finally { setLoading(false); }
  }
  useEffect(() => {
    let active = true;
    selfServiceLinkApi.get().then(data => { if (active) setLink(data); })
      .catch(e => { if (active) setError(e.status === 401 ? 'Iniciá sesión como profesional para administrar el enlace.' : e.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  const url = link?.token ? bookingUrl(link.token) : '';
  async function change() {
    setBusy(true); setError(''); setNotice('');
    try {
      if (dialog === 'revocar') { await selfServiceLinkApi.revoke(); setLink({ activo: false }); setShowQr(false); setNotice('Enlace revocado. Ya no admite nuevas reservas.'); }
      else { const result = await selfServiceLinkApi.create(); setLink({ activo: true, recuperable: true, token: result.token }); setNotice('Enlace listo para compartir.'); }
      setDialog(null);
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  async function copy() {
    try { await navigator.clipboard.writeText(url); setNotice('Enlace copiado.'); }
    catch { setError('No se pudo copiar. Seleccioná el enlace y copialo manualmente.'); }
  }
  return <div className="flex flex-col gap-6">
    <PageHeader eyebrow="Reservas de clientes" title="Autogestión" description="Un enlace para reservar. Tu agenda y tus reglas." />
    {error && <Alert variant="destructive"><AlertTitle>No se pudo completar la acción</AlertTitle><AlertDescription>{error}</AlertDescription></Alert>}
    {notice && <p role="status" className="text-sm text-muted-foreground">{notice}</p>}
    <div className="grid gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <Card>
        <CardHeader><div className="flex items-center justify-between gap-3"><CardTitle>Tu enlace de reserva</CardTitle>{!loading && link && <Badge variant={link.activo ? 'default' : 'secondary'}>{link.activo ? 'Activo' : 'Sin enlace activo'}</Badge>}</div>
          <CardDescription>Disponible hasta que lo revoques o generes uno nuevo.</CardDescription></CardHeader>
        <CardContent className="flex flex-col gap-5">
          {loading ? <p aria-busy="true" className="flex items-center gap-2"><LoaderCircle className="animate-spin" />Consultando enlace…</p> : link ? <>
            {url ? <><div className="flex flex-col gap-2"><Label htmlFor="booking-link">Enlace para compartir</Label><Input id="booking-link" readOnly value={url} onFocus={e => e.target.select()} /></div>
              <div className="flex flex-wrap gap-2"><Button onClick={copy}><Copy data-icon="inline-start" />Copiar enlace</Button><Button variant="outline" asChild><a href={url} target="_blank" rel="noopener noreferrer">Abrir portal<ArrowUpRight data-icon="inline-end" /></a></Button><Button variant="outline" onClick={() => setShowQr(v => !v)} aria-expanded={showQr}><QrCode data-icon="inline-start" />{showQr ? 'Ocultar QR' : 'Mostrar QR'}</Button></div>
              {showQr && <figure className="flex flex-col items-center gap-3 rounded-xl border p-6"><div className="rounded-lg bg-white p-4"><QRCodeSVG value={url} size={192} title="Código QR del enlace de reserva" /></div><figcaption className="text-sm text-muted-foreground">Escaneá para abrir la reserva con este profesional.</figcaption></figure>}
            </> : <p className="text-muted-foreground">{link.activo ? 'Este enlace se generó antes de habilitar su recuperación. Generá uno nuevo para poder copiarlo desde el panel.' : 'Generá tu enlace y compartilo con tus clientes para que puedan solicitar un turno.'}</p>}
          </> : <Button variant="outline" onClick={load}>Volver a consultar</Button>}
        </CardContent>
        {link && !loading && <CardFooter className="flex flex-wrap gap-2"><Button variant={link.activo ? 'outline' : 'default'} disabled={busy} onClick={() => setDialog(link.activo ? 'regenerar' : 'crear')}><RefreshCw data-icon="inline-start" />{link.activo ? 'Generar un enlace nuevo' : 'Generar enlace'}</Button>{link.activo && <Button variant="destructive" disabled={busy} onClick={() => setDialog('revocar')}>Revocar enlace</Button>}</CardFooter>}
      </Card>
      <Card><CardHeader><CardTitle className="flex items-center gap-2"><ShieldCheck data-icon="inline-start" />Cómo funciona</CardTitle><CardDescription>Vos seguís administrando quién puede reservar.</CardDescription></CardHeader>
        <CardContent><ul className="flex flex-col gap-4 text-sm text-muted-foreground"><li>Los clientes se identifican con DNI y teléfono.</li><li>Los clientes habilitados reciben un turno asignado.</li><li>Si requieren aprobación, la solicitud queda pendiente.</li><li>Los clientes nuevos esperan tu verificación antes de reservar.</li></ul></CardContent>
      </Card>
    </div>
    <ConfirmDialog open={Boolean(dialog)} onOpenChange={open => !open && setDialog(null)} loading={busy} title={dialog === 'revocar' ? 'Revocar enlace' : dialog === 'crear' ? 'Generar enlace de reserva' : 'Generar un enlace nuevo'} description={dialog === 'crear' ? 'El enlace permitirá solicitar turnos según la disponibilidad y el estado de cada cliente.' : 'El enlace anterior y las sesiones abiertas desde él dejarán de funcionar. Si generás uno nuevo, tendrás que compartirlo nuevamente.'} confirmLabel={dialog === 'revocar' ? 'Revocar enlace' : 'Generar enlace'} variant={dialog === 'revocar' ? 'danger' : 'default'} onConfirm={change} />
  </div>;
}
