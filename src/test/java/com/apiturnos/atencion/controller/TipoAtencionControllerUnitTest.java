package com.apiturnos.atencion.controller;

import com.apiturnos.atencion.dto.TipoAtencionRequestDto;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.service.ActivarTipoAtencion;
import com.apiturnos.atencion.service.EditarTipoAtencion;
import com.apiturnos.atencion.service.InactivarTipoAtencion;
import com.apiturnos.atencion.service.ListarTiposAtencion;
import com.apiturnos.atencion.service.ObtenerTipoAtencion;
import com.apiturnos.atencion.service.RegistrarTipoAtencion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.GlobalExceptionHandler;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TipoAtencionControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RegistrarTipoAtencion registrarTipoAtencion;

    @Mock
    private EditarTipoAtencion editarTipoAtencion;

    @Mock
    private ActivarTipoAtencion activarTipoAtencion;

    @Mock
    private InactivarTipoAtencion inactivarTipoAtencion;

    @Mock
    private ListarTiposAtencion listarTiposAtencion;

    @Mock
    private ObtenerTipoAtencion obtenerTipoAtencion;

    @InjectMocks
    private TipoAtencionController controller;

    private Profesional profesional;
    private TipoAtencion tipoAtencion;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        profesional = new Profesional();
        profesional.setId(1L);

        tipoAtencion = new TipoAtencion();
        tipoAtencion.setId(10L);
        tipoAtencion.setProfesional(profesional);
        tipoAtencion.setNombre("Consulta General");
        tipoAtencion.setDescripcion("Chequeo general");
        tipoAtencion.setDuracionMinutos(30);
        tipoAtencion.setCapacidadSimultanea(1);
        tipoAtencion.setActivo(true);
    }

    @Test
    @DisplayName("POST /api/profesionales/1/tipos-atencion con datos válidos -> 201 Created")
    void registrar_valido_retorna201() throws Exception {
        TipoAtencionRequestDto request = new TipoAtencionRequestDto("Consulta General", "Chequeo general", 30, 1);
        when(registrarTipoAtencion.ejecutar(eq(1L), eq("Consulta General"), eq("Chequeo general"), eq(30), eq(1), any()))
                .thenReturn(tipoAtencion);

        mockMvc.perform(post("/api/profesionales/1/tipos-atencion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Consulta General"))
                .andExpect(jsonPath("$.duracionMinutos").value(30))
                .andExpect(jsonPath("$.capacidadSimultanea").value(1))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("POST /api/profesionales/1/tipos-atencion con datos inválidos -> 400 Bad Request")
    void registrar_invalido_retorna400() throws Exception {
        TipoAtencionRequestDto request = new TipoAtencionRequestDto("", null, 0, 0);

        mockMvc.perform(post("/api/profesionales/1/tipos-atencion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/profesionales/1/tipos-atencion -> 200 OK")
    void listar_retorna200() throws Exception {
        when(listarTiposAtencion.ejecutar(1L, false)).thenReturn(List.of(tipoAtencion));

        mockMvc.perform(get("/api/profesionales/1/tipos-atencion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nombre").value("Consulta General"));
    }

    @Test
    @DisplayName("GET /api/profesionales/1/tipos-atencion/10 -> 200 OK")
    void obtenerPorId_retorna200() throws Exception {
        when(obtenerTipoAtencion.ejecutar(1L, 10L)).thenReturn(tipoAtencion);

        mockMvc.perform(get("/api/profesionales/1/tipos-atencion/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Consulta General"));
    }

    @Test
    @DisplayName("GET /api/profesionales/1/tipos-atencion/10 de otro profesional -> 403 Forbidden")
    void obtenerPorId_otroProfesional_retorna403() throws Exception {
        when(obtenerTipoAtencion.ejecutar(1L, 10L))
                .thenThrow(new TipoAtencionNoPerteneceProfesionalException(10L, 1L));

        mockMvc.perform(get("/api/profesionales/1/tipos-atencion/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/profesionales/1/tipos-atencion/10 -> 200 OK")
    void editar_retorna200() throws Exception {
        TipoAtencionRequestDto request = new TipoAtencionRequestDto("Consulta Extendida", "Desc", 45, 2);
        tipoAtencion.setNombre("Consulta Extendida");
        tipoAtencion.setDuracionMinutos(45);
        tipoAtencion.setCapacidadSimultanea(2);

        when(editarTipoAtencion.ejecutar(eq(1L), eq(10L), eq("Consulta Extendida"), eq("Desc"), eq(45), eq(2), any()))
                .thenReturn(tipoAtencion);

        mockMvc.perform(put("/api/profesionales/1/tipos-atencion/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Consulta Extendida"))
                .andExpect(jsonPath("$.duracionMinutos").value(45))
                .andExpect(jsonPath("$.capacidadSimultanea").value(2));
    }

    @Test
    @DisplayName("PATCH /api/profesionales/1/tipos-atencion/10/inactivar -> 200 OK")
    void inactivar_retorna200() throws Exception {
        tipoAtencion.setActivo(false);
        when(inactivarTipoAtencion.ejecutar(eq(1L), eq(10L), any())).thenReturn(tipoAtencion);

        mockMvc.perform(patch("/api/profesionales/1/tipos-atencion/10/inactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}

