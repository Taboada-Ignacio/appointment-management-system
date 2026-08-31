package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.*;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.service.*;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MesAgendaControllerUnitTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ObtenerDetalleMesAgenda obtenerDetalleMesAgenda;
    @Mock
    private ConfigurarMesAgenda configurarMesAgenda;
    @Mock
    private ConfigurarMesModoSemana configurarMesModoSemana;
    @Mock
    private ConfigurarMesModoMes configurarMesModoMes;
    @Mock
    private ActivarInactivarMesAgenda activarInactivarMesAgenda;
    @Mock
    private ActualizarRepetirConfiguracionMes actualizarRepetirConfiguracionMes;
    @Mock
    private RepetirConfiguracionMes repetirConfiguracionMes;
    @Mock
    private GestorCambioEstado gestorCambioEstado;

    @InjectMocks
    private MesAgendaController mesAgendaController;

    private MesAgenda mes;
    private MesAgendaDetalleResponseDto detalleDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mesAgendaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Profesional profesional = new Profesional();
        profesional.setId(1L);

        AgendaAnual agenda = new AgendaAnual();
        agenda.setId(10L);
        agenda.setProfesional(profesional);
        agenda.setAnio(2027);

        mes = new MesAgenda();
        mes.setId(100L);
        mes.setAgendaAnual(agenda);
        mes.setNroMes(5);
        mes.setRepetirConfiguracion(false);

        detalleDto = new MesAgendaDetalleResponseDto(mes, "ACTIVO", List.of());
    }

    @Test
    @DisplayName("GET /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId} - Retorna detalle 200")
    void testObtenerDetalle200() throws Exception {
        when(obtenerDetalleMesAgenda.ejecutar(1L, 100L)).thenReturn(detalleDto);

        mockMvc.perform(get("/api/profesionales/1/meses-agenda/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nroMes").value(5))
                .andExpect(jsonPath("$.estadoActual").value("ACTIVO"));
    }

    @Test
    @DisplayName("GET /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId} - 403 Forbidden si pertenece a otro profesional")
    void testObtenerDetalle403() throws Exception {
        when(obtenerDetalleMesAgenda.ejecutar(999L, 100L))
                .thenThrow(new ClienteNoPerteneceProfesionalException(100L, 999L));

        mockMvc.perform(get("/api/profesionales/999/meses-agenda/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/activar - Retorna 200")
    void testActivarMes200() throws Exception {
        doNothing().when(activarInactivarMesAgenda).activar(eq(1L), eq(100L), any());

        mockMvc.perform(post("/api/profesionales/1/meses-agenda/100/activar"))
                .andExpect(status().isOk());

        verify(activarInactivarMesAgenda, times(1)).activar(eq(1L), eq(100L), any());
    }

    @Test
    @DisplayName("POST /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/inactivar - Retorna 200")
    void testInactivarMes200() throws Exception {
        doNothing().when(activarInactivarMesAgenda).inactivar(eq(1L), eq(100L), any());

        mockMvc.perform(post("/api/profesionales/1/meses-agenda/100/inactivar"))
                .andExpect(status().isOk());

        verify(activarInactivarMesAgenda, times(1)).inactivar(eq(1L), eq(100L), any());
    }

    @Test
    @DisplayName("PUT /api/profesionales/{profesionalId}/meses-agenda/{mesAgendaId}/repetir-configuracion - Retorna 200")
    void testActualizarRepetirConfiguracion200() throws Exception {
        mes.setRepetirConfiguracion(true);
        when(actualizarRepetirConfiguracionMes.ejecutar(1L, 100L, true)).thenReturn(mes);
        when(gestorCambioEstado.obtenerNombreEstadoActual(any(), eq(100L))).thenReturn("ACTIVO");

        ActualizarRepetirConfiguracionRequestDto request = new ActualizarRepetirConfiguracionRequestDto(true);

        mockMvc.perform(put("/api/profesionales/1/meses-agenda/100/repetir-configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.repetirConfiguracion").value(true));
    }
}

