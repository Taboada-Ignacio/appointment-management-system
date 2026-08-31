package com.apiturnos.turno.controller;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.GlobalExceptionHandler;
import com.apiturnos.turno.dto.CrearTurnoManualRequestDto;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.MotivoRechazoTurnoManual;
import com.apiturnos.turno.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TurnoManualControllerUnitTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 10);
    private static final Instant INICIO = FECHA.atTime(9, 0).toInstant(ZoneOffset.UTC);
    private static final Instant FIN = FECHA.atTime(9, 30).toInstant(ZoneOffset.UTC);

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock private CrearTurnoManual crearTurnoManual;
    @Mock private ValidadorCrearTurnoManual validadorCrearTurnoManual;
    @Mock private SugerirHorariosTurnoManual sugerirHorariosTurnoManual;
    @Mock private DiaAgendaRepository diaAgendaRepository;
    @Mock private TipoAtencionRepository tipoAtencionRepository;

    private Clock clock;
    private TurnoManualController controller;

    private DiaAgenda dia;
    private Cliente cliente;
    private TipoAtencion tipo;
    private DatosConfirmacionTurnoManual datosConfirmacion;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(INICIO, ZoneOffset.UTC);
        controller = new TurnoManualController(
                crearTurnoManual,
                validadorCrearTurnoManual,
                sugerirHorariosTurnoManual,
                diaAgendaRepository,
                tipoAtencionRepository,
                clock);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        dia = new DiaAgenda();
        dia.setId(10L);
        dia.setFecha(FECHA);

        cliente = new Cliente();
        cliente.setId(20L);
        cliente.setNombre("Carlos");
        cliente.setApellido("Perez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("30111222");

        tipo = new TipoAtencion();
        tipo.setId(30L);
        tipo.setNombre("Consulta");
        tipo.setDuracionMinutos(30);
        tipo.setCapacidadSimultanea(1);

        datosConfirmacion = new DatosConfirmacionTurnoManual(
                cliente.getId(), cliente.getNombre(), cliente.getApellido(),
                cliente.getTipoDocumento(), cliente.getNumeroDocumento(), FECHA,
                LocalTime.of(9, 0), LocalTime.of(9, 30), tipo.getId(), tipo.getNombre());

        lenient().when(tipoAtencionRepository.findById(30L)).thenReturn(Optional.of(tipo));
        lenient().when(diaAgendaRepository.findByProfesionalIdAndFecha(1L, FECHA))
                .thenReturn(Optional.of(dia));
    }

    @Test
    @DisplayName("GET /horarios-sugeridos - Devuelve lista de sugerencias")
    void testHorariosSugeridos() throws Exception {
        HorarioSugeridoTurnoManual sugerido = new HorarioSugeridoTurnoManual(
                LocalTime.of(9, 0), LocalTime.of(9, 30), 30, 0, 1, List.of());
        when(sugerirHorariosTurnoManual.ejecutar(1L, 30L, FECHA))
                .thenReturn(List.of(sugerido));

        mockMvc.perform(get("/api/profesionales/1/turnos/horarios-sugeridos")
                        .param("tipoAtencionId", "30")
                        .param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$[0].horaFin").value("09:30:00"))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30));
    }

    @Test
    @DisplayName("POST /validar - Valida y devuelve estructura de confirmación sin persistir")
    void testValidarTurnoManual() throws Exception {
        ValidadorCrearTurnoManual.ContextoValidado contexto = new ValidadorCrearTurnoManual.ContextoValidado(
                dia, cliente, tipo, List.of(), datosConfirmacion);
        when(validadorCrearTurnoManual.validar(any())).thenReturn(contexto);

        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                20L, 10L, 30L, INICIO, FIN, false, "Observaciones");

        mockMvc.perform(post("/api/profesionales/1/turnos/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creado").value(false))
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.requiereConfirmacion").value(false))
                .andExpect(jsonPath("$.cliente.nombre").value("Carlos"))
                .andExpect(jsonPath("$.tipoAtencion.nombre").value("Consulta"));
    }

    @Test
    @DisplayName("POST /turnos - Creación exitosa retorna 201 Created")
    void testCrearTurnoExitoso() throws Exception {
        ResultadoCrearTurnoManual resultado = ResultadoCrearTurnoManual.creado(
                100L, List.of(), datosConfirmacion);
        when(crearTurnoManual.ejecutar(any())).thenReturn(resultado);

        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                20L, 10L, 30L, INICIO, FIN, false, "Observaciones");

        mockMvc.perform(post("/api/profesionales/1/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creado").value(true))
                .andExpect(jsonPath("$.turnoId").value(100))
                .andExpect(jsonPath("$.estado").value("ASIGNADO"));
    }

    @Test
    @DisplayName("POST /turnos - Requiere confirmación retorna 200 OK con requiereConfirmacion: true")
    void testCrearTurnoRequiereConfirmacion() throws Exception {
        ResultadoCrearTurnoManual resultado = ResultadoCrearTurnoManual.requiereConfirmacion(
                List.of(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA), datosConfirmacion);
        when(crearTurnoManual.ejecutar(any())).thenReturn(resultado);

        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                20L, 10L, 30L, INICIO, FIN, false, "Observaciones");

        mockMvc.perform(post("/api/profesionales/1/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creado").value(false))
                .andExpect(jsonPath("$.requiereConfirmacion").value(true))
                .andExpect(jsonPath("$.advertencias[0]").value("HORARIO_FUERA_DE_BRECHA"));
    }

    @Test
    @DisplayName("POST /turnos - Error de negocio bloqueante retorna 400 Bad Request")
    void testCrearTurnoBloqueante400() throws Exception {
        when(crearTurnoManual.ejecutar(any()))
                .thenThrow(new TurnoManualNoPermitidoException(
                        MotivoRechazoTurnoManual.DIA_INACTIVO, "Día inactivo"));

        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                20L, 10L, 30L, INICIO, FIN, false, "Observaciones");

        mockMvc.perform(post("/api/profesionales/1/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Día inactivo"));
    }

    @Test
    @DisplayName("POST /turnos - Cliente ajeno retorna 403 Forbidden")
    void testCrearTurnoClienteAjeno403() throws Exception {
        when(crearTurnoManual.ejecutar(any()))
                .thenThrow(new ClienteNoPerteneceProfesionalException(20L, 1L));

        CrearTurnoManualRequestDto request = new CrearTurnoManualRequestDto(
                20L, 10L, 30L, INICIO, FIN, false, "Observaciones");

        mockMvc.perform(post("/api/profesionales/1/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}

