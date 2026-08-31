package com.apiturnos.turno;

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
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.turno.dto.CrearTurnoManualRequestDto;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "turnos.zona-horaria=UTC")
@Testcontainers
class TurnoManualApiIntegrationTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 9, 10);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired private ProfesionalRepository profesionalRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private AgendaAnualRepository agendaAnualRepository;
    @Autowired private MesAgendaRepository mesAgendaRepository;
    @Autowired private DiaAgendaRepository diaAgendaRepository;
    @Autowired private BrechaHorariaRepository brechaHorariaRepository;
    @Autowired private ExcepcionAgendaRepository excepcionAgendaRepository;
    @Autowired private TipoAtencionRepository tipoAtencionRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private TurnoHistorialRepository turnoHistorialRepository;
    @Autowired private MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    @Autowired private CambioEstadoRepository cambioEstadoRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private AuditoriaEventoRepository auditoriaEventoRepository;
    @Autowired private GestorCambioEstado gestorCambioEstado;

    private Profesional prof1;
    private Profesional prof2;
    private Cliente cliente1;
    private Cliente cliente2;
    private DiaAgenda dia1;
    private TipoAtencion tipoConsulta;
    private TipoAtencion tipoControlInactivo;
    private TipoAtencion tipoProf2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        notificacionRepository.deleteAll();
        auditoriaEventoRepository.deleteAll();
        cambioEstadoRepository.deleteAll();
        turnoHistorialRepository.deleteAll();
        turnoRepository.deleteAll();
        motivoBajaTurnoRepository.deleteAll();
        excepcionAgendaRepository.deleteAll();
        brechaHorariaRepository.deleteAll();
        tipoAtencionRepository.deleteAll();
        diaAgendaRepository.deleteAll();
        mesAgendaRepository.deleteAll();
        agendaAnualRepository.deleteAll();
        configuracionRepository.deleteAll();
        clienteRepository.deleteAll();
        profesionalRepository.deleteAll();

        // Profesional 1
        prof1 = new Profesional();
        prof1.setNombre("Dr. Martin");
        prof1.setApellido("Palermo");
        prof1.setEmail("palermo." + System.nanoTime() + "@test.com");
        prof1.setTelefono("+5491111111111");
        prof1.setEspecialidad("Traumatología");
        prof1 = profesionalRepository.save(prof1);

        // Profesional 2
        prof2 = new Profesional();
        prof2.setNombre("Dr. Guillermo");
        prof2.setApellido("Barros Schelotto");
        prof2.setEmail("guillermo." + System.nanoTime() + "@test.com");
        prof2.setTelefono("+5491122222222");
        prof2.setEspecialidad("Cardiología");
        prof2 = profesionalRepository.save(prof2);

        // Agenda prof1
        AgendaAnual agenda1 = new AgendaAnual();
        agenda1.setProfesional(prof1);
        agenda1.setAnio(FECHA.getYear());
        agenda1 = agendaAnualRepository.save(agenda1);

        MesAgenda mes1 = new MesAgenda();
        mes1.setAgendaAnual(agenda1);
        mes1.setNroMes(FECHA.getMonthValue());
        mes1 = mesAgendaRepository.save(mes1);

        dia1 = new DiaAgenda();
        dia1.setMesAgenda(mes1);
        dia1.setFecha(FECHA);
        dia1 = diaAgendaRepository.save(dia1);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA, dia1.getId(), "ACTIVO", "setup", "Día activo");

        // Brecha 08:00 - 12:00
        BrechaHoraria brecha1 = new BrechaHoraria();
        brecha1.setDiaAgenda(dia1);
        brecha1.setHoraInicioAtencion(LocalTime.of(8, 0));
        brecha1.setHoraFinAtencion(LocalTime.of(12, 0));
        brechaHorariaRepository.save(brecha1);

        // Clientes
        cliente1 = new Cliente();
        cliente1.setProfesional(prof1);
        cliente1.setNombre("Juan");
        cliente1.setApellido("Perez");
        cliente1.setTipoDocumento(TipoDocumento.DNI);
        cliente1.setNumeroDocumento("30111222");
        cliente1.setEmail("juan.perez@test.com");
        cliente1.setTelefono("+5491112345678");
        cliente1.setNotificacionesHabilitadas(true);
        cliente1 = clienteRepository.save(cliente1);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.CLIENTE, cliente1.getId(), "HABILITADO", "setup", "Cliente habilitado");

        cliente2 = new Cliente();
        cliente2.setProfesional(prof2);
        cliente2.setNombre("Maria");
        cliente2.setApellido("Gomez");
        cliente2.setTipoDocumento(TipoDocumento.DNI);
        cliente2.setNumeroDocumento("35999888");
        cliente2.setEmail("maria.gomez@test.com");
        cliente2.setTelefono("+5491187654321");
        cliente2.setNotificacionesHabilitadas(true);
        cliente2 = clienteRepository.save(cliente2);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.CLIENTE, cliente2.getId(), "HABILITADO", "setup", "Cliente habilitado");

        // Tipos de Atención
        tipoConsulta = new TipoAtencion();
        tipoConsulta.setProfesional(prof1);
        tipoConsulta.setNombre("Consulta General");
        tipoConsulta.setDescripcion("Atención general de 30 min");
        tipoConsulta.setDuracionMinutos(30);
        tipoConsulta.setCapacidadSimultanea(1);
        tipoConsulta.setActivo(true);
        tipoConsulta = tipoAtencionRepository.save(tipoConsulta);

        tipoControlInactivo = new TipoAtencion();
        tipoControlInactivo.setProfesional(prof1);
        tipoControlInactivo.setNombre("Control Inactivo");
        tipoControlInactivo.setDescripcion("Tipo inactivo");
        tipoControlInactivo.setDuracionMinutos(15);
        tipoControlInactivo.setCapacidadSimultanea(1);
        tipoControlInactivo.setActivo(false);
        tipoControlInactivo = tipoAtencionRepository.save(tipoControlInactivo);

        tipoProf2 = new TipoAtencion();
        tipoProf2.setProfesional(prof2);
        tipoProf2.setNombre("Consulta Cardiológica");
        tipoProf2.setDescripcion("Cardiología");
        tipoProf2.setDuracionMinutos(45);
        tipoProf2.setCapacidadSimultanea(1);
        tipoProf2.setActivo(true);
        tipoProf2 = tipoAtencionRepository.save(tipoProf2);
    }

    @Test
    @DisplayName("1. Búsqueda/selección de Cliente funciona dentro del Profesional")
    void test01_BusquedaYSeleccionClienteDentroDeProfesional() throws Exception {
        // Búsqueda por DNI en prof1
        mockMvc.perform(get("/api/profesionales/{profesionalId}/clientes", prof1.getId())
                        .param("dni", "30111222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(cliente1.getId()))
                .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
                .andExpect(jsonPath("$.content[0].apellido").value("Perez"))
                .andExpect(jsonPath("$.content[0].numeroDocumento").value("30111222"));

        // Búsqueda por apellido parcial
        mockMvc.perform(get("/api/profesionales/{profesionalId}/clientes", prof1.getId())
                        .param("apellido", "per"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(cliente1.getId()));

        // Búsqueda por nombre parcial
        mockMvc.perform(get("/api/profesionales/{profesionalId}/clientes", prof1.getId())
                        .param("nombre", "jua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(cliente1.getId()));

        // Cliente de prof2 no aparece en prof1
        mockMvc.perform(get("/api/profesionales/{profesionalId}/clientes", prof1.getId())
                        .param("dni", "35999888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // Consulta de detalle por ID
        mockMvc.perform(get("/api/profesionales/{profesionalId}/clientes/{clienteId}", prof1.getId(), cliente1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cliente1.getId()))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.apellido").value("Perez"))
                .andExpect(jsonPath("$.numeroDocumento").value("30111222"));
    }

    @Test
    @DisplayName("2. Lista de TipoAtencion devuelve solo activos del Profesional")
    void test02_ListaTipoAtencionDevuelveSoloActivosDelProfesional() throws Exception {
        mockMvc.perform(get("/api/profesionales/{profesionalId}/tipos-atencion", prof1.getId())
                        .param("soloActivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(tipoConsulta.getId()))
                .andExpect(jsonPath("$[0].nombre").value("Consulta General"))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                .andExpect(jsonPath("$[0].capacidadSimultanea").value(1))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    @DisplayName("3. Días INACTIVOS no son seleccionables")
    void test03_DiasInactivosNoSonSeleccionables() throws Exception {
        DiaAgenda diaInactivo = new DiaAgenda();
        diaInactivo.setMesAgenda(dia1.getMesAgenda());
        diaInactivo.setFecha(FECHA.plusDays(1));
        diaInactivo = diaAgendaRepository.save(diaInactivo);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA, diaInactivo.getId(), "INACTIVO", "test", "Día inactivo");

        // En consulta de días seleccionables
        mockMvc.perform(get("/api/profesionales/{profesionalId}/dias-agenda/seleccionables", prof1.getId())
                        .param("desde", FECHA.toString())
                        .param("hasta", FECHA.plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.diaAgendaId == " + diaInactivo.getId() + ")].seleccionable").value(false))
                .andExpect(jsonPath("$[?(@.diaAgendaId == " + diaInactivo.getId() + ")].mensaje").value("Día inactivo"));

        // Al intentar crear turno en día inactivo
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                diaInactivo.getId(),
                tipoConsulta.getId(),
                FECHA.plusDays(1).atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.plusDays(1).atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Intento en día inactivo");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("inactivo")));
    }

    @Test
    @DisplayName("4. Días FINALIZADOS no son seleccionables")
    void test04_DiasFinalizadosNoSonSeleccionables() throws Exception {
        DiaAgenda diaFinalizado = new DiaAgenda();
        diaFinalizado.setMesAgenda(dia1.getMesAgenda());
        diaFinalizado.setFecha(FECHA.plusDays(2));
        diaFinalizado = diaAgendaRepository.save(diaFinalizado);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA, diaFinalizado.getId(), "FINALIZADO", "test", "Día finalizado");

        // En consulta de días seleccionables
        mockMvc.perform(get("/api/profesionales/{profesionalId}/dias-agenda/seleccionables", prof1.getId())
                        .param("desde", FECHA.toString())
                        .param("hasta", FECHA.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.diaAgendaId == " + diaFinalizado.getId() + ")].seleccionable").value(false))
                .andExpect(jsonPath("$[?(@.diaAgendaId == " + diaFinalizado.getId() + ")].mensaje").value("Día finalizado"));

        // Al intentar crear turno en día finalizado
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                diaFinalizado.getId(),
                tipoConsulta.getId(),
                FECHA.plusDays(2).atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.plusDays(2).atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Intento en día finalizado");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("finalizado")));
    }

    @Test
    @DisplayName("5. Horarios sugeridos respetan duración")
    void test05_HorariosSugeridosRespetanDuracion() throws Exception {
        // Brecha configurada: 08:00 a 12:00 (4 horas = 8 slots de 30 min)
        mockMvc.perform(get("/api/profesionales/{profesionalId}/turnos/horarios-sugeridos", prof1.getId())
                        .param("tipoAtencionId", tipoConsulta.getId().toString())
                        .param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].horaInicio").value("08:00:00"))
                .andExpect(jsonPath("$[0].horaFin").value("08:30:00"))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                .andExpect(jsonPath("$[7].horaInicio").value("11:30:00"))
                .andExpect(jsonPath("$[7].horaFin").value("12:00:00"));
    }

    @Test
    @DisplayName("6. Creación dentro de brecha funciona")
    void test06_CreacionDentroDeBrechaFunciona() throws Exception {
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Turno dentro de brecha");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creado").value(true))
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.requiereConfirmacion").value(false))
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andExpect(jsonPath("$.advertencias", hasSize(0)))
                .andExpect(jsonPath("$.turnoId").isNumber());

        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("7. Fuera de brecha devuelve advertencia")
    void test07_FueraDeBrechaDevuelveAdvertencia() throws Exception {
        // Horario 14:00 - 14:30 (la brecha es 08:00 - 12:00)
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(14, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(14, 30).toInstant(ZoneOffset.UTC),
                false,
                "Turno fuera de brecha");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creado").value(false))
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.requiereConfirmacion").value(true))
                .andExpect(jsonPath("$.advertencias", containsInAnyOrder("HORARIO_FUERA_DE_BRECHA")))
                .andExpect(jsonPath("$.turnoId").value(nullValue()));
    }

    @Test
    @DisplayName("8. Fuera de brecha sin confirmar no crea")
    void test08_FueraDeBrechaSinConfirmarNoCrea() throws Exception {
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(14, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(14, 30).toInstant(ZoneOffset.UTC),
                false,
                "Turno fuera de brecha sin confirmar");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creado").value(false));

        assertThat(turnoRepository.count()).isZero();
    }

    @Test
    @DisplayName("9. Fuera de brecha confirmado crea")
    void test09_FueraDeBrechaConfirmadoCrea() throws Exception {
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(14, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(14, 30).toInstant(ZoneOffset.UTC),
                true, // Confirmado explícitamente
                "Turno fuera de brecha confirmado");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creado").value(true))
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andExpect(jsonPath("$.advertencias", containsInAnyOrder("HORARIO_FUERA_DE_BRECHA")))
                .andExpect(jsonPath("$.turnoId").isNumber());

        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("10. Sobrecapacidad devuelve advertencia")
    void test10_SobrecapacidadDevuelveAdvertencia() throws Exception {
        // Crear primer turno dentro de capacidad
        CrearTurnoManualRequestDto request1 = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Turno 1");
        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Intentar crear segundo turno simultáneo (capacidad = 1) sin confirmar
        CrearTurnoManualRequestDto request2 = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Turno 2 sin confirmar");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creado").value(false))
                .andExpect(jsonPath("$.requiereConfirmacion").value(true))
                .andExpect(jsonPath("$.advertencias", containsInAnyOrder("CAPACIDAD_SUPERADA")));

        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("11. Sobrecapacidad confirmada crea")
    void test11_SobrecapacidadConfirmadaCrea() throws Exception {
        // Crear primer turno
        CrearTurnoManualRequestDto request1 = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Turno 1");
        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Segundo turno confirmado
        CrearTurnoManualRequestDto request2 = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                true, // Confirmado
                "Turno 2 sobrecapacidad confirmado");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creado").value(true))
                .andExpect(jsonPath("$.advertencias", containsInAnyOrder("CAPACIDAD_SUPERADA")));

        assertThat(turnoRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("12. Múltiples advertencias se devuelven juntas")
    void test12_MultiplesAdvertenciasSeDevuelvenJuntas() throws Exception {
        // Primer turno a las 14:00 (fuera de brecha) confirmado
        CrearTurnoManualRequestDto primerFueraBrecha = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(14, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(14, 30).toInstant(ZoneOffset.UTC),
                true,
                "Primer turno 14:00");
        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primerFueraBrecha)))
                .andExpect(status().isCreated());

        // Segundo turno a las 14:00 -> fuera de brecha Y sobrecapacidad
        CrearTurnoManualRequestDto requestDobleAdvertencia = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(14, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(14, 30).toInstant(ZoneOffset.UTC),
                false,
                "Segundo turno 14:00 sin confirmar");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDobleAdvertencia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creado").value(false))
                .andExpect(jsonPath("$.requiereConfirmacion").value(true))
                .andExpect(jsonPath("$.advertencias", containsInAnyOrder("HORARIO_FUERA_DE_BRECHA", "CAPACIDAD_SUPERADA")));
    }

    @Test
    @DisplayName("13. Cliente de otro Profesional produce error")
    void test13_ClienteDeOtroProfesionalProduceError() throws Exception {
        // Intentar crear turno para cliente2 (de prof2) en prof1
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente2.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Cliente ajeno");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message", Matchers.containsString("no pertenece")));
    }

    @Test
    @DisplayName("14. TipoAtencion de otro Profesional produce error")
    void test14_TipoAtencionDeOtroProfesionalProduceError() throws Exception {
        // Intentar crear turno con tipoProf2 (de prof2) en prof1
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoProf2.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Tipo ajeno");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message", Matchers.containsString("no pertenece")));
    }

    @Test
    @DisplayName("15. Vacaciones bloquean creación")
    void test15_VacacionesBloqueanCreacion() throws Exception {
        ExcepcionAgenda vacaciones = new ExcepcionAgenda();
        vacaciones.setProfesional(prof1);
        vacaciones.setTipo(TipoExcepcion.VACACIONES);
        vacaciones.setFechaInicio(FECHA);
        vacaciones.setFechaFin(FECHA);
        vacaciones.setMotivo("Vacaciones de invierno");
        vacaciones.setActiva(true);
        excepcionAgendaRepository.save(vacaciones);

        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                true, // Aún confirmado debe bloquearse
                "Intento en vacaciones");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", Matchers.containsString("excepción de agenda")));
    }

    @Test
    @DisplayName("16. Bloqueo excepcional bloquea franja")
    void test16_BloqueoExcepcionalBloqueaFranja() throws Exception {
        ExcepcionAgenda bloqueo = new ExcepcionAgenda();
        bloqueo.setProfesional(prof1);
        bloqueo.setTipo(TipoExcepcion.BLOQUEO_HORARIO);
        bloqueo.setFechaInicio(FECHA);
        bloqueo.setFechaFin(FECHA);
        bloqueo.setHoraInicio(LocalTime.of(10, 0));
        bloqueo.setHoraFin(LocalTime.of(11, 0));
        bloqueo.setMotivo("Reunión directiva");
        bloqueo.setActiva(true);
        excepcionAgendaRepository.save(bloqueo);

        // Turno que intersecta la franja bloqueada 10:15 - 10:45
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(10, 15).toInstant(ZoneOffset.UTC),
                FECHA.atTime(10, 45).toInstant(ZoneOffset.UTC),
                true,
                "Intento en horario bloqueado");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", Matchers.containsString("bloqueo explícito")));
    }

    @Test
    @DisplayName("17. Confirmación devuelve nombre, apellido y DNI")
    void test17_ConfirmacionDevuelveNombreApellidoYDni() throws Exception {
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(14, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(14, 30).toInstant(ZoneOffset.UTC),
                false,
                "Consulta confirmación");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nombre").value("Juan"))
                .andExpect(jsonPath("$.cliente.apellido").value("Perez"))
                .andExpect(jsonPath("$.cliente.numeroDocumento").value("30111222"))
                .andExpect(jsonPath("$.datosConfirmacion.nombreCliente").value("Juan"))
                .andExpect(jsonPath("$.datosConfirmacion.apellidoCliente").value("Perez"))
                .andExpect(jsonPath("$.datosConfirmacion.numeroDocumento").value("30111222"))
                .andExpect(jsonPath("$.datosConfirmacion.horaInicio").value("14:00:00"))
                .andExpect(jsonPath("$.datosConfirmacion.horaFin").value("14:30:00"));
    }

    @Test
    @DisplayName("18. Turno exitoso queda ASIGNADO")
    void test18_TurnoExitosoQuedaAsignado() throws Exception {
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Turno asignado");

        String responseJson = mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andReturn().getResponse().getContentAsString();

        Long turnoId = objectMapper.readTree(responseJson).get("turnoId").asLong();

        String estadoTurno = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turnoId);
        assertThat(estadoTurno).isEqualTo("ASIGNADO");
    }

    @Test
    @DisplayName("19. Respuesta nunca expone entidades JPA internas")
    void test19_RespuestaNuncaExponeEntidadesJpaInternas() throws Exception {
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "DTO validation");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist())
                .andExpect(jsonPath("$.diaAgenda").doesNotExist())
                .andExpect(jsonPath("$.profesional").doesNotExist())
                .andExpect(jsonPath("$.cliente.profesional").doesNotExist());
    }

    @Test
    @DisplayName("20. Validaciones Bean Validation funcionan")
    void test20_ValidacionesBeanValidationFuncionan() throws Exception {
        // Request con campos obligatorios nulos
        CrearTurnoManualRequestDto requestInvalido = new CrearTurnoManualRequestDto();

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.clienteId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.tipoAtencionId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.inicioEstimado").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.finEstimado").isNotEmpty());
    }

    @Test
    @DisplayName("21. Validación previa mediante /turnos/validar no persiste y devuelve estructura completa")
    void test21_ValidacionPreviaEndpointDevuelveEstructuraEsperada() throws Exception {
        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                cliente1.getId(),
                dia1.getId(),
                tipoConsulta.getId(),
                FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC),
                FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC),
                false,
                "Prevalidación");

        mockMvc.perform(post("/api/profesionales/{profesionalId}/turnos/validar", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creado").value(false))
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.requiereConfirmacion").value(false))
                .andExpect(jsonPath("$.cliente.nombre").value("Juan"))
                .andExpect(jsonPath("$.cliente.numeroDocumento").value("30111222"))
                .andExpect(jsonPath("$.tipoAtencion.nombre").value("Consulta General"))
                .andExpect(jsonPath("$.turnoId").value(nullValue()));

        assertThat(turnoRepository.count()).isZero();
    }
}

