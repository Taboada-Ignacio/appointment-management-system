package com.apiturnos.notificacion.service;

import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.notificacion.model.CanalNotificacion;
import com.apiturnos.notificacion.model.EstadoNotificacion;
import com.apiturnos.notificacion.model.Notificacion;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.turno.model.Turno;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RegistradorNotificacion {

    private final NotificacionRepository notificacionRepository;

    public RegistradorNotificacion(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public Notificacion registrarSiCorresponde(Cliente cliente, Turno turno,
                                                TipoNotificacion tipo, String mensajeTexto) {
        if (!Boolean.TRUE.equals(cliente.getNotificacionesHabilitadas())) {
            return null;
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setCliente(cliente);
        notificacion.setTurno(turno);
        notificacion.setTipo(tipo);
        notificacion.setCanal(CanalNotificacion.WHATSAPP);
        notificacion.setDestinatario(cliente.getTelefono());
        notificacion.setMensaje(mensajeTexto);
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        notificacion.setFechaProgramada(Instant.now());
        return notificacionRepository.save(notificacion);
    }
}
