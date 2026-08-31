package com.apiturnos.notificacion.service;

import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.notificacion.model.CanalNotificacion;
import com.apiturnos.notificacion.model.EstadoNotificacion;
import com.apiturnos.notificacion.model.Notificacion;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.turno.model.Turno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistradorNotificacionUnitTest {

    @Mock
    private NotificacionRepository repository;

    @Test
    void telefonoHabilitadoCreaIntencionWhatsappPendiente() {
        Cliente cliente = cliente(true);
        Turno turno = new Turno();
        turno.setId(10L);
        RegistradorNotificacion registrador = new RegistradorNotificacion(repository);

        registrador.registrarSiCorresponde(
                cliente, turno, TipoNotificacion.CONFIRMACION_TURNO, "Turno asignado");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCanal()).isEqualTo(CanalNotificacion.WHATSAPP);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(captor.getValue().getDestinatario()).isEqualTo("+5491112345678");
        assertThat(captor.getValue().getTurno()).isSameAs(turno);
    }

    @Test
    void telefonoNoHabilitadoNoCreaNotificacion() {
        Cliente cliente = cliente(false);
        RegistradorNotificacion registrador = new RegistradorNotificacion(repository);

        Notificacion resultado = registrador.registrarSiCorresponde(
                cliente, new Turno(), TipoNotificacion.CONFIRMACION_TURNO, "Turno asignado");

        assertThat(resultado).isNull();
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Cliente cliente(boolean habilitado) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setTelefono("+5491112345678");
        cliente.setNotificacionesHabilitadas(habilitado);
        return cliente;
    }
}
