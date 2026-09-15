export function affectedAppointmentToAppointment(affected) {
  if (!affected) return null;
  return {
    id: affected.turnoId,
    turnoId: affected.turnoId,
    fecha: affected.fechaActual || affected.fechaOriginal,
    inicioEstimado: affected.inicioActual || affected.inicioOriginal,
    finEstimado: affected.finActual || affected.finOriginal,
    estado: affected.estadoTurno || affected.estadoTurnoAnterior || 'AFECTADO_POR_EXCEPCION',
    clienteId: affected.clienteId,
    cliente: {
      id: affected.clienteId,
      nombre: affected.nombreCliente || 'Cliente sin nombre informado',
      apellido: '',
      telefono: affected.telefono,
    },
  };
}
