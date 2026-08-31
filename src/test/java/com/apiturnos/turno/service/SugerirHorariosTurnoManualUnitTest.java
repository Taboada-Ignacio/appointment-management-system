package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SugerirHorariosTurnoManualUnitTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 10);

    @Mock private TipoAtencionRepository tipoAtencionRepository;
    @Mock private DiaAgendaRepository diaAgendaRepository;
    @Mock private ExcepcionAgendaRepository excepcionAgendaRepository;
    @Mock private CalcularDisponibilidadDia calcularDisponibilidadDia;
    @Mock private VerificarCapacidadTipoAtencion verificadorCapacidad;
    @Mock private GestorCambioEstado gestorCambioEstado;
    @Mock private EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad;

    private SugerirHorariosTurnoManual casoDeUso;
    private TipoAtencion tipo;
    private DiaAgenda dia;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-10T09:15:00Z"), ZoneOffset.UTC);
        casoDeUso = new SugerirHorariosTurnoManual(
                tipoAtencionRepository,
                diaAgendaRepository,
                excepcionAgendaRepository,
                calcularDisponibilidadDia,
                verificadorCapacidad,
                gestorCambioEstado,
                evaluadorDisponibilidad,
                clock);

        tipo = new TipoAtencion();
        tipo.setId(2L);
        tipo.setActivo(true);
        tipo.setDuracionMinutos(30);

        dia = new DiaAgenda();
        dia.setId(3L);
        dia.setFecha(FECHA);

        lenient().when(tipoAtencionRepository.findByIdAndProfesionalId(2L, 1L))
                .thenReturn(Optional.of(tipo));
        lenient().when(diaAgendaRepository.findByProfesionalIdAndFecha(1L, FECHA))
                .thenReturn(Optional.of(dia));
        lenient().when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 3L))
                .thenReturn("ACTIVO");
        lenient().when(excepcionAgendaRepository.findActivasAplicablesAFecha(1L, FECHA))
                .thenReturn(List.of());
        lenient().when(evaluadorDisponibilidad.hayCierreCompleto(anyCollection())).thenReturn(false);
        lenient().when(evaluadorDisponibilidad.intersectaBloqueoExplicito(any(), anyCollection()))
                .thenReturn(false);
        lenient().when(calcularDisponibilidadDia.ejecutar(1L, FECHA))
                .thenReturn(List.of(new IntervaloHorario(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        lenient().when(verificadorCapacidad.evaluar(any(), any(), any(), isNull()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(0, 1, true, false));
    }

    @Test
    void generaIntervalosSegunDuracionDelTipo() {
        List<HorarioSugeridoTurnoManual> resultado = casoDeUso.ejecutar(1L, 2L, FECHA);

        assertThat(resultado).extracting(HorarioSugeridoTurnoManual::horaInicio)
                .containsExactly(
                        LocalTime.of(8, 0),
                        LocalTime.of(8, 30),
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 30));
    }

    @Test
    void noOcultaSobrecapacidadManualSinoQueLaAdvierte() {
        when(verificadorCapacidad.evaluar(any(), any(), any(), isNull()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(1, 1, false, true));

        List<HorarioSugeridoTurnoManual> resultado = casoDeUso.ejecutar(1L, 2L, FECHA);

        assertThat(resultado).hasSize(4);
        assertThat(resultado).allSatisfy(slot -> assertThat(slot.advertencias())
                .containsExactly(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA));
    }

    @Test
    void diaEnTranscursoSoloSugiereHorariosQueNoComenzaron() {
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 3L))
                .thenReturn("EN_TRANSCURSO");

        List<HorarioSugeridoTurnoManual> resultado = casoDeUso.ejecutar(1L, 2L, FECHA);

        assertThat(resultado).extracting(HorarioSugeridoTurnoManual::horaInicio)
                .containsExactly(LocalTime.of(9, 30));
    }
}
