package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.CrearAgendaAnualRequestDto;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.CrearAgendaAnual;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.AgendaAnualDuplicadaException;
import com.apiturnos.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgendaAnualControllerUnitTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private CrearAgendaAnual crearAgendaAnual;

    @Mock
    private AgendaAnualRepository agendaAnualRepository;

    @Mock
    private MesAgendaRepository mesAgendaRepository;

    @Mock
    private GestorCambioEstado gestorCambioEstado;

    @InjectMocks
    private AgendaAnualController agendaAnualController;

    private Profesional profesional;
    private AgendaAnual agenda;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(agendaAnualController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Dr. Favaloro");

        agenda = new AgendaAnual();
        agenda.setId(10L);
        agenda.setProfesional(profesional);
        agenda.setAnio(2027);
        agenda.setFechaCreacion(Instant.now());
    }

    @Test
    @DisplayName("POST /api/profesionales/{profesionalId}/agendas - Crea agenda anual retornando 201")
    void testCrearAgendaAnual201() throws Exception {
        when(crearAgendaAnual.ejecutar(eq(1L), eq(2027), any())).thenReturn(agenda);

        CrearAgendaAnualRequestDto request = new CrearAgendaAnualRequestDto(2027);

        mockMvc.perform(post("/api/profesionales/1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.anio").value(2027))
                .andExpect(jsonPath("$.profesionalId").value(1));
    }

    @Test
    @DisplayName("POST /api/profesionales/{profesionalId}/agendas - Conflicto 409 cuando ya existe año")
    void testCrearAgendaAnualDuplicada409() throws Exception {
        when(crearAgendaAnual.ejecutar(eq(1L), eq(2027), any()))
                .thenThrow(new AgendaAnualDuplicadaException(1L, 2027));

        CrearAgendaAnualRequestDto request = new CrearAgendaAnualRequestDto(2027);

        mockMvc.perform(post("/api/profesionales/1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/profesionales/{profesionalId}/agendas/{anio} - 404 cuando no existe")
    void testObtenerAgendaAnualNoEncontrada404() throws Exception {
        when(agendaAnualRepository.findByProfesionalIdAndAnio(1L, 2099))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/profesionales/1/agendas/2099"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/profesionales/{profesionalId}/agendas/{anio}/meses - Lista los 12 meses")
    void testListarMeses200() throws Exception {
        MesAgenda mes = new MesAgenda();
        mes.setId(101L);
        mes.setAgendaAnual(agenda);
        mes.setNroMes(1);
        mes.setRepetirConfiguracion(false);

        when(agendaAnualRepository.findByProfesionalIdAndAnio(1L, 2027))
                .thenReturn(Optional.of(agenda));
        when(mesAgendaRepository.findByAgendaAnualId(10L))
                .thenReturn(List.of(mes));
        when(gestorCambioEstado.obtenerEstadosActualesPorEntidades(AmbitoEstado.MES_AGENDA, List.of(101L)))
                .thenReturn(Map.of(101L, "ACTIVO"));

        mockMvc.perform(get("/api/profesionales/1/agendas/2027/meses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].nroMes").value(1))
                .andExpect(jsonPath("$[0].estadoActual").value("ACTIVO"));
    }
}
