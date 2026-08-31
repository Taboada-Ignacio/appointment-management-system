package com.apiturnos.agenda;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.AplicarExcepcionAgenda;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "turnos.zona-horaria=UTC")
@Testcontainers
@Import(ExcepcionAgendaRollbackIntegrationTest.ServicioPruebaRollback.class)
class ExcepcionAgendaRollbackIntegrationTest {

    private static final int ANIO = 2045;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ProfesionalRepository profesionalRepository;

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
    private ClienteRepository clienteRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private CambioEstadoRepository cambioEstadoRepository;

    @Autowired
    private MotivoBajaTurnoRepository motivoBajaTurnoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Autowired
    private GestorCambioEstado gestorCambioEstado;

    @Autowired
    private ServicioPruebaRollback servicioPruebaRollback;

    private Profesional profesional;
    private DiaAgenda diaAgenda;
    private Turno turno;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setNombre("Profesional");
        profesional.setApellido("Rollback");
        profesional.setEmail("rollback." + System.nanoTime() + "@test.com");
        profesional.setTelefono("+5491100001111");
        profesional.setEspecialidad("Odontologia");
        profesional = profesionalRepository.save(profesional);

        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesional);
        agenda.setAnio(ANIO);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(10);
        mes.setRepetirConfiguracion(false);
        mes = mesAgendaRepository.save(mes);

        LocalDate fecha = LocalDate.of(ANIO, 10, 20);
        diaAgenda = new DiaAgenda();
        diaAgenda.setMesAgenda(mes);
        diaAgenda.setFecha(fecha);
        diaAgenda = diaAgendaRepository.save(diaAgenda);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA, diaAgenda.getId(), "ACTIVO", "test-setup", "Dia inicial");

        BrechaHoraria brecha = new BrechaHoraria();
        brecha.setDiaAgenda(diaAgenda);
        brecha.setHoraInicioAtencion(LocalTime.of(8, 0));
        brecha.setHoraFinAtencion(LocalTime.of(12, 0));
        brechaHorariaRepository.save(brecha);

        Cliente cliente = new Cliente();
        cliente.setProfesional(profesional);
        cliente.setNombre("Cliente");
        cliente.setApellido("Rollback");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("44999999");
        cliente.setEmail("cliente.rollback." + System.nanoTime() + "@test.com");
        cliente.setTelefono("+5491199998888");
        cliente.setNotificacionesHabilitadas(true);
        cliente = clienteRepository.save(cliente);

        turno = new Turno();
        turno.setDiaAgenda(diaAgenda);
        turno.setCliente(cliente);
        turno.setInicioEstimado(fecha.atTime(LocalTime.of(9, 0)).toInstant(ZoneOffset.UTC));
        turno.setFinEstimado(fecha.atTime(LocalTime.of(9, 30)).toInstant(ZoneOffset.UTC));
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno = turnoRepository.save(turno);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.TURNO, turno.getId(), "ASIGNADO", "test-setup", "Turno inicial");
    }

    @Test
    @DisplayName("Una falla transaccional durante la aplicación revierte completamente Excepcion, CambioEstado, MotivoBaja, Notificacion y Auditoria")
    void falloTransaccionalRevierteCompletamenteTodasLasEntidades() {
        LocalDate fecha = diaAgenda.getFecha();

        long conteoExcepcionesAntes = excepcionAgendaRepository.count();
        long conteoCambiosEstadoAntes = cambioEstadoRepository.count();
        long conteoMotivosBajaAntes = motivoBajaTurnoRepository.count();
        long conteoNotificacionesAntes = notificacionRepository.count();
        long conteoAuditoriaAntes = auditoriaEventoRepository.count();

        assertThatThrownBy(() -> servicioPruebaRollback.ejecutarConFallo(
                profesional.getId(),
                fecha,
                fecha,
                TipoExcepcion.BLOQUEO_HORARIO,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Bloqueo con falla provocada",
                "test-rollback-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fallo provocado intencionalmente para verificar rollback");

        // Verificación desde una nueva transacción / lectura directa: todo debe haber vuelto al estado previo
        assertThat(excepcionAgendaRepository.count()).isEqualTo(conteoExcepcionesAntes);
        assertThat(cambioEstadoRepository.count()).isEqualTo(conteoCambiosEstadoAntes);
        assertThat(motivoBajaTurnoRepository.count()).isEqualTo(conteoMotivosBajaAntes);
        assertThat(notificacionRepository.count()).isEqualTo(conteoNotificacionesAntes);
        assertThat(auditoriaEventoRepository.count()).isEqualTo(conteoAuditoriaAntes);

        // El turno debe seguir existiendo y permanecer en su estado inicial ASIGNADO
        assertThat(turnoRepository.existsById(turno.getId())).isTrue();
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turno.getId()))
                .isEqualTo("ASIGNADO");

        List<CambioEstado> historialTurno = cambioEstadoRepository
                .findByAmbitoAndEntidadIdOrderByFechaHoraInicioAscIdAsc(AmbitoEstado.TURNO, turno.getId());
        assertThat(historialTurno).hasSize(1);
        assertThat(historialTurno.getFirst().getEstado().getNombre()).isEqualTo("ASIGNADO");
        assertThat(historialTurno.getFirst().getFechaHoraFin()).isNull();
    }

    @TestConfiguration
    static class TestConfig {
    }

    @Service
    static class ServicioPruebaRollback {
        private final AplicarExcepcionAgenda aplicarExcepcionAgenda;

        public ServicioPruebaRollback(AplicarExcepcionAgenda aplicarExcepcionAgenda) {
            this.aplicarExcepcionAgenda = aplicarExcepcionAgenda;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void ejecutarConFallo(Long profesionalId,
                                      LocalDate fechaInicio,
                                      LocalDate fechaFin,
                                      TipoExcepcion tipo,
                                      LocalTime horaInicio,
                                      LocalTime horaFin,
                                      String motivo,
                                      String usuario) {
            // Ejecuta la aplicación de la excepción (crea excepción, cambio de estado, motivo de baja, notificación, auditoría)
            aplicarExcepcionAgenda.ejecutar(profesionalId, fechaInicio, fechaFin, tipo, horaInicio, horaFin, motivo, usuario);

            // Provoca intencionalmente una excepción de tiempo de ejecución para forzar el rollback de toda la transacción
            throw new RuntimeException("Fallo provocado intencionalmente para verificar rollback");
        }
    }
}

