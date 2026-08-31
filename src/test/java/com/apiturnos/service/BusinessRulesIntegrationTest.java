package com.apiturnos.service;

import com.apiturnos.agenda.model.*;
import com.apiturnos.agenda.repository.*;
import com.apiturnos.agenda.service.*;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.cliente.service.*;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.Notificacion;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.profesional.service.ModificarConfiguracionProfesional;
import com.apiturnos.shared.exception.*;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.model.TurnoHistorial;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.service.*;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class BusinessRulesIntegrationTest {

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
    private CambioEstadoRepository cambioEstadoRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Autowired
    private GestorCambioEstado gestorCambioEstado;
    @Autowired
    private RegistrarCliente registrarCliente;
    @Autowired
    private VerificarCliente verificarCliente;
    @Autowired
    private InhabilitarCliente inhabilitarCliente;
    @Autowired
    private DarDeBajaCliente darDeBajaCliente;
    @Autowired
    private ReactivarCliente reactivarCliente;
    @Autowired
    private CrearAgendaAnual crearAgendaAnual;
    @Autowired
    private ConfigurarMesAgenda configurarMesAgenda;
    @Autowired
    private ConfigurarDiaAgenda configurarDiaAgenda;
    @Autowired
    private RepetirConfiguracionMes repetirConfiguracionMes;
    @Autowired
    private ActivarInactivarMesAgenda activarInactivarMesAgenda;
    @Autowired
    private AplicarExcepcionAgenda aplicarExcepcionAgenda;
    @Autowired
    private CrearTurno crearTurno;
    @Autowired
    private AprobarTurno aprobarTurno;
    @Autowired
    private CancelarTurno cancelarTurno;
    @Autowired
    private ReprogramarTurno reprogramarTurno;
    @Autowired
    private RegistrarAsistencia registrarAsistencia;
    @Autowired
    private RegistrarAusencia registrarAusencia;
    @Autowired
    private ModificarConfiguracionProfesional modificarConfiguracionProfesional;

    private Profesional profesional1;
    private Profesional profesional2;
    private DiaAgenda diaAgenda1;

    @BeforeEach
    void setUp() {
        // Crear Profesional 1
        profesional1 = new Profesional();
        profesional1.setNombre("Laura");
        profesional1.setApellido("Fernandez");
        profesional1.setEmail("laura." + System.nanoTime() + "@turnos.com");
        profesional1.setTelefono("+5491144332211");
        profesional1.setEspecialidad("Cardiología");
        profesional1 = profesionalRepository.save(profesional1);

        // Configuración Profesional 1
        Configuracion config = new Configuracion();
        config.setProfesional(profesional1);
        config.setCantidadMaxTurnosALaVez(1);
        config.setDuracionAproximadaPorTurno(30);
        configuracionRepository.save(config);

        // Crear Profesional 2
        profesional2 = new Profesional();
        profesional2.setNombre("Roberto");
        profesional2.setApellido("Gimenez");
        profesional2.setEmail("roberto." + System.nanoTime() + "@turnos.com");
        profesional2.setTelefono("+5491155667788");
        profesional2.setEspecialidad("Traumatología");
        profesional2 = profesionalRepository.save(profesional2);

        // Agenda Profesional 1 para año 2026
        AgendaAnual agenda = crearAgendaAnual.ejecutar(profesional1.getId(), 2026, "admin");
        MesAgenda mes8 = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 8).orElseThrow();
        List<DiaAgenda> dias = configurarMesAgenda.ejecutar(mes8.getId());
        diaAgenda1 = dias.stream().filter(d -> d.getFecha().getDayOfMonth() == 15).findFirst().orElseThrow();

        // Brecha horaria: 09:00 a 13:00
        configurarDiaAgenda.ejecutar(diaAgenda1.getId(), List.of(
                new ConfigurarDiaAgenda.BrechaInput(LocalTime.of(9, 0), LocalTime.of(13, 0))
        ), "admin");
    }

    @Test
    @DisplayName("1. Mismo DNI permitido entre distintos profesionales pero rechazado en el mismo profesional")
    void test1_ValidacionDniEntreProfesionales() {
        String dniCompartido = "30111222";

        // Registrar cliente con profesional 1
        Cliente c1 = registrarCliente.ejecutar(
                profesional1.getId(), "Lucas", "Pratto",
                TipoDocumento.DNI, dniCompartido, "lucas1@test.com", "+5491100001", false, "admin");
        assertThat(c1.getId()).isNotNull();

        // Registrar cliente con profesional 2 (mismo DNI) -> permitido
        Cliente c2 = registrarCliente.ejecutar(
                profesional2.getId(), "Lucas", "Pratto",
                TipoDocumento.DNI, dniCompartido, "lucas2@test.com", "+5491100002", false, "admin");
        assertThat(c2.getId()).isNotNull();
        assertThat(c1.getId()).isNotEqualTo(c2.getId());

        // Intentar registrar duplicado en profesional 1 -> ClienteDuplicadoException
        assertThatThrownBy(() -> registrarCliente.ejecutar(
                profesional1.getId(), "Lucas Duplicado", "Pratto",
                TipoDocumento.DNI, dniCompartido, "lucas.dup@test.com", "+5491100003", false, "admin"))
                .isInstanceOf(ClienteDuplicadoException.class);
    }

    @Test
    @DisplayName("2. Reactivación de cliente recupera el estado anterior a DADO_DE_BAJA")
    void test2_ReactivacionRecuperaEstadoAnterior() {
        Cliente cliente = registrarCliente.ejecutar(
                profesional1.getId(), "Federico", "Mendez",
                TipoDocumento.DNI, "31222333", "fede@test.com", "+5491100004", false, "admin");

        // HABILITADO -> INHABILITADO
        inhabilitarCliente.ejecutar(cliente.getId(), "Mora reiterada", "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("INHABILITADO");

        // INHABILITADO -> DADO_DE_BAJA
        darDeBajaCliente.ejecutar(cliente.getId(), "Solicitud de baja", "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("DADO_DE_BAJA");

        // Reactivar: debe volver a INHABILITADO (el estado antes de la baja)
        reactivarCliente.ejecutar(cliente.getId(), "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("INHABILITADO");
    }

    @Test
    @DisplayName("3. Crear turno con cliente de otro profesional es rechazado")
    void test3_ClienteDeOtroProfesionalRechazado() {
        Cliente clienteProf2 = registrarCliente.ejecutar(
                profesional2.getId(), "Sonia", "Alvarez",
                TipoDocumento.DNI, "32333444", "sonia@test.com", "+5491100005", false, "admin");

        Instant inicio = Instant.parse("2026-08-15T10:00:00Z");
        Instant fin = Instant.parse("2026-08-15T10:30:00Z");

        // diaAgenda1 pertenece a profesional1, pero clienteProf2 pertenece a profesional2
        assertThatThrownBy(() -> crearTurno.ejecutar(
                diaAgenda1.getId(), clienteProf2.getId(), inicio, fin,
                OrigenTurno.PROFESIONAL, "Consulta", "admin"))
                .isInstanceOf(ClienteNoPerteneceProfesionalException.class);
    }

    @Test
    @DisplayName("4. Turno inicial depende del estado del cliente (HABILITADO -> ASIGNADO, REQUIERE_APROBACION -> PENDIENTE)")
    void test4_EstadoInicialTurnoSegunCliente() {
        Cliente clienteHabilitado = registrarCliente.ejecutar(
                profesional1.getId(), "Marcos", "Paz",
                TipoDocumento.DNI, "33444555", "marcos@test.com", "+5491100006", false, "admin");

        Instant inicio1 = Instant.parse("2026-08-15T09:00:00Z");
        Instant fin1 = Instant.parse("2026-08-15T09:30:00Z");

        CrearTurno.Resultado r1 = crearTurno.ejecutar(
                diaAgenda1.getId(), clienteHabilitado.getId(), inicio1, fin1,
                OrigenTurno.PROFESIONAL, "Consulta", "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, r1.getTurno().getId()))
                .isEqualTo("ASIGNADO");

        // Modificar estado del cliente a REQUIERE_APROBACION manualmente para probar la regla
        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, clienteHabilitado.getId(), "DADO_DE_BAJA", "admin", "test", null);
        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, clienteHabilitado.getId(), "REQUIERE_APROBACION", "admin", "test", null);

        Instant inicio2 = Instant.parse("2026-08-15T11:00:00Z");
        Instant fin2 = Instant.parse("2026-08-15T11:30:00Z");

        CrearTurno.Resultado r2 = crearTurno.ejecutar(
                diaAgenda1.getId(), clienteHabilitado.getId(), inicio2, fin2,
                OrigenTurno.PROFESIONAL, "Consulta 2", "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, r2.getTurno().getId()))
                .isEqualTo("PENDIENTE_DE_APROBACION");

        // Aprobar turno
        aprobarTurno.ejecutar(r2.getTurno().getId(), "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, r2.getTurno().getId()))
                .isEqualTo("ASIGNADO");
    }

    @Test
    @DisplayName("5. Reprogramación de turno conserva historial completo y crea TurnoHistorial")
    void test5_ReprogramacionConservaHistorial() {
        Cliente cliente = registrarCliente.ejecutar(
                profesional1.getId(), "Valeria", "Rios",
                TipoDocumento.DNI, "34555666", "valeria@test.com", "+5491100007", false, "admin");

        Instant inicio = Instant.parse("2026-08-15T09:00:00Z");
        Instant fin = Instant.parse("2026-08-15T09:30:00Z");

        CrearTurno.Resultado resultado = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente.getId(), inicio, fin,
                OrigenTurno.PROFESIONAL, "Turno original", "admin");
        Long turnoId = resultado.getTurno().getId();

        Instant nuevoInicio = Instant.parse("2026-08-15T12:00:00Z");
        Instant nuevoFin = Instant.parse("2026-08-15T12:30:00Z");

        reprogramarTurno.ejecutar(turnoId, diaAgenda1.getId(), nuevoInicio, nuevoFin,
                "Paciente solicitó cambio de horario", "admin");

        // Verificar estado actual
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turnoId))
                .isEqualTo("ASIGNADO");

        // Verificar historial de estados: ASIGNADO -> REPROGRAMADO -> ASIGNADO
        List<CambioEstado> historialEstados = gestorCambioEstado.obtenerHistorial(AmbitoEstado.TURNO, turnoId);
        assertThat(historialEstados).hasSize(3);
        assertThat(historialEstados.get(0).getEstado().getNombre()).isEqualTo("ASIGNADO");
        assertThat(historialEstados.get(1).getEstado().getNombre()).isEqualTo("REPROGRAMADO");
        assertThat(historialEstados.get(2).getEstado().getNombre()).isEqualTo("ASIGNADO");

        // Verificar TurnoHistorial
        List<TurnoHistorial> snapshots = turnoHistorialRepository.findByTurnoIdOrderByFechaEventoAsc(turnoId);
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getInicioEstimadoAnterior()).isEqualTo(inicio);
        assertThat(snapshots.get(0).getInicioEstimadoNuevo()).isEqualTo(nuevoInicio);
        assertThat(snapshots.get(0).getMotivo()).contains("cambio de horario");
    }

    @Test
    @DisplayName("6. Turnos manuales fuera de brecha y superpuestos están permitidos; autogestión respeta capacidad")
    void test6_CapacidadManualVsAutogestion() {
        Cliente c1 = registrarCliente.ejecutar(
                profesional1.getId(), "Clara", "Bustos",
                TipoDocumento.DNI, "35666777", "clara@test.com", "+5491100008", false, "admin");
        Cliente c2 = registrarCliente.ejecutar(
                profesional1.getId(), "Diego", "Torres",
                TipoDocumento.DNI, "36777888", "diego@test.com", "+5491100009", false, "admin");

        // Turno fuera de brecha horaria (brecha es 9-13, turno a las 15:00)
        Instant fueraDeBrechaInicio = Instant.parse("2026-08-15T15:00:00Z");
        Instant fueraDeBrechaFin = Instant.parse("2026-08-15T15:30:00Z");

        CrearTurno.Resultado r1 = crearTurno.ejecutar(
                diaAgenda1.getId(), c1.getId(), fueraDeBrechaInicio, fueraDeBrechaFin,
                OrigenTurno.PROFESIONAL, "Urgencia fuera de hora", "admin");
        assertThat(r1.getTurno().getId()).isNotNull();

        // Turno superpuesto manual (capacidadMax=1, se crea el segundo en el mismo horario)
        CrearTurno.Resultado r2 = crearTurno.ejecutar(
                diaAgenda1.getId(), c2.getId(), fueraDeBrechaInicio, fueraDeBrechaFin,
                OrigenTurno.PROFESIONAL, "Sobreturno manual", "admin");
        assertThat(r2.getTurno().getId()).isNotNull();
        assertThat(r2.isCapacidadExcedida()).isTrue(); // Advierte pero no bloquea

        // Autogestión en el mismo horario debe ser bloqueada por capacidad agotada
        Cliente c3 = registrarCliente.ejecutar(
                profesional1.getId(), "Elena", "Vega",
                TipoDocumento.DNI, "37888999", "elena@test.com", "+5491100010", false, "admin");

        assertThatThrownBy(() -> crearTurno.ejecutar(
                diaAgenda1.getId(), c3.getId(), fueraDeBrechaInicio, fueraDeBrechaFin,
                OrigenTurno.CLIENTE_AUTOGESTION, "Autogestion", "elena"))
                .isInstanceOf(CapacidadAgotadaException.class);
    }

    @Test
    @DisplayName("7. Aplicar excepción de agenda da de baja los turnos afectados y genera notificaciones")
    void test7_AplicarExcepcionDaDeBajaTurnos() {
        Cliente cliente = registrarCliente.ejecutar(
                profesional1.getId(), "Gustavo", "Ibañez",
                TipoDocumento.DNI, "38999000", "gustavo@test.com", "+5491100011", false, "admin");

        Instant inicio = Instant.parse("2026-08-15T09:30:00Z");
        Instant fin = Instant.parse("2026-08-15T10:00:00Z");

        CrearTurno.Resultado r = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente.getId(), inicio, fin,
                OrigenTurno.PROFESIONAL, "Consulta", "admin");
        Long turnoId = r.getTurno().getId();

        // Aplicar excepción de vacaciones que cubre el día 15
        aplicarExcepcionAgenda.ejecutar(
                profesional1.getId(), LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 16),
                TipoExcepcion.VACACIONES, "Congreso médico", "admin");

        // El turno debe estar en estado DADO_DE_BAJA
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turnoId))
                .isEqualTo("DADO_DE_BAJA");

        // Notificaciones generadas para el cliente
        List<Notificacion> notificaciones = notificacionRepository.findByClienteId(cliente.getId());
        assertThat(notificaciones).anyMatch(n -> n.getMensaje().contains("dado de baja"));
    }

    @Test
    @DisplayName("8. Repetir configuración de mes copia brechas a todos los días del mes siguiente")
    void test8_RepetirConfiguracionMes() {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(profesional2.getId(), 2027, "admin");
        MesAgenda mes1 = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 1).orElseThrow();
        mes1.setRepetirConfiguracion(true);
        mesAgendaRepository.save(mes1);

        List<DiaAgenda> diasEnero = configurarMesAgenda.ejecutar(mes1.getId());
        // Configurar brecha en los días de enero
        for (DiaAgenda d : diasEnero) {
            configurarDiaAgenda.ejecutar(d.getId(), List.of(
                    new ConfigurarDiaAgenda.BrechaInput(LocalTime.of(8, 0), LocalTime.of(12, 0))
            ), "admin");
        }

        // Repetir en febrero
        MesAgenda mes2 = repetirConfiguracionMes.ejecutar(mes1.getId());
        assertThat(mes2.getNroMes()).isEqualTo(2);

        List<DiaAgenda> diasFebrero = diaAgendaRepository.findByMesAgendaId(mes2.getId());
        assertThat(diasFebrero).isNotEmpty();

        // Verificar que un día de febrero tiene las brechas copiadas
        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(diasFebrero.get(0).getId());
        assertThat(brechas).hasSize(1);
        assertThat(brechas.get(0).getHoraInicioAtencion()).isEqualTo(LocalTime.of(8, 0));
        assertThat(brechas.get(0).getHoraFinAtencion()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("9. Cancelar turno registra estado CANCELADO con motivo de baja")
    void test9_CancelarTurnoConMotivo() {
        Cliente cliente = registrarCliente.ejecutar(
                profesional1.getId(), "Patricia", "Navarro",
                TipoDocumento.DNI, "39000111", "patricia@test.com", "+5491100012", false, "admin");

        Instant inicio = Instant.parse("2026-08-15T10:00:00Z");
        Instant fin = Instant.parse("2026-08-15T10:30:00Z");

        CrearTurno.Resultado r = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente.getId(), inicio, fin,
                OrigenTurno.PROFESIONAL, "Consulta", "admin");

        cancelarTurno.ejecutar(r.getTurno().getId(), "Imprevisto personal", "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, r.getTurno().getId()))
                .isEqualTo("CANCELADO");

        CambioEstado ultimo = gestorCambioEstado.obtenerCambioEstadoActual(AmbitoEstado.TURNO, r.getTurno().getId())
                .orElseThrow();
        assertThat(ultimo.getMotivoBajaTurno()).isNotNull();
        assertThat(ultimo.getMotivoBajaTurno().getMotivo()).isEqualTo("Imprevisto personal");
    }

    @Test
    @DisplayName("10. Registrar asistencia y ausencia actualizan el turno a COMPLETADO y NO_ASISTIO")
    void test10_AsistenciaYAusencia() {
        Cliente cliente = registrarCliente.ejecutar(
                profesional1.getId(), "Hernan", "Soto",
                TipoDocumento.DNI, "40111222", "hernan@test.com", "+5491100013", false, "admin");

        Instant inicio1 = Instant.parse("2026-08-15T10:30:00Z");
        Instant fin1 = Instant.parse("2026-08-15T11:00:00Z");

        CrearTurno.Resultado r1 = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente.getId(), inicio1, fin1,
                OrigenTurno.PROFESIONAL, "Consulta 1", "admin");

        registrarAsistencia.ejecutar(r1.getTurno().getId(), "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, r1.getTurno().getId()))
                .isEqualTo("COMPLETADO");

        Instant inicio2 = Instant.parse("2026-08-15T11:30:00Z");
        Instant fin2 = Instant.parse("2026-08-15T12:00:00Z");

        CrearTurno.Resultado r2 = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente.getId(), inicio2, fin2,
                OrigenTurno.PROFESIONAL, "Consulta 2", "admin");

        registrarAusencia.ejecutar(r2.getTurno().getId(), "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, r2.getTurno().getId()))
                .isEqualTo("NO_ASISTIO");
    }
}

