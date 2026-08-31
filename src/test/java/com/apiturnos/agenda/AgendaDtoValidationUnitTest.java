package com.apiturnos.agenda;

import com.apiturnos.agenda.dto.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgendaDtoValidationUnitTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CrearAgendaAnualRequestDto valida año nulo, menor a 2020 y mayor a 2100")
    void testValidacionCrearAgendaAnualRequest() {
        CrearAgendaAnualRequestDto dtoNulo = new CrearAgendaAnualRequestDto(null);
        Set<ConstraintViolation<CrearAgendaAnualRequestDto>> violations1 = validator.validate(dtoNulo);
        assertThat(violations1).isNotEmpty();

        CrearAgendaAnualRequestDto dtoMenor = new CrearAgendaAnualRequestDto(2010);
        Set<ConstraintViolation<CrearAgendaAnualRequestDto>> violations2 = validator.validate(dtoMenor);
        assertThat(violations2).isNotEmpty();

        CrearAgendaAnualRequestDto dtoMayor = new CrearAgendaAnualRequestDto(2150);
        Set<ConstraintViolation<CrearAgendaAnualRequestDto>> violations3 = validator.validate(dtoMayor);
        assertThat(violations3).isNotEmpty();

        CrearAgendaAnualRequestDto dtoValido = new CrearAgendaAnualRequestDto(2026);
        Set<ConstraintViolation<CrearAgendaAnualRequestDto>> violationsValido = validator.validate(dtoValido);
        assertThat(violationsValido).isEmpty();
    }

    @Test
    @DisplayName("BrechaHorariaRequestDto valida campos de hora obligatorios")
    void testValidacionBrechaHorariaRequest() {
        BrechaHorariaRequestDto dtoNulo = new BrechaHorariaRequestDto(null, null);
        Set<ConstraintViolation<BrechaHorariaRequestDto>> violations = validator.validate(dtoNulo);
        assertThat(violations).hasSize(2);

        BrechaHorariaRequestDto dtoValido = new BrechaHorariaRequestDto(LocalTime.of(8, 0), LocalTime.of(12, 0));
        Set<ConstraintViolation<BrechaHorariaRequestDto>> violationsValido = validator.validate(dtoValido);
        assertThat(violationsValido).isEmpty();
    }

    @Test
    @DisplayName("ConfigurarModoSemanaRequestDto valida lista no vacía y valida elementos anidados")
    void testValidacionConfigurarModoSemanaRequest() {
        ConfigurarModoSemanaRequestDto dtoVacio = new ConfigurarModoSemanaRequestDto(List.of());
        Set<ConstraintViolation<ConfigurarModoSemanaRequestDto>> violations = validator.validate(dtoVacio);
        assertThat(violations).isNotEmpty();

        DiaSemanaConfiguracionDto diaInvalido = new DiaSemanaConfiguracionDto(null, List.of(new BrechaHorariaRequestDto(null, null)));
        ConfigurarModoSemanaRequestDto dtoAnidadoInvalido = new ConfigurarModoSemanaRequestDto(List.of(diaInvalido));
        Set<ConstraintViolation<ConfigurarModoSemanaRequestDto>> violationsAnidadas = validator.validate(dtoAnidadoInvalido);
        assertThat(violationsAnidadas).isNotEmpty();

        DiaSemanaConfiguracionDto diaValido = new DiaSemanaConfiguracionDto(
                DayOfWeek.MONDAY,
                List.of(new BrechaHorariaRequestDto(LocalTime.of(8, 0), LocalTime.of(12, 0)))
        );
        ConfigurarModoSemanaRequestDto dtoValido = new ConfigurarModoSemanaRequestDto(List.of(diaValido));
        Set<ConstraintViolation<ConfigurarModoSemanaRequestDto>> violationsValido = validator.validate(dtoValido);
        assertThat(violationsValido).isEmpty();
    }

    @Test
    @DisplayName("ConfigurarModoMesRequestDto valida fecha requerida y brechas anidadas")
    void testValidacionConfigurarModoMesRequest() {
        DiaMesConfiguracionDto diaInvalido = new DiaMesConfiguracionDto(null, List.of());
        ConfigurarModoMesRequestDto dtoInvalido = new ConfigurarModoMesRequestDto(List.of(diaInvalido));
        Set<ConstraintViolation<ConfigurarModoMesRequestDto>> violations = validator.validate(dtoInvalido);
        assertThat(violations).isNotEmpty();

        DiaMesConfiguracionDto diaValido = new DiaMesConfiguracionDto(
                LocalDate.of(2026, 8, 15),
                List.of(new BrechaHorariaRequestDto(LocalTime.of(9, 0), LocalTime.of(13, 0)))
        );
        ConfigurarModoMesRequestDto dtoValido = new ConfigurarModoMesRequestDto(List.of(diaValido));
        Set<ConstraintViolation<ConfigurarModoMesRequestDto>> violationsValido = validator.validate(dtoValido);
        assertThat(violationsValido).isEmpty();
    }
}

