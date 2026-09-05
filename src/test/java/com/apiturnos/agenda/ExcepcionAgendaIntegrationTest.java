package com.apiturnos.agenda;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.AfectacionTurnoExcepcionRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.AplicarExcepcionAgenda;
import com.apiturnos.agenda.service.AplicarExcepcionConResoluciones;
import com.apiturnos.agenda.service.CancelarExcepcionAgenda;
import com.apiturnos.agenda.service.ModificarExcepcionAgenda;
import com.apiturnos.agenda.service.PrevisualizarExcepcionAgenda;
import com.apiturnos.agenda.service.ResultadoAplicacionExcepcionAgenda;
import com.apiturnos.agenda.service.SolicitudExcepcionAgenda;
import com.apiturnos.agenda.service.TokenImpactoExcepcionAgenda;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.CanalNotificacion;
import com.apiturnos.notificacion.model.EstadoNotificacion;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "turnos.zona-horaria=UTC")
@Testcontainers
@Transactional
class ExcepcionAgendaIntegrationTest {

    private static final int ANIO = 2044;

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
    private AfectacionTurnoExcepcionRepository afectacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private CambioEstadoRepository cambioEstadoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private GestorCambioEstado gestorCambioEstado;

    @Autowired
    private AplicarExcepcionAgenda aplicarExcepcionAgenda;

    @Autowired
    private ModificarExcepcionAgenda modificarExcepcionAgenda;

    @Autowired
    private CancelarExcepcionAgenda cancelarExcepcionAgenda;

    @Autowired
    private PrevisualizarExcepcionAgenda previsualizarExcepcionAgenda;

    @Autowired
    private AplicarExcepcionConResoluciones aplicarConResoluciones;

    @Autowired
    private TokenImpactoExcepcionAgenda tokenImpacto;

    @Autowired
    private CalcularDisponibilidadDia calcularDisponibilidadDia;

    @Autowired
    private EntityManager entityManager;

    private final Map<Integer, MesAgenda> meses = new HashMap<>();

    private Profesional profesional;
    private AgendaAnual agenda;
    private int secuenciaCliente;

    @BeforeEach
    void prepararProfesionalYAgenda() {
        profesional = crearProfesional("principal");

        agenda = new AgendaAnual();
        agenda.setProfesional(profesional);
        agenda.setAnio(ANIO);
        agenda = agendaAnualRepository.save(agenda);
    }

    @Test
    @DisplayName("Las queries excluyen excepciones inactivas y respetan fecha, rango y profesional")
    void queriesActivasAplicablesYRango() {
        ExcepcionAgenda vacaciones = guardarExcepcion(
                profesional,
                TipoExcepcion.VACACIONES,
                fecha(1, 10),
                fecha(1, 15),
                null,
                null,
                true,
                "Vacaciones");
        ExcepcionAgenda bloqueo = guardarExcepcion(
                profesional,
                TipoExcepcion.BLOQUEO_HORARIO,
                fecha(1, 20),
                fecha(1, 20),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                true,
                "Capacitacion");
        guardarExcepcion(
                profesional,
                TipoExcepcion.DIA_NO_LABORABLE,
                fecha(1, 12),
                fecha(1, 12),
                null,
                null,
                false,
                "Excepcion cancelada");

        Profesional otroProfesional = crearProfesional("otro");
        guardarExcepcion(
                otroProfesional,
                TipoExcepcion.VACACIONES,
                fecha(1, 1),
                fecha(1, 31),
                null,
                null,
                true,
                "Vacaciones ajenas");

        entityManager.flush();
        entityManager.clear();

        assertThat(excepcionAgendaRepository
                .findByProfesionalIdAndActivaTrueOrderByFechaInicioAscIdAsc(profesional.getId()))
                .extracting(ExcepcionAgenda::getId)
                .containsExactly(vacaciones.getId(), bloqueo.getId());
        assertThat(excepcionAgendaRepository.findActivasAplicablesAFecha(
                profesional.getId(), fecha(1, 12)))
                .extracting(ExcepcionAgenda::getId)
                .containsExactly(vacaciones.getId());
        assertThat(excepcionAgendaRepository.findActivasIntersectandoRango(
                profesional.getId(), fecha(1, 15), fecha(1, 20)))
                .extracting(ExcepcionAgenda::getId)
                .containsExactly(vacaciones.getId(), bloqueo.getId());
    }

