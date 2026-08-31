package com.apiturnos;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.auditoria.model.AuditoriaEvento;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.model.Estado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.repository.EstadoRepository;
import com.apiturnos.notificacion.model.CanalNotificacion;
import com.apiturnos.notificacion.model.EstadoNotificacion;
import com.apiturnos.notificacion.model.Notificacion;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.model.TurnoHistorial;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
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
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AgendaAnualRepository agendaAnualRepository;

    @Autowired
    private MesAgendaRepository mesAgendaRepository;

    @Autowired
    private DiaAgendaRepository diaAgendaRepository;

    @Autowired
    private BrechaHorariaRepository brechaHorariaRepository;

    @Autowired
    private ExcepcionAgendaRepository excepcionAgendaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private TurnoHistorialRepository turnoHistorialRepository;

    @Autowired
    private MotivoBajaTurnoRepository motivoBajaTurnoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private CambioEstadoRepository cambioEstadoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Test
    @DisplayName("Valida que Flyway y Hibernate inicialicen y validen correctamente todas las entidades y tablas del nuevo modelo")
    void shouldPersistAndRetrieveEntitiesSuccessfully() {
        // 1. Crear Profesional
        Profesional profesional = new Profesional();
        profesional.setNombre("Carlos");
        profesional.setApellido("Gomez");
        profesional.setEmail("carlos.gomez@turnos.com");
        profesional.setTelefono("+5491199887766");
        profesional.setEspecialidad("Odontología");
        profesional = profesionalRepository.save(profesional);
        assertThat(profesional.getId()).isNotNull();

        // 2. Crear Configuración
        Configuracion configuracion = new Configuracion();
        configuracion.setProfesional(profesional);
        configuracion.setCantidadMaxTurnosALaVez(2);
        configuracion.setDuracionAproximadaPorTurno(30);
        configuracion.setAgendaSoloManejadaPorProfesional(false);
        configuracion = configuracionRepository.save(configuracion);
        assertThat(configuracion.getId()).isNotNull();

        // 3. Crear Cliente asociado a Profesional
        Cliente cliente = new Cliente();
        cliente.setProfesional(profesional);
        cliente.setNombre("Juan");
        cliente.setApellido("Perez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("12345678");
        cliente.setEmail("juan.perez@example.com");
        cliente.setTelefono("+5491112345678");
        cliente.setNotificacionesHabilitadas(true);
        cliente = clienteRepository.save(cliente);
        assertThat(cliente.getId()).isNotNull();

        // 4. Registrar CambioEstado inicial del Cliente (HABILITADO)
        Estado estadoHabilitado = estadoRepository.findByNombreAndAmbito("HABILITADO", AmbitoEstado.CLIENTE).orElseThrow();
        CambioEstado cambioEstadoCliente = new CambioEstado();
        cambioEstadoCliente.setEstado(estadoHabilitado);
        cambioEstadoCliente.setAmbito(AmbitoEstado.CLIENTE);
        cambioEstadoCliente.setEntidadId(cliente.getId());
        cambioEstadoCliente.setFechaHoraInicio(Instant.now());
        cambioEstadoCliente.setUsuario("admin");
        cambioEstadoCliente = cambioEstadoRepository.save(cambioEstadoCliente);
        assertThat(cambioEstadoCliente.getId()).isNotNull();

        // 5. Crear Jerarquía de Agenda: AgendaAnual -> MesAgenda -> DiaAgenda -> BrechaHoraria
        AgendaAnual agendaAnual = new AgendaAnual();
        agendaAnual.setProfesional(profesional);
        agendaAnual.setAnio(2026);
        agendaAnual = agendaAnualRepository.save(agendaAnual);
        assertThat(agendaAnual.getId()).isNotNull();

        MesAgenda mesAgenda = new MesAgenda();
        mesAgenda.setAgendaAnual(agendaAnual);
        mesAgenda.setNroMes(8);
        mesAgenda.setRepetirConfiguracion(false);
        mesAgenda = mesAgendaRepository.save(mesAgenda);
        assertThat(mesAgenda.getId()).isNotNull();

        DiaAgenda diaAgenda = new DiaAgenda();
        diaAgenda.setMesAgenda(mesAgenda);
        diaAgenda.setFecha(LocalDate.of(2026, 8, 30));
        diaAgenda = diaAgendaRepository.save(diaAgenda);
        assertThat(diaAgenda.getId()).isNotNull();

        BrechaHoraria brecha = new BrechaHoraria();
        brecha.setDiaAgenda(diaAgenda);
        brecha.setHoraInicioAtencion(LocalTime.of(8, 0));
        brecha.setHoraFinAtencion(LocalTime.of(12, 0));
        brecha = brechaHorariaRepository.save(brecha);
        assertThat(brecha.getId()).isNotNull();

        // 6. Crear Excepción de Agenda
        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setProfesional(profesional);
        excepcion.setTipo(TipoExcepcion.VACACIONES);
        excepcion.setFechaInicio(LocalDate.of(2026, 9, 1));
        excepcion.setFechaFin(LocalDate.of(2026, 9, 15));
        excepcion.setMotivo("Vacaciones de primavera");
        excepcion = excepcionAgendaRepository.save(excepcion);
        assertThat(excepcion.getId()).isNotNull();

        // 7. Crear Turno
        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant fin = inicio.plus(30, ChronoUnit.MINUTES);
        Turno turno = new Turno();
        turno.setDiaAgenda(diaAgenda);
        turno.setCliente(cliente);
        turno.setInicioEstimado(inicio);
        turno.setFinEstimado(fin);
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno = turnoRepository.save(turno);
        assertThat(turno.getId()).isNotNull();

        // 8. Registrar Estado del Turno (ASIGNADO)
        Estado estadoAsignado = estadoRepository.findByNombreAndAmbito("ASIGNADO", AmbitoEstado.TURNO).orElseThrow();
        CambioEstado cambioEstadoTurno = new CambioEstado();
        cambioEstadoTurno.setEstado(estadoAsignado);
        cambioEstadoTurno.setAmbito(AmbitoEstado.TURNO);
        cambioEstadoTurno.setEntidadId(turno.getId());
        cambioEstadoTurno.setFechaHoraInicio(Instant.now());
        cambioEstadoTurno.setUsuario("admin");
        cambioEstadoTurno = cambioEstadoRepository.save(cambioEstadoTurno);
        assertThat(cambioEstadoTurno.getId()).isNotNull();

        // 9. Crear Notificación
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

        // 10. Crear Registro de Auditoría
        AuditoriaEvento auditoria = new AuditoriaEvento();
        auditoria.setModulo("TURNO");
        auditoria.setEntidad("Turno");
        auditoria.setEntidadId(turno.getId().toString());
        auditoria.setOperacion(OperacionAuditoria.CREATE);
        auditoria.setUsuario("carlos.gomez@turnos.com");
        auditoria.setProfesionalId(profesional.getId());
        auditoria.setDetalles("Turno asignado a cliente");
        auditoria = auditoriaEventoRepository.save(auditoria);
        assertThat(auditoria.getId()).isNotNull();
    }
}
