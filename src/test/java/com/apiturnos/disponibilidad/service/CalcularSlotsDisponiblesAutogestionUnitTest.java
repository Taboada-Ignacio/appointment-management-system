package com.apiturnos.disponibilidad.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.disponibilidad.dto.SlotDisponibleDto;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalcularSlotsDisponiblesAutogestionUnitTest {

    @Mock
    private TipoAtencionRepository tipoAtencionRepository;

    @Mock
    private CalcularDisponibilidadDia calcularDisponibilidadDia;

    @Mock
    private VerificarCapacidadTipoAtencion verificarCapacidadTipoAtencion;

    private CalcularSlotsDisponiblesAutogestion calculadorSlots;

    private Profesional profesional;
    private TipoAtencion tipoAtencion;
    private final LocalDate fecha = LocalDate.of(2026, 9, 1);

    @BeforeEach
    void setUp() {
        calculadorSlots = new CalcularSlotsDisponiblesAutogestion(
                tipoAtencionRepository,
                calcularDisponibilidadDia,
                verificarCapacidadTipoAtencion,
                "UTC"
        );

        profesional = new Profesional();
        profesional.setId(1L);

        tipoAtencion = new TipoAtencion();
        tipoAtencion.setId(10L);
        tipoAtencion.setProfesional(profesional);
        tipoAtencion.setNombre("Consulta General");
        tipoAtencion.setDuracionMinutos(30);
        tipoAtencion.setCapacidadSimultanea(1);
        tipoAtencion.setActivo(true);
    }

    @Test
    @DisplayName("TipoAtencion inactivo no ofrece intervalos para autogestión")
    void tipoInactivo_retornaVacio() {
        tipoAtencion.setActivo(false);
        when(tipoAtencionRepository.findById(10L)).thenReturn(Optional.of(tipoAtencion));

        List<SlotDisponibleDto> slots = calculadorSlots.ejecutar(1L, 10L, fecha);

        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("TipoAtencion de otro profesional lanza TipoAtencionNoPerteneceProfesionalException")
    void tipoOtroProfesional_lanzaExcepcion() {
        Profesional otro = new Profesional();
        otro.setId(999L);
        tipoAtencion.setProfesional(otro);

        when(tipoAtencionRepository.findById(10L)).thenReturn(Optional.of(tipoAtencion));

        assertThatThrownBy(() -> calculadorSlots.ejecutar(1L, 10L, fecha))
                .isInstanceOf(TipoAtencionNoPerteneceProfesionalException.class);
    }

    @Test
    @DisplayName("Autogestión no ofrece intervalo con capacidad completa")
    void autogestion_noOfreceIntervaloConCapacidadCompleta() {
        when(tipoAtencionRepository.findById(10L)).thenReturn(Optional.of(tipoAtencion));

        // Franja efectiva de 08:00 a 10:00 (4 slots de 30 min: 08:00, 08:30, 09:00, 09:30)
        when(calcularDisponibilidadDia.ejecutar(1L, fecha))
                .thenReturn(List.of(new IntervaloHorario(LocalTime.of(8, 0), LocalTime.of(10, 0))));

        // 08:00-08:30 -> disponible
        when(verificarCapacidadTipoAtencion.evaluar(
                eq(tipoAtencion), eq(Instant.parse("2026-09-01T08:00:00Z")), eq(Instant.parse("2026-09-01T08:30:00Z")), any()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(0, 1, true, false));

        // 08:30-09:00 -> capacidad agotada (1 turno ya asignado para capacidad 1)
        when(verificarCapacidadTipoAtencion.evaluar(
                eq(tipoAtencion), eq(Instant.parse("2026-09-01T08:30:00Z")), eq(Instant.parse("2026-09-01T09:00:00Z")), any()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(1, 1, false, true));

        // 09:00-09:30 -> disponible
        when(verificarCapacidadTipoAtencion.evaluar(
                eq(tipoAtencion), eq(Instant.parse("2026-09-01T09:00:00Z")), eq(Instant.parse("2026-09-01T09:30:00Z")), any()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(0, 1, true, false));

        // 09:30-10:00 -> disponible
        when(verificarCapacidadTipoAtencion.evaluar(
                eq(tipoAtencion), eq(Instant.parse("2026-09-01T09:30:00Z")), eq(Instant.parse("2026-09-01T10:00:00Z")), any()))
                .thenReturn(new VerificarCapacidadTipoAtencion.ResultadoCapacidad(0, 1, true, false));

        List<SlotDisponibleDto> slots = calculadorSlots.ejecutar(1L, 10L, fecha);

        assertThat(slots).hasSize(3);
        assertThat(slots).extracting(SlotDisponibleDto::horaInicio)
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(9, 30));
    }
}

