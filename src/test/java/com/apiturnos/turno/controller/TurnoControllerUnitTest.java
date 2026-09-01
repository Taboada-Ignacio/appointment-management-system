package com.apiturnos.turno.controller;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.GlobalExceptionHandler;
import com.apiturnos.shared.exception.TransicionEstadoInvalidaException;
import com.apiturnos.shared.exception.TurnoNoPerteneceProfesionalException;
import com.apiturnos.turno.dto.CancelarTurnoRequestDto;
import com.apiturnos.turno.dto.DarDeBajaTurnoRequestDto;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.service.CancelarTurno;
import com.apiturnos.turno.service.DarDeBajaTurno;
import com.apiturnos.turno.service.ResultadoCancelacionTurno;
import com.apiturnos.turno.service.TipoResolucionCancelacion;
import com.apiturnos.turno.service.TurnoYaIniciadoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TurnoControllerUnitTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private CancelarTurno cancelarTurno;
    @Mock private DarDeBajaTurno darDeBajaTurno;
    @Mock private TurnoRepository turnoRepository;

    private TurnoController controller;
    private Turno turnoMock;

    @BeforeEach
    void setUp() {
        controller = new TurnoController(cancelarTurno, darDeBajaTurno, turnoRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Profesional profesional = new Profesional();
        profesional.setId(1L);

        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesional);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);

        DiaAgenda dia = new DiaAgenda();
        dia.setId(10L);
        dia.setFecha(LocalDate.of(2030, 9, 10));
        dia.setMesAgenda(mes);

        Cliente cliente = new Cliente();
        cliente.setId(20L);
        cliente.setNombre("Juan");
        cliente.setApellido("Perez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("30111222");

        TipoAtencion tipo = new TipoAtencion();
        tipo.setId(30L);
        tipo.setNombre("Consulta");
        tipo.setDuracionMinutos(30);
        tipo.setCapacidadSimultanea(1);

        turnoMock = new Turno();
        turnoMock.setId(100L);
        turnoMock.setDiaAgenda(dia);
        turnoMock.setCliente(cliente);
        turnoMock.setTipoAtencion(tipo);
        turnoMock.setInicioEstimado(Instant.parse("2030-09-10T10:00:00Z"));
        turnoMock.setFinEstimado(Instant.parse("2030-09-10T10:30:00Z"));
        turnoMock.setOrigen(OrigenTurno.PROFESIONAL);
    }

    @Test
    @DisplayName("Cancelación anticipada devuelve 200 con resultado ELIMINADO_ANTICIPADAMENTE")
    void testCancelacionAnticipadaExitosa() throws Exception {
        when(cancelarTurno.ejecutar(eq(1L), eq(100L), any(), eq("admin")))
                .thenReturn(new ResultadoCancelacionTurno(100L, TipoResolucionCancelacion.ELIMINACION_ANTICIPADA));

        mockMvc.perform(post("/api/profesionales/1/turnos/100/cancelacion")
                        .header("X-Usuario", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("ELIMINADO_ANTICIPADAMENTE")))
                .andExpect(jsonPath("$.turnoId", is(100)))
                .andExpect(jsonPath("$.turno", is(nullValue())));

        verify(cancelarTurno).ejecutar(1L, 100L, null, "admin");
    }

    @Test
    @DisplayName("Cancelación dentro del umbral devuelve 200 con resultado CANCELADO y turno")
    void testCancelacionConHistorialExitosa() throws Exception {
        when(cancelarTurno.ejecutar(eq(1L), eq(100L), eq("Problema personal"), eq("profesional")))
                .thenReturn(new ResultadoCancelacionTurno(100L, TipoResolucionCancelacion.CANCELACION_CON_HISTORIAL));
        when(turnoRepository.findByIdConRelaciones(100L)).thenReturn(Optional.of(turnoMock));

        CancelarTurnoRequestDto request = new CancelarTurnoRequestDto("Problema personal");

        mockMvc.perform(post("/api/profesionales/1/turnos/100/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("CANCELADO")))
                .andExpect(jsonPath("$.turnoId", is(100)))
                .andExpect(jsonPath("$.turno.id", is(100)))
                .andExpect(jsonPath("$.turno.estado", is("CANCELADO")))
                .andExpect(jsonPath("$.turno.cliente.nombre", is("Juan")))
                .andExpect(jsonPath("$.turno.tipoAtencion.nombre", is("Consulta")));

        verify(cancelarTurno).ejecutar(1L, 100L, "Problema personal", "profesional");
    }

    @Test
    @DisplayName("Cancelación sin motivo cuando cae en umbral devuelve 400")
    void testCancelacionSinMotivoDevuelve400() throws Exception {
        when(cancelarTurno.ejecutar(eq(1L), eq(100L), any(), any()))
                .thenThrow(new EstadoInvalidoException("Cancelar un Turno requiere MotivoBajaTurno"));

        mockMvc.perform(post("/api/profesionales/1/turnos/100/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Cancelar un Turno requiere MotivoBajaTurno")));
    }

    @Test
    @DisplayName("Cancelación de turno ya iniciado devuelve 400")
    void testCancelacionTurnoYaIniciadoDevuelve400() throws Exception {
        when(cancelarTurno.ejecutar(eq(1L), eq(100L), any(), any()))
                .thenThrow(new TurnoYaIniciadoException(100L, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/profesionales/1/turnos/100/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Motivo"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Cancelación de turno en estado terminal devuelve 400")
    void testCancelacionTransicionInvalidaDevuelve400() throws Exception {
        when(cancelarTurno.ejecutar(eq(1L), eq(100L), any(), any()))
                .thenThrow(new TransicionEstadoInvalidaException("CANCELADO", "CANCELADO", "TURNO", 100L));

        mockMvc.perform(post("/api/profesionales/1/turnos/100/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Motivo"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Cancelación de turno de otro profesional devuelve 403 Forbidden")
    void testCancelacionTurnoAjenoDevuelve403() throws Exception {
        when(cancelarTurno.ejecutar(eq(1L), eq(100L), any(), any()))
                .thenThrow(new TurnoNoPerteneceProfesionalException(100L, 1L));

        mockMvc.perform(post("/api/profesionales/1/turnos/100/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Motivo"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("Cancelación de turno inexistente devuelve 404 Not Found")
    void testCancelacionTurnoInexistenteDevuelve404() throws Exception {
        when(cancelarTurno.ejecutar(eq(1L), eq(999L), any(), any()))
                .thenThrow(new EntidadNoEncontradaException("Turno", 999L));

        mockMvc.perform(post("/api/profesionales/1/turnos/999/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelarTurnoRequestDto("Motivo"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("Baja administrativa devuelve 200 con estado DADO_DE_BAJA")
    void testBajaAdministrativaExitosa() throws Exception {
        when(darDeBajaTurno.ejecutar(eq(1L), eq(100L), eq("Baja manual por mantenimiento"), eq("admin")))
                .thenReturn(turnoMock);
        when(turnoRepository.findByIdConRelaciones(100L)).thenReturn(Optional.of(turnoMock));

        DarDeBajaTurnoRequestDto request = new DarDeBajaTurnoRequestDto("Baja manual por mantenimiento");

        mockMvc.perform(post("/api/profesionales/1/turnos/100/baja")
                        .header("X-Usuario", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.turnoId", is(100)))
                .andExpect(jsonPath("$.estado", is("DADO_DE_BAJA")))
                .andExpect(jsonPath("$.cliente.nombre", is("Juan")))
                .andExpect(jsonPath("$.tipoAtencion.nombre", is("Consulta")));

        verify(darDeBajaTurno).ejecutar(1L, 100L, "Baja manual por mantenimiento", "admin");
    }

    @Test
    @DisplayName("Baja administrativa sin motivo produce 400 por validación DTO")
    void testBajaSinMotivoDevuelve400() throws Exception {
        DarDeBajaTurnoRequestDto requestInvalido = new DarDeBajaTurnoRequestDto("");

        mockMvc.perform(post("/api/profesionales/1/turnos/100/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.motivo").isNotEmpty());
    }

    @Test
    @DisplayName("Baja administrativa de turno ajeno devuelve 403 Forbidden")
    void testBajaTurnoAjenoDevuelve403() throws Exception {
        when(darDeBajaTurno.ejecutar(eq(1L), eq(100L), any(), any()))
                .thenThrow(new TurnoNoPerteneceProfesionalException(100L, 1L));

        DarDeBajaTurnoRequestDto request = new DarDeBajaTurnoRequestDto("Motivo");

        mockMvc.perform(post("/api/profesionales/1/turnos/100/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }
}
