package com.apiturnos.profesional.controller;

import com.apiturnos.profesional.dto.ConfiguracionRequestDto;
import com.apiturnos.profesional.dto.ProfesionalRequestDto;
import com.apiturnos.profesional.dto.ProfesionalResponseDto;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.service.EditarProfesional;
import com.apiturnos.profesional.service.EliminarProfesional;
import com.apiturnos.profesional.service.ListarProfesionales;
import com.apiturnos.profesional.service.ModificarConfiguracionProfesional;
import com.apiturnos.profesional.service.ObtenerConfiguracionProfesional;
import com.apiturnos.profesional.service.ObtenerProfesional;
import com.apiturnos.profesional.service.RegistrarConfiguracionProfesional;
import com.apiturnos.profesional.service.RegistrarProfesional;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.GlobalExceptionHandler;
import com.apiturnos.shared.exception.ProfesionalDuplicadoException;
import com.apiturnos.shared.exception.ProfesionalConDependenciasException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProfesionalControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RegistrarProfesional registrarProfesional;

    @Mock
    private ObtenerProfesional obtenerProfesional;

    @Mock
    private ListarProfesionales listarProfesionales;

    @Mock
    private EditarProfesional editarProfesional;

    @Mock
    private EliminarProfesional eliminarProfesional;

    @Mock
    private ObtenerConfiguracionProfesional obtenerConfiguracionProfesional;

    @Mock
    private ModificarConfiguracionProfesional modificarConfiguracionProfesional;

    @Mock
    private RegistrarConfiguracionProfesional registrarConfiguracionProfesional;

    @InjectMocks
    private ProfesionalController controller;

    private Profesional profesional;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Gómez");
        profesional.setEmail("carlos.gomez@test.com");
        profesional.setTelefono("+5491100001111");
        profesional.setEspecialidad("Odontología");
    }

    @Test
    @DisplayName("POST /api/profesionales con datos válidos -> 201 Created")
    void registrar_valido_retorna201() throws Exception {
        ProfesionalRequestDto request = new ProfesionalRequestDto(
                "Carlos", "Gómez", "carlos.gomez@test.com", "+5491100001111", "Odontología");

        when(registrarProfesional.ejecutar(
                eq("Carlos"), eq("Gómez"), eq("carlos.gomez@test.com"),
                eq("+5491100001111"), eq("Odontología"), any()))
                .thenReturn(profesional);

        mockMvc.perform(post("/api/profesionales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.apellido").value("Gómez"))
                .andExpect(jsonPath("$.email").value("carlos.gomez@test.com"));
    }

    @Test
    @DisplayName("POST /api/profesionales con datos inválidos -> 400 Bad Request")
    void registrar_invalido_retorna400() throws Exception {
        ProfesionalRequestDto request = new ProfesionalRequestDto("", "", "invalido", "", null);

        mockMvc.perform(post("/api/profesionales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/profesionales con JSON ilegible -> 400 Bad Request")
    void registrar_jsonIlegible_retorna400() throws Exception {
        mockMvc.perform(post("/api/profesionales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"incompleto\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El cuerpo de la solicitud no contiene un JSON válido"));
    }

    @Test
    @DisplayName("POST /api/profesionales con email duplicado -> 409 Conflict")
    void registrar_duplicado_retorna409() throws Exception {
        ProfesionalRequestDto request = new ProfesionalRequestDto(
                "Carlos", "Gómez", "carlos.gomez@test.com", "+5491100001111", "Odontología");

        when(registrarProfesional.ejecutar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ProfesionalDuplicadoException("carlos.gomez@test.com"));

        mockMvc.perform(post("/api/profesionales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/profesionales/1 -> 200 OK")
    void obtenerPorId_retorna200() throws Exception {
        when(obtenerProfesional.ejecutar(1L)).thenReturn(profesional);

        mockMvc.perform(get("/api/profesionales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos"));
    }

    @Test
    @DisplayName("GET /api/profesionales/99 no existente -> 404 Not Found")
    void obtenerPorId_noExiste_retorna404() throws Exception {
        when(obtenerProfesional.ejecutar(99L))
                .thenThrow(new EntidadNoEncontradaException("Profesional", 99L));

        mockMvc.perform(get("/api/profesionales/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/profesionales -> 200 OK")
    void listar_retorna200() throws Exception {
        when(listarProfesionales.ejecutar()).thenReturn(List.of(new ProfesionalResponseDto(profesional)));

        mockMvc.perform(get("/api/profesionales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Carlos"));
    }

    @Test
    @DisplayName("GET /api/profesionales/buscar conserva filtros y paginación")
    void buscar_retornaPagina() throws Exception {
        PageImpl<ProfesionalResponseDto> page = new PageImpl<>(
                List.of(new ProfesionalResponseDto(profesional)), PageRequest.of(0, 20), 1);
        when(listarProfesionales.ejecutar(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/profesionales/buscar")
                        .param("busqueda", "Carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @DisplayName("PUT /api/profesionales/1 -> 200 OK")
    void editar_retorna200() throws Exception {
        ProfesionalRequestDto request = new ProfesionalRequestDto(
                "Carlos", "Gómez", "carlos.nuevo@test.com", "+5491100001111", "Ortodoncia");

        profesional.setEmail("carlos.nuevo@test.com");
        profesional.setEspecialidad("Ortodoncia");

        when(editarProfesional.ejecutar(eq(1L), eq("Carlos"), eq("Gómez"),
                eq("carlos.nuevo@test.com"), eq("+5491100001111"), eq("Ortodoncia"), any()))
                .thenReturn(profesional);

        mockMvc.perform(put("/api/profesionales/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("carlos.nuevo@test.com"))
                .andExpect(jsonPath("$.especialidad").value("Ortodoncia"));
    }

    @Test
    @DisplayName("DELETE /api/profesionales/1 -> 204 No Content")
    void eliminar_retorna204() throws Exception {
        doNothing().when(eliminarProfesional).ejecutar(eq(1L), any());

        mockMvc.perform(delete("/api/profesionales/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/profesionales/1 con relaciones -> 409 Conflict")
    void eliminar_conDependencias_retorna409() throws Exception {
        doThrow(new ProfesionalConDependenciasException(1L, new RuntimeException("restricción FK")))
                .when(eliminarProfesional).ejecutar(eq(1L), any());

        mockMvc.perform(delete("/api/profesionales/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar el profesional con id 1 porque tiene información asociada"));
    }

    @Test
    @DisplayName("GET /api/profesionales/1/configuracion -> 200 OK")
    void obtenerConfiguracion_retorna200() throws Exception {
        Configuracion config = new Configuracion();
        config.setId(10L);
        config.setProfesional(profesional);
        config.setCantidadMaxTurnosALaVez(2);
        config.setDuracionAproximadaPorTurno(40);
        config.setUmbralCancelacionHoras(12);

        when(obtenerConfiguracionProfesional.ejecutar(1L)).thenReturn(config);

        mockMvc.perform(get("/api/profesionales/1/configuracion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.profesionalId").value(1))
                .andExpect(jsonPath("$.cantidadMaxTurnosALaVez").value(2))
                .andExpect(jsonPath("$.duracionAproximadaPorTurno").value(40))
                .andExpect(jsonPath("$.umbralCancelacionHoras").value(12));
    }

    @Test
    @DisplayName("PUT /api/profesionales/1/configuracion -> 200 OK")
    void modificarConfiguracion_retorna200() throws Exception {
        ConfiguracionRequestDto request = new ConfiguracionRequestDto(2, 45, true, 24);
        Configuracion config = new Configuracion();
        config.setId(10L);
        config.setProfesional(profesional);
        config.setCantidadMaxTurnosALaVez(2);
        config.setDuracionAproximadaPorTurno(45);
        config.setAgendaSoloManejadaPorProfesional(true);
        config.setUmbralCancelacionHoras(24);

        when(modificarConfiguracionProfesional.ejecutar(eq(1L), eq(2), eq(45), eq(true), eq(24), any()))
                .thenReturn(config);

        mockMvc.perform(put("/api/profesionales/1/configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadMaxTurnosALaVez").value(2))
                .andExpect(jsonPath("$.duracionAproximadaPorTurno").value(45))
                .andExpect(jsonPath("$.agendaSoloManejadaPorProfesional").value(true));
    }

    @Test
    @DisplayName("POST /api/profesionales/1/configuracion -> 201 Created")
    void registrarConfiguracion_retorna201() throws Exception {
        ConfiguracionRequestDto request = new ConfiguracionRequestDto(2, 45, true, 24);
        Configuracion config = new Configuracion();
        config.setId(10L);
        config.setProfesional(profesional);
        config.setCantidadMaxTurnosALaVez(2);
        config.setDuracionAproximadaPorTurno(45);
        config.setAgendaSoloManejadaPorProfesional(true);
        config.setUmbralCancelacionHoras(24);

        when(registrarConfiguracionProfesional.ejecutar(eq(1L), eq(2), eq(45), eq(true), eq(24), any()))
                .thenReturn(config);

        mockMvc.perform(post("/api/profesionales/1/configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.profesionalId").value(1))
                .andExpect(jsonPath("$.cantidadMaxTurnosALaVez").value(2))
                .andExpect(jsonPath("$.duracionAproximadaPorTurno").value(45))
                .andExpect(jsonPath("$.agendaSoloManejadaPorProfesional").value(true))
                .andExpect(jsonPath("$.umbralCancelacionHoras").value(24));
    }

    @Test
    @DisplayName("POST /api/profesionales/99/configuracion con profesional inexistente -> 404 Not Found")
    void registrarConfiguracion_profesionalNoExiste_retorna404() throws Exception {
        ConfiguracionRequestDto request = new ConfiguracionRequestDto(2, 45, true, 24);

        when(registrarConfiguracionProfesional.ejecutar(eq(99L), eq(2), eq(45), eq(true), eq(24), any()))
                .thenThrow(new EntidadNoEncontradaException("Profesional", 99L));

        mockMvc.perform(post("/api/profesionales/99/configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/profesionales/1/configuracion con datos inválidos -> 400 Bad Request")
    void registrarConfiguracion_invalido_retorna400() throws Exception {
        ConfiguracionRequestDto request = new ConfiguracionRequestDto(0, -5, null, -1);

        mockMvc.perform(post("/api/profesionales/1/configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
