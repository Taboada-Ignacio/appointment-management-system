package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.BrechaHorariaRequestDto;
import com.apiturnos.agenda.dto.BrechaHorariaResponseDto;
import com.apiturnos.agenda.dto.ConfigurarDiaRequestDto;
import com.apiturnos.agenda.dto.DiaAgendaDetalleResponseDto;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.service.ConfigurarDiaAgenda;
import com.apiturnos.agenda.service.ObtenerDetalleDiaAgenda;
import com.apiturnos.shared.exception.DiaAgendaNoValidoException;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiaAgendaControllerUnitTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ObtenerDetalleDiaAgenda obtenerDetalleDiaAgenda;
    @Mock
    private ConfigurarDiaAgenda configurarDiaAgenda;

    @InjectMocks
    private DiaAgendaController diaAgendaController;

    private DiaAgendaDetalleResponseDto diaDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(diaAgendaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        diaDto = new DiaAgendaDetalleResponseDto(
                null, "ACTIVO", List.of(new BrechaHorariaResponseDto(1L, LocalTime.of(8, 0), LocalTime.of(12, 0)))
        );
        diaDto.setId(50L);
        diaDto.setFecha(LocalDate.of(2027, 5, 10));
        diaDto.setDiaSemana(DayOfWeek.MONDAY);
        diaDto.setNombreDiaSemana("lunes");
    }

    @Test
    @DisplayName("GET /api/profesionales/{profesionalId}/dias-agenda/{diaAgendaId} - Retorna detalle 200")
    void testObtenerDetalleDia200() throws Exception {
        when(obtenerDetalleDiaAgenda.ejecutar(1L, 50L)).thenReturn(diaDto);

        mockMvc.perform(get("/api/profesionales/1/dias-agenda/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.nombreDiaSemana").value("lunes"))
                .andExpect(jsonPath("$.estadoActual").value("ACTIVO"));
    }

    @Test
    @DisplayName("PUT /api/profesionales/{profesionalId}/dias-agenda/{diaAgendaId}/brechas - 400 cuando horario inválido")
    void testConfigurarBrechasInvalido400() throws Exception {
        when(configurarDiaAgenda.ejecutar(eq(1L), eq(50L), any(), any()))
                .thenThrow(new DiaAgendaNoValidoException("La hora de inicio debe ser anterior a la hora de fin"));

        ConfigurarDiaRequestDto request = new ConfigurarDiaRequestDto(List.of(
                new BrechaHorariaRequestDto(LocalTime.of(14, 0), LocalTime.of(10, 0))
        ));

        mockMvc.perform(put("/api/profesionales/1/dias-agenda/50/brechas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

