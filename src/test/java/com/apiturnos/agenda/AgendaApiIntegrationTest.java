package com.apiturnos.agenda;

import com.apiturnos.agenda.dto.*;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.CrearAgendaAnual;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
class AgendaApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    private GestorCambioEstado gestorCambioEstado;

    @Autowired
    private CrearAgendaAnual crearAgendaAnual;

    private Profesional prof1;
    private Profesional prof2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        prof1 = new Profesional();
        prof1.setNombre("Carlos");
        prof1.setApellido("Bianchi");
        prof1.setEmail("carlos." + System.nanoTime() + "@turnos.com");
        prof1.setTelefono("+549111234567");
        prof1.setEspecialidad("Director Técnico");
        prof1 = profesionalRepository.save(prof1);

        prof2 = new Profesional();
        prof2.setNombre("Marcelo");
        prof2.setApellido("Gallardo");
        prof2.setEmail("marcelo." + System.nanoTime() + "@turnos.com");
        prof2.setTelefono("+549117654321");
        prof2.setEspecialidad("Estratega");
        prof2 = profesionalRepository.save(prof2);
    }

    @Test
    @DisplayName("1. POST agendas crea meses y días con estados iniciales no nulos")
    void test1_CrearAgendaAnualExitosamente() throws Exception {
        YearMonth actual = YearMonth.now();
        CrearAgendaAnualRequestDto request = new CrearAgendaAnualRequestDto(actual.getYear());

        String agendaJson = mockMvc.perform(post("/api/profesionales/{profesionalId}/agendas", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.profesionalId").value(prof1.getId()))
                .andExpect(jsonPath("$.anio").value(actual.getYear()))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Long agendaId = objectMapper.readTree(agendaJson).get("id").asLong();
        List<MesAgenda> meses = mesAgendaRepository.findByAgendaAnualId(agendaId);
        assertThat(meses).hasSize(12);

        MesAgenda mesActual = meses.stream()
                .filter(mes -> mes.getNroMes() == actual.getMonthValue())
                .findFirst().orElseThrow();
        mockMvc.perform(get("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}",
                        prof1.getId(), mesActual.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoActual").value("ACTIVO"))
                .andExpect(jsonPath("$.dias", hasSize(actual.lengthOfMonth())))
                .andExpect(jsonPath("$.dias[*].estadoActual",
                        Matchers.everyItem(Matchers.is("ACTIVO"))));

        List<String> estadosEsperados = new ArrayList<>();
        YearMonth siguiente = actual.plusMonths(1);
        for (int numeroMes = 1; numeroMes <= 12; numeroMes++) {
            YearMonth mes = YearMonth.of(actual.getYear(), numeroMes);
            estadosEsperados.add(mes.equals(actual) || mes.equals(siguiente) ? "ACTIVO" : "INACTIVO");
        }
        mockMvc.perform(get("/api/profesionales/{profesionalId}/agendas/{anio}/meses",
                        prof1.getId(), actual.getYear()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].estadoActual",
                        Matchers.contains(estadosEsperados.toArray())));

        MesAgenda mesInactivo = meses.stream()
                .filter(mes -> "INACTIVO".equals(estadosEsperados.get(mes.getNroMes() - 1)))
                .findFirst().orElseThrow();
        mockMvc.perform(get("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}",
                        prof1.getId(), mesInactivo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoActual").value("INACTIVO"))
                .andExpect(jsonPath("$.dias[*].estadoActual",
                        Matchers.everyItem(Matchers.is("INACTIVO"))));
    }

    @Test
    @DisplayName("Inicialización atómica configura días laborables y la activación futura copia la semana actual")
    void inicializacionAtomicaYCopiaAlActivar() throws Exception {
        YearMonth actual = YearMonth.now();
        String request = """
                {
                  "repetirAlMesSiguiente": true,
                  "diasSemana": [
                    {"diaSemana":"MONDAY","brechas":[{"horaInicio":"09:00","horaFin":"13:00"}]},
                    {"diaSemana":"WEDNESDAY","brechas":[{"horaInicio":"14:00","horaFin":"18:00"}]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/profesionales/{profesionalId}/agendas/inicializacion", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completado").value(true))
                .andExpect(jsonPath("$.diasLaborablesPorSemana").value(2))
                .andExpect(jsonPath("$.mesesConfigurados", hasSize(2)))
                .andExpect(jsonPath("$.mesesConfigurados[*].estado",
                        Matchers.everyItem(Matchers.is("ACTIVO"))));

        AgendaAnual agenda = agendaAnualRepository
                .findByProfesionalIdAndAnio(prof1.getId(), actual.getYear()).orElseThrow();
        MesAgenda mesActual = mesAgendaRepository
                .findByAgendaAnualIdAndNroMes(agenda.getId(), actual.getMonthValue()).orElseThrow();
        List<DiaAgenda> diasActuales = diaAgendaRepository.findByMesAgendaId(mesActual.getId());
        for (DiaAgenda dia : diasActuales) {
            boolean laborable = dia.getFecha().getDayOfWeek() == DayOfWeek.MONDAY
                    || dia.getFecha().getDayOfWeek() == DayOfWeek.WEDNESDAY;
            assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, dia.getId()))
                    .isEqualTo(laborable ? "ACTIVO" : "INACTIVO");
        }

        MesAgenda mesInactivo = mesAgendaRepository.findByAgendaAnualId(agenda.getId()).stream()
                .filter(mes -> "INACTIVO".equals(gestorCambioEstado.obtenerNombreEstadoActual(
                        AmbitoEstado.MES_AGENDA, mes.getId())))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/activar",
                        prof1.getId(), mesInactivo.getId()))
                .andExpect(status().isOk());

        List<DiaAgenda> diasCopiados = diaAgendaRepository.findByMesAgendaId(mesInactivo.getId());
        DiaAgenda lunes = diasCopiados.stream()
                .filter(dia -> dia.getFecha().getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst().orElseThrow();
        DiaAgenda martes = diasCopiados.stream()
                .filter(dia -> dia.getFecha().getDayOfWeek() == DayOfWeek.TUESDAY)
                .findFirst().orElseThrow();
        assertThat(brechaHorariaRepository.findByDiaAgendaId(lunes.getId())).hasSize(1);
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, lunes.getId()))
                .isEqualTo("ACTIVO");
        assertThat(brechaHorariaRepository.findByDiaAgendaId(martes.getId())).isEmpty();
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, martes.getId()))
                .isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("Inicialización rechaza franjas superpuestas y revierte la agenda completa")
    void inicializacionInvalidaHaceRollback() throws Exception {
        int anioActual = YearMonth.now().getYear();
        String request = """
                {
                  "repetirAlMesSiguiente": true,
                  "diasSemana": [
                    {"diaSemana":"MONDAY","brechas":[
                      {"horaInicio":"09:00","horaFin":"13:00"},
                      {"horaInicio":"12:00","horaFin":"16:00"}
                    ]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/profesionales/{profesionalId}/agendas/inicializacion", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertThat(agendaAnualRepository.findByProfesionalIdAndAnio(prof1.getId(), anioActual))
                .isEmpty();
    }

    @Test
    @DisplayName("2. POST /api/profesionales/{profesionalId}/agendas - Validación Bean Validation de año inválido")
    void test2_CrearAgendaAnualValidacionInvalida() throws Exception {
        CrearAgendaAnualRequestDto request = new CrearAgendaAnualRequestDto(1990); // Min is 2020

        mockMvc.perform(post("/api/profesionales/{profesionalId}/agendas", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.anio").isNotEmpty());
    }

    @Test
    @DisplayName("3. POST /api/profesionales/{profesionalId}/agendas - Conflicto 409 al duplicar año en mismo profesional")
    void test3_CrearAgendaAnualDuplicadaConflicto() throws Exception {
        crearAgendaAnual.ejecutar(prof1.getId(), 2029, "admin");

        CrearAgendaAnualRequestDto request = new CrearAgendaAnualRequestDto(2029);

        mockMvc.perform(post("/api/profesionales/{profesionalId}/agendas", prof1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("4. GET /api/profesionales/{profesionalId}/agendas/{anio}/meses - Lista los 12 meses")
    void test4_Consultar12Meses() throws Exception {
        crearAgendaAnual.ejecutar(prof1.getId(), 2030, "admin");

        mockMvc.perform(get("/api/profesionales/{profesionalId}/agendas/{anio}/meses", prof1.getId(), 2030))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].nroMes").value(1))
                .andExpect(jsonPath("$[0].nombreMes").isNotEmpty())
                .andExpect(jsonPath("$[11].nroMes").value(12));
    }

    @Test
    @DisplayName("5. GET /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId} - Obtiene detalle de MesAgenda")
    void test5_ObtenerDetalleMesAgenda() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2031, "admin");
        MesAgenda mes1 = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 1).orElseThrow();

        mockMvc.perform(get("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}", prof1.getId(), mes1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mes1.getId()))
                .andExpect(jsonPath("$.nroMes").value(1))
                .andExpect(jsonPath("$.nombreMes").isNotEmpty())
                .andExpect(jsonPath("$.repetirConfiguracion").value(false));
    }

    @Test
    @DisplayName("6. GET /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId} - 403 si pertenece a otro profesional")
    void test6_DetalleMesAgendaValidaProfesional() throws Exception {
        AgendaAnual agendaProf1 = crearAgendaAnual.ejecutar(prof1.getId(), 2032, "admin");
        MesAgenda mes1 = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agendaProf1.getId(), 1).orElseThrow();

        // Intento de acceso desde prof2
        mockMvc.perform(get("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}", prof2.getId(), mes1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("7. POST /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/modo-semana - Configura modo SEMANA")
    void test7_ConfigurarModoSemana() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2033, "admin");
        MesAgenda mes3 = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 3).orElseThrow(); // Marzo 2033

        // Plantilla: Lunes 09:00-13:00, Miércoles 14:00-18:00
        ConfigurarModoSemanaRequestDto request = new ConfigurarModoSemanaRequestDto(List.of(
                new DiaSemanaConfiguracionDto(DayOfWeek.MONDAY, List.of(
                        new BrechaHorariaRequestDto(LocalTime.of(9, 0), LocalTime.of(13, 0))
                )),
                new DiaSemanaConfiguracionDto(DayOfWeek.WEDNESDAY, List.of(
                        new BrechaHorariaRequestDto(LocalTime.of(14, 0), LocalTime.of(18, 0))
                ))
        ));

        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/modo-semana", prof1.getId(), mes3.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mes3.getId()))
                .andExpect(jsonPath("$.dias", hasSize(31))); // Marzo tiene 31 días
    }

    @Test
    @DisplayName("8. POST /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/modo-mes - Configura modo MES")
    void test8_ConfigurarModoMes() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2034, "admin");
        MesAgenda mes5 = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 5).orElseThrow(); // Mayo 2034

        ConfigurarModoMesRequestDto request = new ConfigurarModoMesRequestDto(List.of(
                new DiaMesConfiguracionDto(LocalDate.of(2034, 5, 10), List.of(
                        new BrechaHorariaRequestDto(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new BrechaHorariaRequestDto(LocalTime.of(16, 0), LocalTime.of(20, 0))
                ))
        ));

        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/modo-mes", prof1.getId(), mes5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mes5.getId()));
    }

    @Test
    @DisplayName("9. PUT /api/profesionales/{profesionalId}/dias-agenda/{diaAgendaId}/brechas - Configura brechas de un día")
    void test9_ConfigurarBrechasDia() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2035, "admin");
        MesAgenda mes = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 6).orElseThrow();

        // Generar días
        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/dias", prof1.getId(), mes.getId()))
                .andExpect(status().isOk());

        DiaAgenda dia = diaAgendaRepository.findByMesAgendaIdAndFecha(mes.getId(), LocalDate.of(2035, 6, 15)).orElseThrow();

        ConfigurarDiaRequestDto request = new ConfigurarDiaRequestDto(List.of(
                new BrechaHorariaRequestDto(LocalTime.of(10, 0), LocalTime.of(14, 0))
        ));

        mockMvc.perform(put("/api/profesionales/{profesionalId}/dias-agenda/{diaAgendaId}/brechas", prof1.getId(), dia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dia.getId()))
                .andExpect(jsonPath("$.brechas", hasSize(1)))
                .andExpect(jsonPath("$.brechas[0].horaInicio").value("10:00"))
                .andExpect(jsonPath("$.brechas[0].horaFin").value("14:00"));
    }

    @Test
    @DisplayName("10. GET /api/profesionales/{profesionalId}/dias-agenda/{diaAgendaId} - Detalle de DiaAgenda y brechas")
    void test10_ConsultarDetalleDiaAgenda() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2036, "admin");
        MesAgenda mes = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 7).orElseThrow();

        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/dias", prof1.getId(), mes.getId()))
                .andExpect(status().isOk());

        DiaAgenda dia = diaAgendaRepository.findByMesAgendaIdAndFecha(mes.getId(), LocalDate.of(2036, 7, 20)).orElseThrow();

        mockMvc.perform(get("/api/profesionales/{profesionalId}/dias-agenda/{diaAgendaId}", prof1.getId(), dia.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dia.getId()))
                .andExpect(jsonPath("$.fecha").value("2036-07-20"))
                .andExpect(jsonPath("$.nombreDiaSemana").isNotEmpty());
    }

    @Test
    @DisplayName("11. POST /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/activar y /inactivar")
    void test11_ActivarEInactivarMes() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2037, "admin");
        MesAgenda mes = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 8).orElseThrow();

        // Activar
        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/activar", prof1.getId(), mes.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}", prof1.getId(), mes.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoActual").value("ACTIVO"));

        // Inactivar
        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/inactivar", prof1.getId(), mes.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}", prof1.getId(), mes.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoActual").value("INACTIVO"));
    }

    @Test
    @DisplayName("12. PUT /repetir-configuracion y POST /repetir - Repite configuración al mes siguiente")
    void test12_RepetirConfiguracionMesSiguiente() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2038, "admin");
        MesAgenda mes1 = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 1).orElseThrow();

        // Configurar mes 1 en modo semana
        ConfigurarModoSemanaRequestDto modoSemana = new ConfigurarModoSemanaRequestDto(List.of(
                new DiaSemanaConfiguracionDto(DayOfWeek.MONDAY, List.of(
                        new BrechaHorariaRequestDto(LocalTime.of(8, 0), LocalTime.of(12, 0))
                ))
        ));

        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/modo-semana", prof1.getId(), mes1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modoSemana)))
                .andExpect(status().isOk());

        // Activar bandera repetirConfiguracion = true
        ActualizarRepetirConfiguracionRequestDto reqRepetir = new ActualizarRepetirConfiguracionRequestDto(true);
        mockMvc.perform(put("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/repetir-configuracion", prof1.getId(), mes1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqRepetir)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetirConfiguracion").value(true));

        // Ejecutar repetición
        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/repetir", prof1.getId(), mes1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nroMes").value(2));
    }

    @Test
    @DisplayName("13. Validación de horaInicio >= horaFin devuelve 400 BAD REQUEST con mensaje de error claro")
    void test13_ValidacionHorariosInvalidosDevuelve400() throws Exception {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(prof1.getId(), 2039, "admin");
        MesAgenda mes = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), 4).orElseThrow();

        mockMvc.perform(post("/api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/dias", prof1.getId(), mes.getId()))
                .andExpect(status().isOk());

        DiaAgenda dia = diaAgendaRepository.findByMesAgendaIdAndFecha(mes.getId(), LocalDate.of(2039, 4, 10)).orElseThrow();

        // horaInicio (15:00) >= horaFin (10:00)
        ConfigurarDiaRequestDto requestInvalido = new ConfigurarDiaRequestDto(List.of(
                new BrechaHorariaRequestDto(LocalTime.of(15, 0), LocalTime.of(10, 0))
        ));

        mockMvc.perform(put("/api/profesionales/{profesionalId}/dias-agenda/{diaAgendaId}/brechas", prof1.getId(), dia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", Matchers.containsString("debe ser anterior")));
    }
}

