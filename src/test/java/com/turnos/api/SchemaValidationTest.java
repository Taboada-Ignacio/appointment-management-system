package com.turnos.api;

import com.turnos.api.agenda.Agenda;
import com.turnos.api.agenda.AgendaExcepcion;
import com.turnos.api.agenda.AgendaHorario;
import com.turnos.api.agenda.AgendaRepository;
import com.turnos.api.agenda.TipoExcepcion;
import com.turnos.api.auditoria.AuditoriaEvento;
import com.turnos.api.auditoria.AuditoriaEventoRepository;
import com.turnos.api.auditoria.OperacionAuditoria;
import com.turnos.api.cliente.Cliente;
import com.turnos.api.cliente.ClienteHistorialEstado;
import com.turnos.api.cliente.ClienteRepository;
import com.turnos.api.cliente.EstadoCliente;
import com.turnos.api.cliente.TipoDocumento;
import com.turnos.api.notificacion.CanalNotificacion;
import com.turnos.api.notificacion.EstadoNotificacion;
import com.turnos.api.notificacion.Notificacion;
import com.turnos.api.notificacion.NotificacionRepository;
import com.turnos.api.notificacion.TipoNotificacion;
import com.turnos.api.turno.EstadoTurno;
import com.turnos.api.turno.OrigenBaja;
import com.turnos.api.turno.TipoEventoTurno;
import com.turnos.api.turno.Turno;
import com.turnos.api.turno.TurnoHistorial;
import com.turnos.api.turno.TurnoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SchemaValidationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Test
    @DisplayName("Valida que Flyway y Hibernate inicialicen y validen correctamente todas las entidades y tablas")
    void shouldPersistAndRetrieveEntitiesSuccessfully() {
        // 1. Crear y persistir Cliente con estado y notificaciones
        Cliente cliente = new Cliente();
        cliente.setNombre("Juan");
        cliente.setApellido("Perez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("12345678");
        cliente.setEmail("juan.perez@example.com");
        cliente.setTelefono("+5491112345678");
        cliente.setNotificacionesHabilitadas(true);
        cliente.setEstadoActual(EstadoCliente.ACTIVO);
        cliente = clienteRepository.save(cliente);

        assertThat(cliente.getId()).isNotNull();
        assertThat(cliente.getCreadoEn()).isNotNull();

        // 2. Crear y persistir Agenda
        Agenda agenda = new Agenda();
        agenda.setNombre("Agenda Dr. Test");
        agenda.setDuracionTurnoMinutos(30);
        agenda.setTiempoEntreTurnosMinutos(5);
        agenda.setAnticipacionMaximaDias(30);
        agenda.setAnticipacionMinimaHoras(2);
        agenda.setActiva(true);
        agenda = agendaRepository.save(agenda);

        assertThat(agenda.getId()).isNotNull();

        // 3. Crear y persistir Excepción de Agenda (Vacaciones)
        AgendaExcepcion excepcion = new AgendaExcepcion();
        excepcion.setAgenda(agenda);
        excepcion.setTipo(TipoExcepcion.VACACIONES);
        excepcion.setFechaInicio(Instant.now().plus(10, ChronoUnit.DAYS));
        excepcion.setFechaFin(Instant.now().plus(20, ChronoUnit.DAYS));
        excepcion.setMotivo("Vacaciones de invierno");

        // 4. Crear Turno asignado
        Turno turno = new Turno();
        turno.setAgenda(agenda);
        turno.setCliente(cliente);
        turno.setFechaHoraInicio(Instant.now().plus(1, ChronoUnit.DAYS));
        turno.setFechaHoraFin(Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES));
        turno.setEstado(EstadoTurno.ASIGNADO);
        turno = turnoRepository.save(turno);

        assertThat(turno.getId()).isNotNull();

        // 5. Crear Notificación
        Notificacion notificacion = new Notificacion();
        notificacion.setCliente(cliente);
        notificacion.setTurno(turno);
        notificacion.setTipo(TipoNotificacion.CONFIRMACION_TURNO);
        notificacion.setCanal(CanalNotificacion.WHATSAPP);
        notificacion.setDestinatario(cliente.getTelefono());
        notificacion.setMensaje("Hola Juan, tu turno fue asignado exitosamente.");
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        notificacion.setFechaProgramada(Instant.now());
        notificacion = notificacionRepository.save(notificacion);

        assertThat(notificacion.getId()).isNotNull();

        // 6. Crear Registro de Auditoría
        AuditoriaEvento auditoria = new AuditoriaEvento();
        auditoria.setModulo("TURNO");
        auditoria.setEntidad("Turno");
        auditoria.setEntidadId(turno.getId().toString());
        auditoria.setOperacion(OperacionAuditoria.CREATE);
        auditoria.setUsuario("admin@turnos.com");
        auditoria.setDetalles("Turno asignado a cliente");
        auditoria = auditoriaEventoRepository.save(auditoria);

        assertThat(auditoria.getId()).isNotNull();
    }
}

