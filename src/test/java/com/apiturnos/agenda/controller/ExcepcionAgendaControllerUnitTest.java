package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.ImpactoExcepcionAgendaResponseDto;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.service.AplicarExcepcionAgenda;
import com.apiturnos.agenda.service.AplicarExcepcionConResoluciones;
import com.apiturnos.agenda.service.CancelarExcepcionAgenda;
import com.apiturnos.agenda.service.ConstructorImpactoExcepcionAgenda;
import com.apiturnos.agenda.service.ModificarExcepcionAgenda;
import com.apiturnos.agenda.service.PrevisualizarExcepcionAgenda;
import com.apiturnos.agenda.service.ResultadoAplicacionExcepcionAgenda;
import com.apiturnos.agenda.service.TokenImpactoExcepcionAgenda;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExcepcionAgendaControllerUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @Mock ExcepcionAgendaRepository repository;
    @Mock PrevisualizarExcepcionAgenda previsualizar;
    @Mock AplicarExcepcionAgenda aplicar;
    @Mock ModificarExcepcionAgenda modificar;
    @Mock CancelarExcepcionAgenda cancelar;
    @Mock ConstructorImpactoExcepcionAgenda constructorImpacto;
    @Mock TokenImpactoExcepcionAgenda tokenImpacto;
    @Mock AplicarExcepcionConResoluciones aplicarConResoluciones;
    @InjectMocks ExcepcionAgendaController controller;

    private ExcepcionAgenda excepcion;
    private ImpactoExcepcionAgendaResponseDto impacto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        Profesional profesional = new Profesional();
        profesional.setId(1L);
        excepcion = new ExcepcionAgenda();
        excepcion.setId(10L);
        excepcion.setProfesional(profesional);
        excepcion.setFechaInicio(LocalDate.of(2030, 3, 10));
        excepcion.setFechaFin(LocalDate.of(2030, 3, 10));
        excepcion.setTipo(TipoExcepcion.BLOQUEO_HORARIO);
        excepcion.setMotivo("Capacitación");
        excepcion.setActiva(true);
        excepcion.setFechaCreacion(Instant.parse("2030-01-01T12:00:00Z"));
        excepcion.agregarBrecha(java.time.LocalTime.of(9, 0), java.time.LocalTime.of(11, 0));
        impacto = new ImpactoExcepcionAgendaResponseDto(0, 0, 0, List.of());
    }

    @Test
    void listaFiltradaYObtieneDetalle() throws Exception {
        when(repository.findByProfesionalId(1L)).thenReturn(List.of(excepcion));
        when(repository.findByIdAndProfesionalId(10L, 1L)).thenReturn(Optional.of(excepcion));

        mockMvc.perform(get("/api/profesionales/1/excepciones-agenda")
                        .param("desde", "2030-03-01").param("hasta", "2030-03-31").param("activa", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].brechas[0].horaInicio").value("09:00:00"));

        mockMvc.perform(get("/api/profesionales/1/excepciones-agenda/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("BLOQUEO_HORARIO"));
    }

    @Test
    void previewNuevaNoInvocaAplicacion() throws Exception {
        when(previsualizar.nueva(eq(1L), any())).thenReturn(List.of());
        when(tokenImpacto.generar(any(), eq(List.of()))).thenReturn("token");
        when(constructorImpacto.construir("token", List.of())).thenReturn(impacto);

        mockMvc.perform(post("/api/profesionales/1/excepciones-agenda/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadTurnosAfectados").value(0));

        verify(previsualizar).nueva(eq(1L), any());
    }

    @Test
    void creaModificaYCancela() throws Exception {
        ResultadoAplicacionExcepcionAgenda resultado =
                new ResultadoAplicacionExcepcionAgenda(excepcion, List.of());
        when(aplicar.ejecutarConResultado(eq(1L), any(), eq("profesional"))).thenReturn(resultado);
        when(modificar.ejecutarConResultado(eq(1L), eq(10L), any(), eq("profesional"))).thenReturn(resultado);
        when(cancelar.ejecutar(1L, 10L, "profesional")).thenReturn(excepcion);
        when(constructorImpacto.construir(List.of())).thenReturn(impacto);

        mockMvc.perform(post("/api/profesionales/1/excepciones-agenda")
                        .header("X-Usuario", "profesional")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.excepcion.id").value(10))
                .andExpect(jsonPath("$.impacto.cantidadTurnosAfectados").value(0));

        mockMvc.perform(put("/api/profesionales/1/excepciones-agenda/10")
                        .header("X-Usuario", "profesional")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/profesionales/1/excepciones-agenda/10")
                        .header("X-Usuario", "profesional"))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaRequestSinMotivo() throws Exception {
        String invalido = """
                {"fechaInicio":"2030-03-10","fechaFin":"2030-03-10",
                 "tipo":"BLOQUEO_HORARIO","brechas":[]}
                """;
        mockMvc.perform(post("/api/profesionales/1/excepciones-agenda")
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest());
    }

    private String requestJson() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "fechaInicio", "2030-03-10",
                "fechaFin", "2030-03-10",
                "tipo", "BLOQUEO_HORARIO",
                "motivo", "Capacitación",
                "brechas", List.of(java.util.Map.of("horaInicio", "09:00", "horaFin", "11:00"))));
    }
}
