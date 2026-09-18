import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BookingPage } from '@/features/selfService/BookingPage';
import { selfServiceApi } from '@/features/selfService/api';
import { BOOKING_STORAGE_KEY, dateKey, readBooking, saveBooking } from '@/features/selfService/session';

vi.mock('@/features/selfService/api', () => ({ selfServiceApi: Object.fromEntries(['open', 'resume', 'identify', 'register', 'contact', 'appointments', 'config', 'months', 'days', 'dates', 'gaps', 'choose', 'confirm'].map(key => [key, vi.fn()])) }));
vi.mock('@/features/professional/components/MonthCalendar', () => ({ MonthCalendar: ({ onSelectDay, days }) => {
  const day = days.find(d => d.seleccionable);
  return <button disabled={!day} onClick={() => onSelectDay(day)}>Fecha disponible</button>;
} }));

const link = 'L'.repeat(43), session = 'S'.repeat(43), reference = 'R'.repeat(43);
const professional = { nombre: 'María', apellido: 'Pérez', especialidad: 'Odontología', telefono: '1122334455', zonaHoraria: 'America/Argentina/Buenos_Aires' };
const enabled = { nombre: 'Ana', apellido: 'López', email: 'ana@test.com', telefono: '1199887766', estado: 'HABILITADO', puedeReservar: true };
const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
const date = dateKey(tomorrow);
const failure = (status, message = 'Error de conexión') => Object.assign(new Error(message), { status });

beforeEach(() => {
  vi.resetAllMocks(); sessionStorage.clear(); window.history.replaceState({}, '', `/reservar#${link}`);
  selfServiceApi.open.mockResolvedValue({ token: session, vence: '2099-01-01T00:00:00Z' });
  selfServiceApi.resume.mockResolvedValue({ profesional: professional, cliente: null, dni: null, resultado: null });
  selfServiceApi.identify.mockResolvedValue(enabled); selfServiceApi.contact.mockResolvedValue(enabled);
  selfServiceApi.appointments.mockResolvedValue([]);
  selfServiceApi.config.mockResolvedValue({ duracionMinutos: 30, capacidadSimultanea: 1 });
  selfServiceApi.months.mockResolvedValue([date.slice(0, 7)]);
  selfServiceApi.days.mockImplementation((_session, month) => Promise.resolve(date.startsWith(month) ? [{ id: 1, fecha: date, estadoActual: 'ACTIVO', seleccionable: true }] : []));
  selfServiceApi.gaps.mockResolvedValue([{ inicio: '08:00:00', fin: '10:00:00', intervalos: [{ inicio: '08:00:00', fin: '08:30:00' }] }]);
  selfServiceApi.choose.mockResolvedValue({ token: reference, vence: '2099-01-01T00:00:00Z' });
  selfServiceApi.confirm.mockResolvedValue({ turnoId: 42, estado: 'ASIGNADO', inicio: `${date}T11:00:00Z`, fin: `${date}T11:30:00Z` });
});

