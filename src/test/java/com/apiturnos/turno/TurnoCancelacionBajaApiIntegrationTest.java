package com.apiturnos.turno;

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
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
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
import com.apiturnos.turno.dto.CancelarTurnoRequestDto;
import com.apiturnos.turno.dto.DarDeBajaTurnoRequestDto;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.model.TurnoHistorial;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Import(TurnoCancelacionBajaApiIntegrationTest.ClockTestConfig.class)
class TurnoCancelacionBajaApiIntegrationTest {

    private static final Instant AHORA = Instant.parse("2035-06-10T18:00:00Z");
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private ProfesionalRepository profesionalRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private AgendaAnualRepository agendaAnualRepository;
    @Autowired private MesAgendaRepository mesAgendaRepository;
    @Autowired private DiaAgendaRepository diaAgendaRepository;
    @Autowired private BrechaHorariaRepository brechaHorariaRepository;
    @Autowired private TipoAtencionRepository tipoAtencionRepository;
    @Autowired private ExcepcionAgendaRepository excepcionAgendaRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private TurnoHistorialRepository turnoHistorialRepository;
    @Autowired private CambioEstadoRepository cambioEstadoRepository;
    @Autowired private MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    @Autowired private AuditoriaEventoRepository auditoriaEventoRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private GestorCambioEstado gestorCambioEstado;
    @Autowired private AplicarExcepcionAgenda aplicarExcepcionAgenda;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("1. Cancelar anticipadamente devuelve resultado ELIMINADO_ANTICIPADAMENTE y borra registro")
    void test01_CancelarAnticipadamenteDevuelveEliminado() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(25)), 24, true);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("ELIMINADO_ANTICIPADAMENTE")))
                .andExpect(jsonPath("$.turnoId", is(datos.turno().getId().intValue())))
                .andExpect(jsonPath("$.turno", is(nullValue())));

        assertThat(turnoRepository.existsById(datos.turno().getId())).isFalse();
        assertThat(turnoHistorialRepository.findByTurnoIdOrderByFechaEventoAsc(datos.turno().getId())).isEmpty();
        assertThat(cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(
                AmbitoEstado.TURNO, datos.turno().getId())).isEmpty();
    }

    @Test
    @DisplayName("2. Cancelar dentro del umbral devuelve CANCELADO y conserva registro en BD")
    void test02_CancelarDentroDelUmbralDevuelveCancelado() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(5)), 24, true);

        CancelarTurnoRequestDto request = new CancelarTurnoRequestDto("Imprevisto médico");

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("CANCELADO")))
                .andExpect(jsonPath("$.turnoId", is(datos.turno().getId().intValue())))
                .andExpect(jsonPath("$.turno.id", is(datos.turno().getId().intValue())))
                .andExpect(jsonPath("$.turno.estado", is("CANCELADO")));

        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .isEqualTo("CANCELADO");
    }

    @Test
    @DisplayName("3. Cancelar exactamente en el límite devuelve CANCELADO")
    void test03_CancelarExactamenteEnElLimiteDevuelveCancelado() throws Exception {
        // En exactamente 24 horas (inicioEstimado - umbral = ahora)
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(24)), 24, true);

        CancelarTurnoRequestDto request = new CancelarTurnoRequestDto("Cancelación en el límite");

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("CANCELADO")))
                .andExpect(jsonPath("$.turno.estado", is("CANCELADO")));

        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
    }

    @Test
    @DisplayName("4. CANCELADO sin motivo produce error 400")
    void test04_CanceladoSinMotivoProduceError400() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(5)), 24, true);

        CancelarTurnoRequestDto requestVacio = new CancelarTurnoRequestDto("   ");

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVacio)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", Matchers.containsString("MotivoBajaTurno")));
    }

    @Test
    @DisplayName("5. Baja administrativa produce DADO_DE_BAJA")
    void test05_BajaAdministrativaProduceDadoDeBaja() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofDays(30)), 24, true);

        DarDeBajaTurnoRequestDto request = new DarDeBajaTurnoRequestDto("Baja por solicitud judicial");

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/baja",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(datos.turno().getId().intValue())))
                .andExpect(jsonPath("$.estado", is("DADO_DE_BAJA")));

        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .isEqualTo("DADO_DE_BAJA");
    }

    @Test
    @DisplayName("6. Baja exige motivo (400 Bad Request)")
    void test06_BajaExigeMotivo() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofDays(30)), 24, true);

        DarDeBajaTurnoRequestDto requestSinMotivo = new DarDeBajaTurnoRequestDto("");

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/baja",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestSinMotivo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.motivo").isNotEmpty());
    }

    @Test
    @DisplayName("7. Cancelación y baja utilizan endpoints diferentes")
    void test07_CancelacionYBajaUtilizanEndpointsDiferentes() throws Exception {
        Datos datos1 = crearDatos(AHORA.plus(Duration.ofDays(5)), 24, true);
        Datos datos2 = crearDatos(AHORA.plus(Duration.ofDays(5)), 24, true);

        // Cancelación con 5 días de anticipación -> ELIMINACION_ANTICIPADA
        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos1.profesional().getId(), datos1.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("ELIMINADO_ANTICIPADAMENTE")));

        // Baja administrativa con 5 días de anticipación -> DADO_DE_BAJA (no se elimina)
        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/baja",
                        datos2.profesional().getId(), datos2.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DarDeBajaTurnoRequestDto("Baja admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("DADO_DE_BAJA")));

        assertThat(turnoRepository.existsById(datos1.turno().getId())).isFalse();
        assertThat(turnoRepository.existsById(datos2.turno().getId())).isTrue();
    }

    @Test
    @DisplayName("8. Profesional no puede cancelar Turno ajeno (403 Forbidden)")
    void test08_ProfesionalNoPuedeCancelarTurnoAjeno() throws Exception {
        Datos datosProf1 = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, true);
        Profesional prof2 = crearProfesional("Dr. Ajeno");

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        prof2.getId(), datosProf1.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Motivo"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", Matchers.containsString("no pertenece al profesional")));
    }

    @Test
    @DisplayName("9. Profesional no puede dar de baja Turno ajeno (403 Forbidden)")
    void test09_ProfesionalNoPuedeDarDeBajaTurnoAjeno() throws Exception {
        Datos datosProf1 = crearDatos(AHORA.plus(Duration.ofDays(2)), 24, true);
        Profesional prof2 = crearProfesional("Dr. Ajeno 2");

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/baja",
                        prof2.getId(), datosProf1.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DarDeBajaTurnoRequestDto("Baja"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", Matchers.containsString("no pertenece al profesional")));
    }

    @Test
    @DisplayName("10. Turno ya iniciado rechaza cancelación ordinaria (400 Bad Request)")
    void test10_TurnoYaIniciadoRechazaCancelacionOrdinaria() throws Exception {
        Datos datos = crearDatos(AHORA.minus(Duration.ofMinutes(10)), 24, true);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Tardío"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", Matchers.containsString("ya inició")));
    }

    @Test
    @DisplayName("11. Estado terminal rechaza nueva cancelación (400 Bad Request)")
    void test11_EstadoTerminalRechazaNuevaCancelacion() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, true);

        // Primera cancelación -> CANCELADO
        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Primera cancelación"))))
                .andExpect(status().isOk());

        // Segunda cancelación -> 400 Bad Request
        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Segunda cancelación"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("12. Vacaciones continúan generando DADO_DE_BAJA correctamente")
    void test12_VacacionesGeneranDadoDeBajaCorrectamente() throws Exception {
        Instant inicio = Instant.parse("2035-06-15T13:00:00Z");
        Datos datos = crearDatos(inicio, 24, true);

        aplicarExcepcionAgenda.ejecutar(
                datos.profesional().getId(),
                LocalDate.of(2035, 6, 15),
                LocalDate.of(2035, 6, 20),
                TipoExcepcion.VACACIONES,
                "Vacaciones de invierno",
                "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .isEqualTo("DADO_DE_BAJA");
        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
    }

    @Test
    @DisplayName("13. Excepción continúa generando DADO_DE_BAJA")
    void test13_ExcepcionContinuaGenerandoDadoDeBaja() throws Exception {
        Instant inicio = Instant.parse("2035-06-15T13:00:00Z"); // 10:00 en Buenos Aires
        Datos datos = crearDatos(inicio, 24, true);

        aplicarExcepcionAgenda.ejecutar(
                datos.profesional().getId(),
                LocalDate.of(2035, 6, 15),
                LocalDate.of(2035, 6, 15),
                TipoExcepcion.BLOQUEO_HORARIO,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "Corte de luz programado",
                "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .isEqualTo("DADO_DE_BAJA");
        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
    }

    @Test
    @DisplayName("14. CANCELADO conserva historial (TurnoHistorial y CambioEstado)")
    void test14_CanceladoConservaHistorial() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, true);

        TurnoHistorial h1 = new TurnoHistorial();
        h1.setTurno(datos.turno());
        h1.setUsuario("recepcionista");
        h1.setMotivo("Asignación de turno");
        turnoHistorialRepository.saveAndFlush(h1);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Cancelado por cliente"))))
                .andExpect(status().isOk());

        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
        assertThat(turnoHistorialRepository.findByTurnoIdOrderByFechaEventoAsc(datos.turno().getId())).hasSize(1);
        assertThat(cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(
                AmbitoEstado.TURNO, datos.turno().getId())).hasSize(2);
    }

    @Test
    @DisplayName("15. DADO_DE_BAJA conserva historial")
    void test15_DadoDeBajaConservaHistorial() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofDays(2)), 24, true);

        TurnoHistorial h1 = new TurnoHistorial();
        h1.setTurno(datos.turno());
        h1.setUsuario("recepcionista");
        h1.setMotivo("Asignación de turno");
        turnoHistorialRepository.saveAndFlush(h1);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/baja",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DarDeBajaTurnoRequestDto("Baja administrativa"))))
                .andExpect(status().isOk());

        assertThat(turnoRepository.existsById(datos.turno().getId())).isTrue();
        assertThat(turnoHistorialRepository.findByTurnoIdOrderByFechaEventoAsc(datos.turno().getId())).hasSize(1);
        assertThat(cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(
                AmbitoEstado.TURNO, datos.turno().getId())).hasSize(2);
    }

    @Test
    @DisplayName("16. Auditoría se genera mediante caso de uso")
    void test16_AuditoriaSeGeneraMedianteCasoDeUso() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, true);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .header("X-Usuario", "dra_ana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Cancelado"))))
                .andExpect(status().isOk());

        var auditorias = auditoriaEventoRepository.findByModuloAndEntidadAndEntidadIdOrderByFechaHoraDesc(
                "TURNO", "Turno", datos.turno().getId().toString());

        assertThat(auditorias).anySatisfy(a -> {
            assertThat(a.getOperacion()).isEqualTo(OperacionAuditoria.CANCEL);
            assertThat(a.getUsuario()).isEqualTo("dra_ana");
            assertThat(a.getProfesionalId()).isEqualTo(datos.profesional().getId());
            assertThat(a.getDetalles()).startsWith("TURNO_CANCELADO: Cancelado");
        });
    }

    @Test
    @DisplayName("17. Notificación se genera según reglas existentes")
    void test17_NotificacionSeGeneraSegunReglasExistentes() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, true);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Cancelación con aviso"))))
                .andExpect(status().isOk());

        var notificaciones = notificacionRepository.findByClienteId(datos.cliente().getId());
        assertThat(notificaciones).anySatisfy(n -> {
            assertThat(n.getTipo()).isEqualTo(TipoNotificacion.CANCELACION_TURNO);
            assertThat(n.getCanal()).isEqualTo(CanalNotificacion.WHATSAPP);
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        });
    }

    @Test
    @DisplayName("18. Respuestas no exponen entidades JPA")
    void test18_RespuestasNoExponenEntidadesJpa() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(2)), 24, true);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Privacidad"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist())
                .andExpect(jsonPath("$.turno.diaAgenda").doesNotExist())
                .andExpect(jsonPath("$.turno.profesional").doesNotExist())
                .andExpect(jsonPath("$.turno.cliente.profesional").doesNotExist());
    }

    @Test
    @DisplayName("19. Validaciones DTO funcionan")
    void test19_ValidacionesDtoFuncionan() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofDays(1)), 24, true);

        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/baja",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DarDeBajaTurnoRequestDto("   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.motivo").value("El motivo de baja es obligatorio"));
    }

    @Test
    @DisplayName("20. Suite completa no presenta regresiones (verificación de creación y posterior cancelación)")
    void test20_CreacionYPosteriorCancelacionSinRegresiones() throws Exception {
        Datos datos = crearDatos(AHORA.plus(Duration.ofHours(4)), 24, true);

        // Turno inicialmente ASIGNADO
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .isEqualTo("ASIGNADO");

        // Cancelar vía REST
        mockMvc.perform(post("/api/profesionales/{profId}/turnos/{turnoId}/cancelacion",
                        datos.profesional().getId(), datos.turno().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Cancelación flujo completo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("CANCELADO")));

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, datos.turno().getId()))
                .isEqualTo("CANCELADO");
    }

    private Profesional crearProfesional(String nombre) {
        String sufijo = UUID.randomUUID().toString();
        Profesional profesional = new Profesional();
        profesional.setNombre(nombre);
        profesional.setApellido("Test");
        profesional.setEmail("prof-" + sufijo + "@test.local");
        profesional.setTelefono("+5491100000000");
        return profesionalRepository.save(profesional);
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
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA, dia.getId(), "ACTIVO", "setup", "Día activo");

        BrechaHoraria brecha = new BrechaHoraria();
        brecha.setDiaAgenda(dia);
        brecha.setHoraInicioAtencion(LocalTime.of(8, 0));
        brecha.setHoraFinAtencion(LocalTime.of(18, 0));
        brechaHorariaRepository.save(brecha);

        TipoAtencion tipo = new TipoAtencion();
        tipo.setProfesional(profesional);
        tipo.setNombre("Consulta General");
        tipo.setDescripcion("Consulta");
        tipo.setDuracionMinutos(30);
        tipo.setCapacidadSimultanea(1);
        tipo.setActivo(true);
        tipo = tipoAtencionRepository.save(tipo);

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
        turno.setTipoAtencion(tipo);
        turno.setInicioEstimado(inicio);
        turno.setFinEstimado(inicio.plus(Duration.ofMinutes(30)));
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno = turnoRepository.save(turno);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.TURNO, turno.getId(), "ASIGNADO", "test", "Turno de prueba");

        return new Datos(profesional, turno, cliente, dia);
    }

    private record Datos(Profesional profesional, Turno turno, Cliente cliente, DiaAgenda dia) {
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
