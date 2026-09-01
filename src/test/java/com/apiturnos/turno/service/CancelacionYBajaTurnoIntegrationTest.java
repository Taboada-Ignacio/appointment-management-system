package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.ConfigurarDiaAgenda;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.CanalNotificacion;
import com.apiturnos.notificacion.model.EstadoNotificacion;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.TransicionEstadoInvalidaException;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.model.TurnoHistorial;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Import(CancelacionYBajaTurnoIntegrationTest.ClockTestConfig.class)
class CancelacionYBajaTurnoIntegrationTest {

    private static final Instant AHORA = Instant.parse("2035-06-10T18:00:00Z");
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private ProfesionalRepository profesionalRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private AgendaAnualRepository agendaAnualRepository;
    @Autowired private MesAgendaRepository mesAgendaRepository;
    @Autowired private DiaAgendaRepository diaAgendaRepository;
    @Autowired private BrechaHorariaRepository brechaHorariaRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private TurnoHistorialRepository turnoHistorialRepository;
    @Autowired private CambioEstadoRepository cambioEstadoRepository;
    @Autowired private MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    @Autowired private AuditoriaEventoRepository auditoriaEventoRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private GestorCambioEstado gestorCambioEstado;
    @Autowired private CancelarTurno cancelarTurno;
    @Autowired private DarDeBajaTurno darDeBajaTurno;
    @Autowired private ConfigurarDiaAgenda configurarDiaAgenda;
    @Autowired private EntityManager entityManager;