function renderBooking() { return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })}><BookingPage /></QueryClientProvider>); }
async function click(element) { await act(async () => { fireEvent.click(element); }); }
async function identify() {
  await screen.findByLabelText('DNI');
  fireEvent.change(screen.getByLabelText('DNI'), { target: { value: '30111222' } });
  await click(screen.getByRole('button', { name: /Ingresar con mi DNI/ }));
}
async function review() {
  await identify();
  await click(await screen.findByRole('button', { name: /Registrar un nuevo turno/ }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Fecha disponible' })).toBeEnabled());
  await click(screen.getByRole('button', { name: 'Fecha disponible' }));
  await click(await screen.findByRole('button', { name: /Seleccionar.*08:00.*08:30/ }));
  await click(screen.getByRole('button', { name: /Revisar solicitud/ }));
  await screen.findByRole('heading', { name: 'Revisá tu solicitud' });
}

describe('Reserva pública', () => {
  it('edita datos desde el panel y vuelve al inicio sin iniciar una reserva', async () => {
    selfServiceApi.contact.mockResolvedValue({ ...enabled, nombre: 'Lucía', apellido: 'Gómez', email: 'lucia@test.com', telefono: '1123456789' });
    renderBooking(); await identify();
    await click(screen.getByRole('button', { name: 'Editar mis datos' }));
    expect(screen.getByRole('heading', { name: 'Editar mis datos' })).toBeInTheDocument();
    expect(screen.queryByLabelText('DNI')).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'LUCÍA' } });
    fireEvent.change(screen.getByLabelText('Apellido'), { target: { value: 'gÓMEZ' } });
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'lucia@test.com' } });
    fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '1123456789' } });
    await click(screen.getByRole('button', { name: 'Guardar mis datos' }));
    expect(selfServiceApi.contact).toHaveBeenCalledWith(session, { nombre: 'Lucía', apellido: 'Gómez', email: 'lucia@test.com', telefono: '1123456789' });
    expect(screen.getByRole('heading', { name: 'Te damos la bienvenida' })).toBeInTheDocument();
    expect(selfServiceApi.choose).not.toHaveBeenCalled();
  });

  it('confirma sin randomUUID y libera la carga al rechazar un segundo turno diario', async () => {
    const original = Object.getOwnPropertyDescriptor(crypto, 'randomUUID');
    Object.defineProperty(crypto, 'randomUUID', { configurable: true, value: undefined });
    try {
      const message = 'El cliente ya tiene un turno en este día. El profesional no permite más de un turno por cliente en el mismo día.';
      selfServiceApi.confirm.mockRejectedValue(Object.assign(new Error(message), { status: 400 }));
      renderBooking(); await review();
      await click(screen.getByRole('button', { name: 'Confirmar turno' }));
      expect(await screen.findByText(message)).toBeInTheDocument();
      expect(screen.getByRole('alert')).toHaveFocus();
      expect(screen.getByRole('button', { name: 'Confirmar turno' })).toBeEnabled();
      expect(screen.getByRole('button', { name: 'Volver' })).toBeEnabled();
      expect(readBooking().draft.pending).toBeNull();
      expect(selfServiceApi.confirm.mock.calls[0][1]).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    } finally {
      if (original) Object.defineProperty(crypto, 'randomUUID', original);
      else delete crypto.randomUUID;
    }
  });
  it('completa el flujo y confirma con referencia e idempotencia, sin IDs del cliente o profesional', async () => {
    renderBooking(); await review();
    expect(selfServiceApi.choose).toHaveBeenCalledWith(session, { fecha: date, horaInicio: '08:00:00' });
    expect(screen.queryByLabelText('Tipo de atención')).not.toBeInTheDocument();
    await click(screen.getByRole('button', { name: 'Confirmar turno' }));
    await screen.findByRole('heading', { name: 'Tu turno está asignado' });
    expect(selfServiceApi.confirm).toHaveBeenCalledWith(session, expect.any(String), { referencia: reference, observaciones: '' });
    expect(selfServiceApi.confirm.mock.calls[0][2]).not.toHaveProperty('clienteId');
    expect(screen.getByText('#42')).toBeInTheDocument();
    expect(screen.getByText('María Pérez')).toBeInTheDocument();
  });
  it('valida los campos y no envía una identificación incompleta', async () => {
    renderBooking(); await screen.findByLabelText('DNI');
    await click(screen.getByRole('button', { name: /Ingresar con mi DNI/ }));
    expect(screen.getByLabelText('DNI')).toHaveAttribute('aria-invalid', 'true');
    expect(selfServiceApi.identify).not.toHaveBeenCalled();
  });
  it('ingresa solamente con DNI y muestra los turnos del cliente', async () => {
    selfServiceApi.appointments.mockResolvedValue([{ turnoId: 10, estado: 'PENDIENTE_DE_APROBACION', inicio: `${date}T11:00:00Z`, tipoAtencion: 'Consulta' }]);
    renderBooking(); await identify();
    await screen.findByRole('heading', { name: 'Te damos la bienvenida' });
    expect(selfServiceApi.appointments).not.toHaveBeenCalled();
    await click(screen.getByRole('button', { name: 'Consultar mis turnos' }));
    await screen.findByText('Consulta · #10');
    expect(screen.getByRole('heading', { name: 'Mis turnos' })).toBeInTheDocument();
    expect(selfServiceApi.identify).toHaveBeenCalledWith(session, { dni: '30111222' });
    expect(selfServiceApi.contact).not.toHaveBeenCalled();
  });
  it('registra un cliente pendiente y le permite solicitar un turno pendiente de aprobación', async () => {
    selfServiceApi.identify.mockResolvedValue({ estado: 'NUEVO', puedeReservar: false });
    selfServiceApi.register.mockResolvedValue({ ...enabled, estado: 'PENDIENTE_DE_VERIFICACION', puedeReservar: true });
    selfServiceApi.confirm.mockResolvedValue({ turnoId: 43, estado: 'PENDIENTE_DE_APROBACION', inicio: `${date}T11:00:00Z` });
    renderBooking(); await identify();
    await click(await screen.findByRole('button', { name: 'Registrarse como cliente de María Pérez' }));
    fireEvent.change(await screen.findByLabelText('Nombre'), { target: { value: '  aNA  ' } });
    fireEvent.change(screen.getByLabelText('Apellido'), { target: { value: 'LÓPEZ' } });
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'ana@test.com' } });
    fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '1199887766' } });
    await click(screen.getByRole('button', { name: 'Registrarme y elegir fecha' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Fecha disponible' })).toBeEnabled());
    await click(screen.getByRole('button', { name: 'Fecha disponible' }));
    await click(await screen.findByRole('button', { name: /Seleccionar.*08:00.*08:30/ }));
    await click(screen.getByRole('button', { name: /Revisar solicitud/ }));
    await click(await screen.findByRole('button', { name: 'Solicitar turno' }));
    await screen.findByRole('heading', { name: 'Tu solicitud espera aprobación' });
    expect(selfServiceApi.register).toHaveBeenCalledWith(session, { nombre: 'Ana', apellido: 'López', email: 'ana@test.com', telefono: '1199887766' });
  });
  it('distingue la solicitud pendiente de aprobación de un turno asignado', async () => {
    const pending = { ...enabled, estado: 'REQUIERE_APROBACION' };
    selfServiceApi.identify.mockResolvedValue(pending); selfServiceApi.contact.mockResolvedValue(pending);
    selfServiceApi.confirm.mockResolvedValue({ turnoId: 43, estado: 'PENDIENTE_DE_APROBACION', inicio: `${date}T11:00:00Z` });
    renderBooking(); await review();
    await click(screen.getByRole('button', { name: 'Solicitar turno' }));
    await screen.findByRole('heading', { name: 'Tu solicitud espera aprobación' });
    expect(screen.queryByText('Tu turno está asignado')).not.toBeInTheDocument();
  });
  it('bloquea clientes inhabilitados y ofrece contacto', async () => {
    selfServiceApi.identify.mockResolvedValue({ ...enabled, estado: 'INHABILITADO', puedeReservar: false });
    renderBooking(); await identify();
    await screen.findByRole('heading', { name: 'Te damos la bienvenida' });
    expect(screen.getByRole('link', { name: /Contactar al profesional/ })).toHaveAttribute('href', 'tel:1122334455');
    expect(selfServiceApi.config).not.toHaveBeenCalled();
  });
  it('recupera sesión y paso desde el backend al recargar', async () => {
    saveBooking({ session, link, step: 'confirmacion', draft: { type: '7', date, hour: '08:00:00', reference } });
    selfServiceApi.resume.mockResolvedValue({ profesional: professional, cliente: enabled, dni: '30111222' });
    renderBooking(); await screen.findByRole('heading', { name: 'Revisá tu solicitud' });
    expect(selfServiceApi.open).not.toHaveBeenCalled(); expect(selfServiceApi.resume).toHaveBeenCalledWith(session);
  });
  it('recupera una confirmación que ya se creó antes de la recarga', async () => {
    saveBooking({ session, link, step: 'confirmacion', draft: { pending: { key: 'original' } } });
    selfServiceApi.resume.mockResolvedValue({ profesional: professional, cliente: enabled, dni: '30111222', claveResultado: 'original', resultado: { turnoId: 42, estado: 'ASIGNADO', inicio: `${date}T11:00:00Z` } });
    renderBooking(); await screen.findByRole('heading', { name: 'Tu turno está asignado' });
    expect(selfServiceApi.confirm).not.toHaveBeenCalled(); expect(readBooking().draft).toEqual({});
  });
  it('no reemplaza una nueva reserva en revisión por un turno creado anteriormente', async () => {
    saveBooking({ session, link, step: 'confirmacion', draft: { type: '7', date, hour: '08:00:00', reference } });
    selfServiceApi.resume.mockResolvedValue({ profesional: professional, cliente: enabled, dni: '30111222', claveResultado: 'previous', resultado: { turnoId: 40, estado: 'ASIGNADO', inicio: `${date}T11:00:00Z` } });
    renderBooking(); await screen.findByRole('heading', { name: 'Revisá tu solicitud' });
    expect(screen.queryByText('#40')).not.toBeInTheDocument();
    expect(readBooking().draft.reference).toBe(reference);
  });
  it('al perder el cupo vuelve a horarios sin repetir la identificación', async () => {
    selfServiceApi.confirm.mockRejectedValue(failure(409, 'Horario ocupado'));
    renderBooking(); await review(); await click(screen.getByRole('button', { name: 'Confirmar turno' }));
    await screen.findByRole('heading', { name: 'Encontrá tu horario' });
    expect(selfServiceApi.identify).toHaveBeenCalledTimes(1); expect(readBooking().draft.reference).toBeNull();
  });
  it('reintenta con la misma clave y cuerpo luego de perder la conexión', async () => {
    selfServiceApi.confirm.mockRejectedValueOnce(failure(0));
    renderBooking(); await review();
    fireEvent.change(screen.getByLabelText('Observaciones (opcional)'), { target: { value: 'Primera consulta' } });
    await click(screen.getByRole('button', { name: 'Confirmar turno' }));
    await click(await screen.findByRole('button', { name: 'Reintentar confirmación' }));
    await screen.findByRole('heading', { name: 'Tu turno está asignado' });
    expect(selfServiceApi.confirm.mock.calls[1]).toEqual(selfServiceApi.confirm.mock.calls[0]);
  });
  it('un doble clic crea una sola solicitud', async () => {
    let resolve; selfServiceApi.confirm.mockImplementation(() => new Promise(done => { resolve = done; }));
    renderBooking(); await review(); const button = screen.getByRole('button', { name: 'Confirmar turno' });
    await click(button); await click(button); expect(selfServiceApi.confirm).toHaveBeenCalledTimes(1);
    await act(async () => { resolve({ turnoId: 42, estado: 'ASIGNADO', inicio: `${date}T11:00:00Z` }); });
    await screen.findByRole('heading', { name: 'Tu turno está asignado' });
  });
  it('no reutiliza la sesión de otro enlace al cambiar de profesional', async () => {
    saveBooking({ session: 'old', link: 'X'.repeat(43), draft: { type: '99' } });
    renderBooking(); await screen.findByLabelText('DNI');
    expect(selfServiceApi.open).toHaveBeenCalledWith(link); expect(readBooking().draft).toEqual({});
  });
  it('una sesión vencida permite volver a iniciar y un enlace revocado no permite reservar', async () => {
    saveBooking({ session, link, draft: {} }); selfServiceApi.resume.mockRejectedValue(failure(401));
    renderBooking(); await click(await screen.findByRole('button', { name: 'Volver a iniciar' }));
    await waitFor(() => expect(selfServiceApi.open).toHaveBeenCalledWith(link));
  });
  it('muestra el límite de intentos con una indicación para continuar', async () => {
    selfServiceApi.identify.mockRejectedValue(failure(429)); renderBooking(); await identify();
    await screen.findByText(/Esperá un minuto/);
  });
  it('funciona sin guardar datos en almacenamiento permanente', async () => {
    renderBooking(); await identify(); await screen.findByRole('heading', { name: 'Te damos la bienvenida' });
    expect(localStorage.getItem(BOOKING_STORAGE_KEY)).toBeNull(); expect(readBooking()).not.toHaveProperty('cliente');
  });
  it('un enlace revocado no ofrece una reserva ni permite reutilizarlo como sesión', async () => {
    selfServiceApi.open.mockRejectedValue(failure(401)); renderBooking();
    await screen.findByText(/Este enlace ya no está disponible/);
    expect(selfServiceApi.resume).not.toHaveBeenCalled(); expect(screen.queryByLabelText('DNI')).not.toBeInTheDocument();
  });
  it('al fallar la conexión durante la recarga recupera la misma sesión sin descartar la confirmación pendiente', async () => {
    saveBooking({ session, link, step: 'confirmacion', draft: { pending: { key: 'original' } } });
    selfServiceApi.resume.mockRejectedValueOnce(failure(0)).mockResolvedValue({ profesional: professional, cliente: enabled, dni: '30111222', claveResultado: 'original', resultado: { turnoId: 42, estado: 'ASIGNADO', inicio: `${date}T11:00:00Z` } });
    renderBooking(); await click(await screen.findByRole('button', { name: 'Volver a consultar' }));
    await screen.findByRole('heading', { name: 'Tu turno está asignado' });
    expect(selfServiceApi.open).not.toHaveBeenCalled(); expect(selfServiceApi.resume).toHaveBeenNthCalledWith(2, session);
  });
});