    @Test
    @DisplayName("Un bloqueo da de baja solo el turno intersectado y conserva agenda, historial y turno")
    void bloqueoBajaSoloTurnoDentroYConservaTrazabilidad() {
        LocalDate dia = fecha(2, 5);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(12, 0));

        Turno dentro = crearTurno(
                diaAgenda,
                crearCliente(true),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30));
        Turno fuera = crearTurno(
                diaAgenda,
                crearCliente(true),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30));

        ExcepcionAgenda excepcion = aplicarExcepcionAgenda.ejecutar(
                profesional.getId(),
                dia,
                dia,
                TipoExcepcion.BLOQUEO_HORARIO,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Reunion institucional",
                "integration-test");

        assertThat(calcularDisponibilidadDia.ejecutar(profesional.getId(), dia))
                .containsExactly(
                        new IntervaloHorario(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        new IntervaloHorario(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        entityManager.flush();
        entityManager.clear();

        // La excepcion es una capa calculada: la brecha base permanece sin cambios.
        assertThat(brechaHorariaRepository.findByDiaAgendaId(diaAgenda.getId()))
                .singleElement()
                .satisfies(brecha -> {
                    assertThat(brecha.getHoraInicioAtencion()).isEqualTo(LocalTime.of(8, 0));
                    assertThat(brecha.getHoraFinAtencion()).isEqualTo(LocalTime.of(12, 0));
                });

        List<CambioEstado> historialDentro = historial(dentro);
        assertThat(historialDentro)
                .extracting(cambio -> cambio.getEstado().getNombre())
                .containsExactly("ASIGNADO", "DADO_DE_BAJA");
        assertThat(historialDentro.getFirst().getFechaHoraFin()).isNotNull();
        assertThat(historialDentro.getLast().getMotivoBajaTurno()).isNotNull();
        assertThat(historialDentro.getLast().getMotivoBajaTurno().getExcepcionAgenda().getId())
                .isEqualTo(excepcion.getId());
        assertThat(historialDentro.getLast().getMotivoBajaTurno().getMotivo())
                .contains("BLOQUEO_HORARIO");

        assertThat(estadoActual(fuera)).isEqualTo("ASIGNADO");
        assertThat(historial(fuera)).hasSize(1);

        assertThat(notificacionRepository.findByTurnoId(dentro.getId()))
                .singleElement()
                .satisfies(notificacion -> {
                    assertThat(notificacion.getCanal()).isEqualTo(CanalNotificacion.WHATSAPP);
                    assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
                    assertThat(notificacion.getTipo()).isEqualTo(TipoNotificacion.BAJA_TURNO);
                    assertThat(notificacion.getDestinatario()).isEqualTo(dentro.getCliente().getTelefono());
                });
        assertThat(notificacionRepository.findByTurnoId(fuera.getId())).isEmpty();

        assertThat(turnoRepository.findByDiaAgendaId(diaAgenda.getId()))
                .extracting(Turno::getId)
                .containsExactlyInAnyOrder(dentro.getId(), fuera.getId());
        assertThat(turnoRepository.findIntersectandoFranja(
                profesional.getId(),
                dia,
                instante(dia, LocalTime.of(9, 0)),
                instante(dia, LocalTime.of(10, 0))))
                .extracting(Turno::getId)
                .containsExactly(dentro.getId());
    }

    @Test
    @DisplayName("La baja no prepara WhatsApp cuando el cliente deshabilito notificaciones")
    void noNotificaCuandoClienteTieneNotificacionesDeshabilitadas() {
        LocalDate dia = fecha(3, 8);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(12, 0));
        Turno turno = crearTurno(
                diaAgenda,
                crearCliente(false),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30));

        aplicarExcepcionAgenda.ejecutar(
                profesional.getId(),
                dia,
                dia,
                TipoExcepcion.BLOQUEO_HORARIO,
                LocalTime.of(8, 30),
                LocalTime.of(10, 0),
                "Tarea administrativa",
                "integration-test");

        entityManager.flush();
        entityManager.clear();

        assertThat(estadoActual(turno)).isEqualTo("DADO_DE_BAJA");
        assertThat(notificacionRepository.findByTurnoId(turno.getId())).isEmpty();
        assertThat(turnoRepository.existsById(turno.getId())).isTrue();
    }

    @Test
    @DisplayName("Vacaciones multifecha cierran cada dia y dan de baja todos los turnos sin borrarlos")
    void vacacionesMultifechaDanDeBajaTodosLosTurnos() {
        LocalDate primerDia = fecha(4, 10);
        LocalDate segundoDia = fecha(4, 11);
        DiaAgenda agendaPrimerDia = crearDia(primerDia, "ACTIVO");
        DiaAgenda agendaSegundoDia = crearDia(segundoDia, "ACTIVO");
        crearBrecha(agendaPrimerDia, LocalTime.of(8, 0), LocalTime.of(12, 0));
        crearBrecha(agendaSegundoDia, LocalTime.of(8, 0), LocalTime.of(12, 0));

        Turno primero = crearTurno(
                agendaPrimerDia,
                crearCliente(false),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30));
        Turno segundo = crearTurno(
                agendaSegundoDia,
                crearCliente(false),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30));

        ExcepcionAgenda vacaciones = aplicarExcepcionAgenda.ejecutar(
                profesional.getId(),
                primerDia,
                segundoDia,
                TipoExcepcion.VACACIONES,
                "Licencia anual",
                "integration-test");

        assertThat(calcularDisponibilidadDia.ejecutar(profesional.getId(), primerDia)).isEmpty();
        assertThat(calcularDisponibilidadDia.ejecutar(profesional.getId(), segundoDia)).isEmpty();

        entityManager.flush();
        entityManager.clear();

        assertThat(estadoActual(primero)).isEqualTo("DADO_DE_BAJA");
        assertThat(estadoActual(segundo)).isEqualTo("DADO_DE_BAJA");
        assertThat(historial(primero).getLast().getMotivoBajaTurno().getExcepcionAgenda().getId())
                .isEqualTo(vacaciones.getId());
        assertThat(historial(segundo).getLast().getMotivoBajaTurno().getExcepcionAgenda().getId())
                .isEqualTo(vacaciones.getId());
        assertThat(turnoRepository.findByProfesionalAndFechaBetween(
                profesional.getId(), primerDia, segundoDia))
                .extracting(Turno::getId)
                .containsExactlyInAnyOrder(primero.getId(), segundo.getId());
    }

    @Test
    @DisplayName("La habilitacion extraordinaria activa el dia inactivo y al cancelar vuelve a inactivo sin cambiar su base ni bajar turnos")
    void habilitacionExtraordinariaNoBajaTurnosNiActivaLaBase() {
        LocalDate dia = fecha(5, 12);
        DiaAgenda diaAgenda = crearDia(dia, "INACTIVO");
        Turno turno = crearTurno(
                diaAgenda,
                crearCliente(true),
                LocalTime.of(18, 30),
                LocalTime.of(19, 0));

        ExcepcionAgenda excepcion = aplicarExcepcionAgenda.ejecutar(
                profesional.getId(),
                dia,
                dia,
                TipoExcepcion.HABILITACION_EXTRAORDINARIA,
                LocalTime.of(18, 0),
                LocalTime.of(21, 0),
                "Atencion extraordinaria",
                "integration-test");

        assertThat(calcularDisponibilidadDia.ejecutar(profesional.getId(), dia))
                .containsExactly(new IntervaloHorario(LocalTime.of(18, 0), LocalTime.of(21, 0)));

        entityManager.flush();
        entityManager.clear();

        assertThat(estadoActual(turno)).isEqualTo("ASIGNADO");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.DIA_AGENDA, diaAgenda.getId())).isEqualTo("ACTIVO");
        assertThat(brechaHorariaRepository.findByDiaAgendaId(diaAgenda.getId())).isEmpty();
        assertThat(notificacionRepository.findByTurnoId(turno.getId())).isEmpty();
        assertThat(turnoRepository.existsById(turno.getId())).isTrue();

        // Al cancelar la habilitación, el día vuelve a INACTIVO
        cancelarExcepcionAgenda.ejecutar(profesional.getId(), excepcion.getId(), "integration-test");

        entityManager.flush();
        entityManager.clear();

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.DIA_AGENDA, diaAgenda.getId())).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("Modificar una excepcion no reactiva silenciosamente turnos ya dados de baja")
    void modificarExcepcionNoReactivaTurnoDadoDeBaja() {
        LocalDate dia = fecha(6, 15);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(12, 0));
        Turno turno = crearTurno(
                diaAgenda,
                crearCliente(true),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30));

        ExcepcionAgenda excepcion = aplicarExcepcionAgenda.ejecutar(
                profesional.getId(),
                dia,
                dia,
                TipoExcepcion.BLOQUEO_HORARIO,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Bloqueo original",
                "integration-test");

        modificarExcepcionAgenda.ejecutar(
                profesional.getId(),
                excepcion.getId(),
                new SolicitudExcepcionAgenda(
                        dia,
                        dia,
                        TipoExcepcion.BLOQUEO_HORARIO,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        "Bloqueo corregido"),
                "integration-test");

        assertThat(calcularDisponibilidadDia.ejecutar(profesional.getId(), dia))
                .containsExactly(
                        new IntervaloHorario(LocalTime.of(8, 0), LocalTime.of(10, 0)),
                        new IntervaloHorario(LocalTime.of(11, 0), LocalTime.of(12, 0)));

        entityManager.flush();
        entityManager.clear();

        assertThat(historial(turno))
                .extracting(cambio -> cambio.getEstado().getNombre())
                .containsExactly("ASIGNADO", "DADO_DE_BAJA");
        assertThat(notificacionRepository.findByTurnoId(turno.getId())).hasSize(1);
        assertThat(turnoRepository.existsById(turno.getId())).isTrue();
        assertThat(brechaHorariaRepository.findByDiaAgendaId(diaAgenda.getId()))
                .singleElement()
                .satisfies(brecha -> {
                    assertThat(brecha.getHoraInicioAtencion()).isEqualTo(LocalTime.of(8, 0));
                    assertThat(brecha.getHoraFinAtencion()).isEqualTo(LocalTime.of(12, 0));
                });
    }

    @Test
    @DisplayName("Modificación con múltiples brechas reemplaza base atómicamente y da de baja turnos fuera del nuevo horario")
    void modificacionHorariaConMultiplesBrechasReemplazaBaseYProcesaBajas() {
        LocalDate dia = fecha(7, 20);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(20, 0));

        Turno turnoManana = crearTurno(diaAgenda, crearCliente(false), LocalTime.of(9, 0), LocalTime.of(9, 30));
        Turno turnoMediodia = crearTurno(diaAgenda, crearCliente(false), LocalTime.of(13, 0), LocalTime.of(13, 30));
        Turno turnoTarde = crearTurno(diaAgenda, crearCliente(false), LocalTime.of(16, 0), LocalTime.of(16, 30));

        SolicitudExcepcionAgenda solicitud = new SolicitudExcepcionAgenda(
                dia,
                dia,
                TipoExcepcion.MODIFICACION_HORARIO,
                List.of(
                        new IntervaloHorario(LocalTime.of(8, 30), LocalTime.of(12, 0)),
                        new IntervaloHorario(LocalTime.of(15, 0), LocalTime.of(19, 0))),
                "Horario partido por jornada especial");

        ExcepcionAgenda modificacion = aplicarExcepcionAgenda.ejecutar(profesional.getId(), solicitud, "integration-test");

        assertThat(calcularDisponibilidadDia.ejecutar(profesional.getId(), dia))
                .containsExactly(
                        new IntervaloHorario(LocalTime.of(8, 30), LocalTime.of(12, 0)),
                        new IntervaloHorario(LocalTime.of(15, 0), LocalTime.of(19, 0)));

        entityManager.flush();
        entityManager.clear();

        assertThat(estadoActual(turnoManana)).isEqualTo("ASIGNADO");
        assertThat(estadoActual(turnoMediodia)).isEqualTo("DADO_DE_BAJA");
        assertThat(estadoActual(turnoTarde)).isEqualTo("ASIGNADO");

        // Brecha base original de 08:00 a 20:00 se mantiene intacta
        assertThat(brechaHorariaRepository.findByDiaAgendaId(diaAgenda.getId()))
                .singleElement()
                .satisfies(brecha -> {
                    assertThat(brecha.getHoraInicioAtencion()).isEqualTo(LocalTime.of(8, 0));
                    assertThat(brecha.getHoraFinAtencion()).isEqualTo(LocalTime.of(20, 0));
                });
    }

    @Test
    @DisplayName("Modificar una excepción que genera nuevas bajas crea un nuevo MotivoBajaTurno sin mutar el histórico")
    void modificacionExcepcionCreaNuevoMotivoBajaSinMutarHistoricos() {
        LocalDate dia = fecha(8, 10);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(18, 0));

        Turno turno1 = crearTurno(diaAgenda, crearCliente(false), LocalTime.of(9, 0), LocalTime.of(9, 30));
        Turno turno2 = crearTurno(diaAgenda, crearCliente(false), LocalTime.of(10, 30), LocalTime.of(11, 0));

        // 1. Primer bloqueo de 09:00 a 10:00 (afecta solo a turno1)
        ExcepcionAgenda excepcion = aplicarExcepcionAgenda.ejecutar(
                profesional.getId(),
                dia,
                dia,
                TipoExcepcion.BLOQUEO_HORARIO,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Motivo original v1",
                "integration-test");

        entityManager.flush();
        entityManager.clear();

        CambioEstado cambioTurno1 = historial(turno1).getLast();
        assertThat(cambioTurno1.getEstado().getNombre()).isEqualTo("DADO_DE_BAJA");
        assertThat(cambioTurno1.getMotivoBajaTurno().getMotivo()).contains("Motivo original v1");
        Long idMotivo1 = cambioTurno1.getMotivoBajaTurno().getId();

        // 2. Modificación del bloqueo a 09:00 a 12:00 (afecta ahora también a turno2)
        modificarExcepcionAgenda.ejecutar(
                profesional.getId(),
                excepcion.getId(),
                new SolicitudExcepcionAgenda(
                        dia,
                        dia,
                        TipoExcepcion.BLOQUEO_HORARIO,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        "Motivo extendido v2"),
                "integration-test");

        entityManager.flush();
        entityManager.clear();

        CambioEstado cambioTurno1Post = historial(turno1).getLast();
        CambioEstado cambioTurno2Post = historial(turno2).getLast();

        assertThat(cambioTurno2Post.getEstado().getNombre()).isEqualTo("DADO_DE_BAJA");
        Long idMotivo2 = cambioTurno2Post.getMotivoBajaTurno().getId();

        // Demuestra que se creó un nuevo motivo para la nueva ejecución y el histórico de turno1 no fue mutado
        assertThat(idMotivo1).isNotEqualTo(idMotivo2);
        assertThat(cambioTurno1Post.getMotivoBajaTurno().getId()).isEqualTo(idMotivo1);
        assertThat(cambioTurno1Post.getMotivoBajaTurno().getMotivo()).contains("Motivo original v1");
        assertThat(cambioTurno2Post.getMotivoBajaTurno().getMotivo()).contains("Motivo extendido v2");
    }

    @Test
    @DisplayName("Cancelar una excepción desactiva la excepción y no reactiva turnos dados de baja")
    void cancelarExcepcionDesactivaExcepcionYSinReactivarTurnos() {
        LocalDate dia = fecha(9, 5);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(12, 0));
        Turno turno = crearTurno(diaAgenda, crearCliente(false), LocalTime.of(9, 0), LocalTime.of(9, 30));

        ExcepcionAgenda excepcion = aplicarExcepcionAgenda.ejecutar(
                profesional.getId(),
                dia,
                dia,
                TipoExcepcion.BLOQUEO_HORARIO,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Bloqueo temporal",
                "integration-test");

        assertThat(estadoActual(turno)).isEqualTo("DADO_DE_BAJA");

        cancelarExcepcionAgenda.ejecutar(profesional.getId(), excepcion.getId(), "integration-test");

        entityManager.flush();
        entityManager.clear();

        ExcepcionAgenda excepcionCancelada = excepcionAgendaRepository.findById(excepcion.getId()).orElseThrow();
        assertThat(excepcionCancelada.isActiva()).isFalse();

        // Disponibilidad del día se restaura
        assertThat(calcularDisponibilidadDia.ejecutar(profesional.getId(), dia))
                .containsExactly(new IntervaloHorario(LocalTime.of(8, 0), LocalTime.of(12, 0)));

        // Pero el turno dado de baja no se reactiva automáticamente
        assertThat(estadoActual(turno)).isEqualTo("DADO_DE_BAJA");
    }

    @Test
    @DisplayName("Previsualizar detecta turnos afectados sin persistir la excepción ni efectos transversales")
    void previsualizarNoProduceEfectosSecundarios() {
        LocalDate dia = fecha(7, 15);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(12, 0));
        Turno turno = crearTurno(
                diaAgenda, crearCliente(true), LocalTime.of(9, 0), LocalTime.of(9, 30));

        List<Turno> afectados = previsualizarExcepcionAgenda.nueva(
                profesional.getId(),
                new SolicitudExcepcionAgenda(
                        dia, dia, TipoExcepcion.DIA_NO_LABORABLE,
                        null, null, "Capacitación"));

        assertThat(afectados).extracting(Turno::getId).containsExactly(turno.getId());
        assertThat(excepcionAgendaRepository.count()).isZero();
        assertThat(estadoActual(turno)).isEqualTo("ASIGNADO");
        assertThat(historial(turno)).hasSize(1);
        assertThat(notificacionRepository.findByTurnoId(turno.getId())).isEmpty();
    }

    @Test
    @DisplayName("Confirmar con resolución opcional conserva el turno y lo deja afectado y pendiente")
    void aplicarConTurnoPendiente() {
        LocalDate dia = fecha(8, 12);
        DiaAgenda diaAgenda = crearDia(dia, "ACTIVO");
        crearBrecha(diaAgenda, LocalTime.of(8, 0), LocalTime.of(12, 0));
        Turno turno = crearTurno(
                diaAgenda, crearCliente(true), LocalTime.of(9, 0), LocalTime.of(9, 30));
        SolicitudExcepcionAgenda solicitud = new SolicitudExcepcionAgenda(
                dia, dia, TipoExcepcion.DIA_NO_LABORABLE, null, null, "Capacitación");
        List<Turno> preview = previsualizarExcepcionAgenda.nueva(profesional.getId(), solicitud);

        aplicarConResoluciones.ejecutar(
                profesional.getId(), solicitud, tokenImpacto.generar(solicitud, preview),
                List.of(), "integration-test");

        assertThat(estadoActual(turno)).isEqualTo("AFECTADO_POR_EXCEPCION");
        assertThat(turnoRepository.existsById(turno.getId())).isTrue();
        assertThat(notificacionRepository.findByTurnoId(turno.getId())).isEmpty();
        assertThat(afectacionRepository.findAll()).singleElement()
                .satisfies(a -> assertThat(a.getEstadoResolucion().name()).isEqualTo("PENDIENTE"));
    }

    private Profesional crearProfesional(String sufijo) {
        Profesional nuevo = new Profesional();
        nuevo.setNombre("Profesional");
        nuevo.setApellido("Integracion");
        nuevo.setEmail(sufijo + "." + System.nanoTime() + "@excepciones.test");
        nuevo.setTelefono("+5491100000000");
        nuevo.setEspecialidad("Clinica");
        return profesionalRepository.save(nuevo);
    }

    private DiaAgenda crearDia(LocalDate fecha, String estadoInicial) {
        MesAgenda mes = meses.computeIfAbsent(fecha.getMonthValue(), numeroMes -> {
            MesAgenda nuevoMes = new MesAgenda();
            nuevoMes.setAgendaAnual(agenda);
            nuevoMes.setNroMes(numeroMes);
            nuevoMes.setRepetirConfiguracion(false);
            return mesAgendaRepository.save(nuevoMes);
        });

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(fecha);
        dia = diaAgendaRepository.save(dia);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA,
                dia.getId(),
                estadoInicial,
                "integration-test",
                "Estado inicial del dia");
        return dia;
    }

    private BrechaHoraria crearBrecha(DiaAgenda dia, LocalTime inicio, LocalTime fin) {
        BrechaHoraria brecha = new BrechaHoraria();
        brecha.setDiaAgenda(dia);
        brecha.setHoraInicioAtencion(inicio);
        brecha.setHoraFinAtencion(fin);
        return brechaHorariaRepository.save(brecha);
    }

    private Cliente crearCliente(boolean notificacionesHabilitadas) {
        secuenciaCliente++;
        String secuencia = Integer.toString(secuenciaCliente);
        Cliente cliente = new Cliente();
        cliente.setProfesional(profesional);
        cliente.setNombre("Cliente");
        cliente.setApellido("Integracion " + secuencia);
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("4400000" + secuencia);
        cliente.setEmail("cliente." + secuencia + "." + System.nanoTime() + "@excepciones.test");
        cliente.setTelefono("+549119000000" + secuencia);
        cliente.setNotificacionesHabilitadas(notificacionesHabilitadas);
        return clienteRepository.save(cliente);
    }

    private Turno crearTurno(DiaAgenda dia, Cliente cliente, LocalTime inicio, LocalTime fin) {
        Turno turno = new Turno();
        turno.setDiaAgenda(dia);
        turno.setCliente(cliente);
        turno.setInicioEstimado(instante(dia.getFecha(), inicio));
        turno.setFinEstimado(instante(dia.getFecha(), fin));
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno = turnoRepository.save(turno);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.TURNO,
                turno.getId(),
                "ASIGNADO",
                "integration-test",
                "Turno inicial");
        return turno;
    }

    private ExcepcionAgenda guardarExcepcion(
            Profesional profesional,
            TipoExcepcion tipo,
            LocalDate inicio,
            LocalDate fin,
            LocalTime horaInicio,
            LocalTime horaFin,
            boolean activa,
            String motivo) {
        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setProfesional(profesional);
        excepcion.setTipo(tipo);
        excepcion.setFechaInicio(inicio);
        excepcion.setFechaFin(fin);
        excepcion.setHoraInicio(horaInicio);
        excepcion.setHoraFin(horaFin);
        excepcion.setActiva(activa);
        excepcion.setMotivo(motivo);
        return excepcionAgendaRepository.save(excepcion);
    }

    private List<CambioEstado> historial(Turno turno) {
        return cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAscIdAsc(
                AmbitoEstado.TURNO, turno.getId());
    }

    private String estadoActual(Turno turno) {
        return gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turno.getId());
    }

    private LocalDate fecha(int mes, int dia) {
        return LocalDate.of(ANIO, mes, dia);
    }

    private Instant instante(LocalDate fecha, LocalTime hora) {
        return fecha.atTime(hora).toInstant(ZoneOffset.UTC);
    }
}