    @Test
    void antesDelUmbralEliminaTurnoEHistorialPeroConservaAuditoriaYNotificacion() {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(25)), 24, true);
        TurnoHistorial historial = new TurnoHistorial();
        historial.setTurno(datos.turno());
        historial.setUsuario("test");
        historial.setMotivo("histórico previo");
        turnoHistorialRepository.saveAndFlush(historial);

        ResultadoCancelacionTurno resultado = cancelarTurno.ejecutar(
                datos.turno().getId(), null, "test");
        entityManager.clear();

        assertThat(resultado.resolucion()).isEqualTo(TipoResolucionCancelacion.ELIMINACION_ANTICIPADA);
        assertThat(turnoRepository.existsById(datos.turno().getId())).isFalse();
        assertThat(turnoHistorialRepository.findByTurnoIdOrderByFechaEventoAsc(datos.turno().getId())).isEmpty();
        assertThat(cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(
                AmbitoEstado.TURNO, datos.turno().getId())).isEmpty();
        assertThat(eventos(datos.turno()))
                .anySatisfy(e -> {
                    assertThat(e.getOperacion()).isEqualTo(OperacionAuditoria.DELETE);
                    assertThat(e.getDetalles()).startsWith("TURNO_ELIMINADO_ANTICIPADAMENTE");
                });
        assertThat(notificacionRepository.findByClienteId(datos.cliente().getId()))
                .anySatisfy(n -> {
                    assertThat(n.getTurno()).isNull();
                    assertThat(n.getTipo()).isEqualTo(TipoNotificacion.CANCELACION_TURNO);
                    assertThat(n.getCanal()).isEqualTo(CanalNotificacion.WHATSAPP);
                    assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
                });
    }

    @Test
    void exactamenteEnElUmbralCancelaConMotivoYConservaTodoElHistorial() {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(24)), 24, true);

        ResultadoCancelacionTurno resultado = cancelarTurno.ejecutar(
                datos.turno().getId(), "Decisión del cliente", "test");

        assertThat(resultado.resolucion()).isEqualTo(TipoResolucionCancelacion.CANCELACION_CON_HISTORIAL);
        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
        assertThat(gestorCambioEstado.obtenerHistorial(AmbitoEstado.TURNO, datos.turno().getId()))
                .extracting(c -> c.getEstado().getNombre())
                .containsExactly("ASIGNADO", "CANCELADO");
        assertThat(gestorCambioEstado.obtenerCambioEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .get()
                .satisfies(c -> assertThat(c.getMotivoBajaTurno().getMotivo())
                        .isEqualTo("Decisión del cliente"));
        assertThat(eventos(datos.turno()))
                .anyMatch(e -> e.getOperacion() == OperacionAuditoria.CANCEL
                        && e.getDetalles().startsWith("TURNO_CANCELADO"));
    }

    @Test
    void cancelacionCercanaExigeMotivoYRevierteSinCrearMotivo() {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, true);
        long motivosAntes = motivoBajaTurnoRepository.count();

        assertThatThrownBy(() -> cancelarTurno.ejecutar(datos.turno().getId(), " ", "test"))
                .isInstanceOf(EstadoInvalidoException.class);

        assertThat(motivoBajaTurnoRepository.count()).isEqualTo(motivosAntes);
        assertThat(gestorCambioEstado.obtenerHistorial(AmbitoEstado.TURNO, datos.turno().getId()))
                .extracting(c -> c.getEstado().getNombre())
                .containsExactly("ASIGNADO");
    }

    @Test
    void turnoIniciadoNoAdmiteCancelacionOrdinaria() {
        Datos datos = crearDatos(AHORA, 24, true);

        assertThatThrownBy(() -> cancelarTurno.ejecutar(datos.turno().getId(), "Tardía", "test"))
                .isInstanceOf(TurnoYaIniciadoException.class);

        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
    }

    @Test
    void bajaAdministrativaNoUsaUmbralConservaTurnoYGeneraNotificacion() {
        Datos datos = crearDatos(AHORA.plus(Duration.ofDays(90)), 1, true);

        darDeBajaTurno.ejecutar(datos.turno().getId(), "Intervención administrativa", "admin");

        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
        assertThat(gestorCambioEstado.obtenerHistorial(AmbitoEstado.TURNO, datos.turno().getId()))
                .extracting(c -> c.getEstado().getNombre())
                .containsExactly("ASIGNADO", "DADO_DE_BAJA");
        assertThat(gestorCambioEstado.obtenerCambioEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .get().satisfies(c -> assertThat(c.getMotivoBajaTurno()).isNotNull());
        assertThat(eventos(datos.turno()))
                .anyMatch(e -> e.getDetalles().startsWith("TURNO_DADO_DE_BAJA"));
        assertThat(notificacionRepository.findByTurnoId(datos.turno().getId()))
                .anyMatch(n -> n.getTipo() == TipoNotificacion.BAJA_TURNO
                        && n.getCanal() == CanalNotificacion.WHATSAPP
                        && n.getEstado() == EstadoNotificacion.PENDIENTE);
    }

    @Test
    void bajaExigeMotivoYEstadoTerminalNoAdmiteOperacionIncompatible() {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, false);
        assertThatThrownBy(() -> darDeBajaTurno.ejecutar(datos.turno().getId(), " ", "admin"))
                .isInstanceOf(EstadoInvalidoException.class);

        darDeBajaTurno.ejecutar(datos.turno().getId(), "Baja válida", "admin");
        assertThatThrownBy(() -> cancelarTurno.ejecutar(datos.turno().getId(), "Incompatible", "admin"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void reducirHorarioDelDiaDaDeBajaMedianteElFlujoCentral() {
        Instant inicio = Instant.parse("2035-06-12T13:00:00Z"); // 10:00 en Buenos Aires
        Datos datos = crearDatos(inicio, 24, true);
        BrechaHoraria original = new BrechaHoraria();
        original.setDiaAgenda(datos.dia());
        original.setHoraInicioAtencion(LocalTime.of(9, 0));
        original.setHoraFinAtencion(LocalTime.of(12, 0));
        brechaHorariaRepository.saveAndFlush(original);

        configurarDiaAgenda.ejecutar(datos.dia().getId(), List.of(
                new ConfigurarDiaAgenda.BrechaInput(LocalTime.of(11, 0), LocalTime.of(12, 0))), "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .isEqualTo("DADO_DE_BAJA");
        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
    }

    @Test
    void dosCancelacionesConcurrentesProducenUnaSolaResolucionDefinitiva() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, false);
        CountDownLatch salida = new CountDownLatch(1);
        CompletableFuture<Boolean> primera = cancelarConcurrentemente(datos.turno().getId(), salida, "a");
        CompletableFuture<Boolean> segunda = cancelarConcurrentemente(datos.turno().getId(), salida, "b");

        salida.countDown();
        List<Boolean> resultados = List.of(
                primera.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));

        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(gestorCambioEstado.obtenerHistorial(AmbitoEstado.TURNO, datos.turno().getId()))
                .extracting(c -> c.getEstado().getNombre())
                .containsExactly("ASIGNADO", "CANCELADO");
    }

    private CompletableFuture<Boolean> cancelarConcurrentemente(Long turnoId, CountDownLatch salida,
                                                                  String usuario) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                salida.await();
                cancelarTurno.ejecutar(turnoId, "Cancelación concurrente", usuario);
                return true;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            } catch (RuntimeException ex) {
                return false;
            }
        });
    }

    private List<com.apiturnos.auditoria.model.AuditoriaEvento> eventos(Turno turno) {
        return auditoriaEventoRepository.findByModuloAndEntidadAndEntidadIdOrderByFechaHoraDesc(
                "TURNO", "Turno", turno.getId().toString());
    }

    private Datos crearDatos(Instant inicio, int umbralHoras, boolean notificaciones) {
        String sufijo = UUID.randomUUID().toString();
        Profesional profesional = new Profesional();
        profesional.setNombre("Ana");
        profesional.setApellido("Test");
        profesional.setEmail("prof-" + sufijo + "@test.local");
        profesional.setTelefono("+5491100000000");
        profesional = profesionalRepository.save(profesional);

        Configuracion configuracion = new Configuracion();
        configuracion.setProfesional(profesional);
        configuracion.setUmbralCancelacionHoras(umbralHoras);
        configuracionRepository.save(configuracion);

        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesional);
        agenda.setAnio(inicio.atZone(ZONA).getYear());
        agenda = agendaAnualRepository.save(agenda);
        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(inicio.atZone(ZONA).getMonthValue());
        mes = mesAgendaRepository.save(mes);
        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(inicio.atZone(ZONA).toLocalDate());
        dia = diaAgendaRepository.save(dia);

        Cliente cliente = new Cliente();
        cliente.setProfesional(profesional);
        cliente.setNombre("Cliente");
        cliente.setApellido("Test");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento(sufijo.substring(0, 8));
        cliente.setEmail("cliente-" + sufijo + "@test.local");
        cliente.setTelefono("+5491199999999");
        cliente.setNotificacionesHabilitadas(notificaciones);
        cliente = clienteRepository.save(cliente);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.CLIENTE, cliente.getId(), "HABILITADO", "test", "Cliente de prueba");

        Turno turno = new Turno();
        turno.setDiaAgenda(dia);
        turno.setCliente(cliente);
        turno.setInicioEstimado(inicio);
        turno.setFinEstimado(inicio.plus(Duration.ofMinutes(30)));
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno = turnoRepository.save(turno);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.TURNO, turno.getId(), "ASIGNADO", "test", "Turno de prueba");
        return new Datos(turno, cliente, dia);
    }

    private record Datos(Turno turno, Cliente cliente, DiaAgenda dia) {
    }

    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(AHORA, ZONA);
        }
    }
}
