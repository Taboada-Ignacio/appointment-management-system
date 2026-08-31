package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.MotivoRechazoTurnoManual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ValidadorCrearTurnoManualUnitTest {

    private static final Long PROFESIONAL_ID = 1L;
    private static final Long DIA_ID = 10L;
    private static final Long CLIENTE_ID = 20L;
    private static final Long TIPO_ID = 30L;
    private static final Instant AHORA = Instant.parse("2026-09-10T12:00:00Z");

    @Mock private DiaAgendaRepository diaAgendaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private TipoAtencionRepository tipoAtencionRepository;
    @Mock private BrechaHorariaRepository brechaHorariaRepository;
    @Mock private ExcepcionAgendaRepository excepcionAgendaRepository;
    @Mock private GestorCambioEstado gestorCambioEstado;
    @Mock private EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad;
    @Mock private VerificarCapacidadTipoAtencion verificadorCapacidad;

    private ValidadorCrearTurnoManual validador;
    private DiaAgenda dia;
    private Cliente cliente;
    private TipoAtencion tipo;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        validador = new ValidadorCrearTurnoManual(
                diaAgendaRepository,
                clienteRepository,
                tipoAtencionRepository,
                brechaHorariaRepository,
                excepcionAgendaRepository,
                gestorCambioEstado,
                evaluadorDisponibilidad,
                verificadorCapacidad,
                clock);

        Profesional profesional = new Profesional();
        profesional.setId(PROFESIONAL_ID);

        dia = new DiaAgenda();
        dia.setId(DIA_ID);
        dia.setFecha(LocalDate.of(2026, 9, 10));

        cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setProfesional(profesional);
        cliente.setNombre("Ana");
        cliente.setApellido("Pérez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("30111222");

        tipo = new TipoAtencion();
        tipo.setId(TIPO_ID);
        tipo.setProfesional(profesional);
        tipo.setNombre("Consulta");
        tipo.setDuracionMinutos(30);
        tipo.setCapacidadSimultanea(1);
        tipo.setActivo(true);

        lenient().when(diaAgendaRepository.findByIdAndProfesionalId(DIA_ID, PROFESIONAL_ID))
                .thenReturn(Optional.of(dia));
        lenient().when(clienteRepository.findByIdAndProfesionalId(CLIENTE_ID, PROFESIONAL_ID))
                .thenReturn(Optional.of(cliente));
        lenient().when(tipoAtencionRepository.findByIdAndProfesionalId(TIPO_ID, PROFESIONAL_ID))
                .thenReturn(Optional.of(tipo));
        lenient().when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, CLIENTE_ID))
                .thenReturn("HABILITADO");
        lenient().when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, DIA_ID))
                .thenReturn("ACTIVO");
        lenient().when(brechaHorariaRepository.findByDiaAgendaId(DIA_ID)).thenReturn(List.of());
        lenient().when(excepcionAgendaRepository.findActivasAplicablesAFecha(
                PROFESIONAL_ID, LocalDate.of(2026, 9, 10))).thenReturn(List.of());
        lenient().when(evaluadorDisponibilidad.evaluar(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of());
        lenient().when(verificadorCapacidad.evaluar(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(0, 1, true, false));
    }

    @Test
    void clienteRequiereAprobacionEsValidoParaAltaDelProfesional() {
        org.mockito.Mockito.when(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.CLIENTE, CLIENTE_ID)).thenReturn("REQUIERE_APROBACION");

        ValidadorCrearTurnoManual.ContextoValidado resultado = validador.validar(solicitud(14, 0, 14, 30));

        assertThat(resultado.cliente()).isSameAs(cliente);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INHABILITADO", "DADO_DE_BAJA", "PENDIENTE_DE_VERIFICACION"})
    void estadosClienteNoPermitidosSonRechazados(String estado) {
        org.mockito.Mockito.when(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.CLIENTE, CLIENTE_ID)).thenReturn(estado);

        assertRechazo(solicitud(14, 0, 14, 30), MotivoRechazoTurnoManual.CLIENTE_NO_HABILITADO);
    }

    @Test
    void clienteDeOtroProfesionalEsRechazadoConConsultaAcotada() {
        org.mockito.Mockito.when(clienteRepository.findByIdAndProfesionalId(99L, PROFESIONAL_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validador.validar(solicitud(99L, TIPO_ID, 14, 0, 14, 30)))
                .isInstanceOf(ClienteNoPerteneceProfesionalException.class);
    }

    @Test
    void tipoAtencionDeOtroProfesionalEsRechazadoConConsultaAcotada() {
        org.mockito.Mockito.when(tipoAtencionRepository.findByIdAndProfesionalId(99L, PROFESIONAL_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validador.validar(
                solicitud(CLIENTE_ID, 99L, 14, 0, 14, 30)))
                .isInstanceOf(TipoAtencionNoPerteneceProfesionalException.class);
    }

    @Test
    void tipoAtencionInactivoEsRechazado() {
        tipo.setActivo(false);

        assertRechazo(solicitud(14, 0, 14, 30), MotivoRechazoTurnoManual.TIPO_ATENCION_INACTIVO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVO", "FINALIZADO"})
    void estadosDiaNoSeleccionablesSonRechazados(String estado) {
        org.mockito.Mockito.when(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.DIA_AGENDA, DIA_ID)).thenReturn(estado);

        MotivoRechazoTurnoManual motivo = "INACTIVO".equals(estado)
                ? MotivoRechazoTurnoManual.DIA_INACTIVO
                : MotivoRechazoTurnoManual.DIA_FINALIZADO;
        assertRechazo(solicitud(14, 0, 14, 30), motivo);
    }

    @Test
    void diaEnTranscursoPermiteHorarioFuturo() {
        org.mockito.Mockito.when(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.DIA_AGENDA, DIA_ID)).thenReturn("EN_TRANSCURSO");

        assertThat(validador.validar(solicitud(13, 0, 13, 30))).isNotNull();
    }

    @Test
    void diaEnTranscursoRechazaHorarioQueYaComenzo() {
        org.mockito.Mockito.when(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.DIA_AGENDA, DIA_ID)).thenReturn("EN_TRANSCURSO");

        assertRechazo(solicitud(11, 0, 11, 30), MotivoRechazoTurnoManual.HORARIO_YA_INICIADO);
    }

    @Test
    void fechaAnteriorEsRechazadaConClockInyectado() {
        dia.setFecha(LocalDate.of(2026, 9, 9));

        assertRechazo(solicitudEnFecha(LocalDate.of(2026, 9, 9), 14, 0, 14, 30),
                MotivoRechazoTurnoManual.FECHA_PASADA);
    }

    @Test
    void acumulaFueraDeBrechaYCapacidadSuperada() {
        org.mockito.Mockito.when(evaluadorDisponibilidad.evaluar(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA));
        org.mockito.Mockito.when(verificadorCapacidad.evaluar(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(1, 1, false, true));

        ValidadorCrearTurnoManual.ContextoValidado resultado = validador.validar(solicitud(14, 0, 14, 30));

        assertThat(resultado.advertencias()).containsExactly(
                AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA,
                AdvertenciaTurnoManual.CAPACIDAD_SUPERADA);
        assertThat(resultado.datosConfirmacion().numeroDocumento()).isEqualTo("30111222");
        assertThat(resultado.datosConfirmacion().fecha()).isEqualTo(dia.getFecha());
    }

    private void assertRechazo(SolicitudCrearTurnoManual solicitud, MotivoRechazoTurnoManual motivo) {
        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(TurnoManualNoPermitidoException.class)
                .extracting(ex -> ((TurnoManualNoPermitidoException) ex).getMotivo())
                .isEqualTo(motivo);
    }

    private SolicitudCrearTurnoManual solicitud(int hi, int mi, int hf, int mf) {
        return solicitud(CLIENTE_ID, TIPO_ID, hi, mi, hf, mf);
    }

    private SolicitudCrearTurnoManual solicitud(
            Long clienteId, Long tipoId, int hi, int mi, int hf, int mf) {
        return solicitudEnFecha(LocalDate.of(2026, 9, 10), clienteId, tipoId, hi, mi, hf, mf);
    }

    private SolicitudCrearTurnoManual solicitudEnFecha(
            LocalDate fecha, int hi, int mi, int hf, int mf) {
        return solicitudEnFecha(fecha, CLIENTE_ID, TIPO_ID, hi, mi, hf, mf);
    }

    private SolicitudCrearTurnoManual solicitudEnFecha(
            LocalDate fecha, Long clienteId, Long tipoId, int hi, int mi, int hf, int mf) {
        return new SolicitudCrearTurnoManual(
                PROFESIONAL_ID,
                DIA_ID,
                clienteId,
                tipoId,
                fecha.atTime(hi, mi).toInstant(ZoneOffset.UTC),
                fecha.atTime(hf, mf).toInstant(ZoneOffset.UTC),
                false,
                "observaciones",
                "profesional@test");
    }

}
