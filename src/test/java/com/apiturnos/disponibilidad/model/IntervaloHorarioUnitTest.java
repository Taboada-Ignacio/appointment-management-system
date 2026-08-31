package com.apiturnos.disponibilidad.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntervaloHorarioUnitTest {

    @Test
    void exigeQueElInicioSeaAnteriorAlFin() {
        LocalTime diez = LocalTime.of(10, 0);

        assertThatThrownBy(() -> new IntervaloHorario(diez, diez))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntervaloHorario(diez, LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usaSemanticaSemiabiertaParaLosSolapamientos() {
        IntervaloHorario primero = intervalo("08:00", "10:00");
        IntervaloHorario contiguo = intervalo("10:00", "12:00");
        IntervaloHorario solapado = intervalo("09:30", "10:30");

        assertThat(primero.seSolapaCon(contiguo)).isFalse();
        assertThat(primero.seSolapaCon(solapado)).isTrue();
    }

    @Test
    void determinaContencionUsandoElIntervaloCompleto() {
        IntervaloHorario disponible = intervalo("08:00", "12:00");

        assertThat(disponible.contiene(intervalo("09:00", "09:30"))).isTrue();
        assertThat(disponible.contiene(intervalo("11:30", "12:30"))).isFalse();
    }

    private static IntervaloHorario intervalo(String inicio, String fin) {
        return new IntervaloHorario(LocalTime.parse(inicio), LocalTime.parse(fin));
    }
}
