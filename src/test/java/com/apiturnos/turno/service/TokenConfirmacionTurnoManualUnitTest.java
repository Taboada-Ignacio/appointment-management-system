package com.apiturnos.turno.service;

import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.ConfirmacionAdvertenciaTurno;
import com.apiturnos.turno.repository.ConfirmacionAdvertenciaTurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenConfirmacionTurnoManualUnitTest {
    @Mock ConfirmacionAdvertenciaTurnoRepository repository;
    private TokenConfirmacionTurnoManual tokens;
    private SolicitudCrearTurnoManual solicitud;
    private final AtomicReference<ConfirmacionAdvertenciaTurno> guardada = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-07T15:00:00Z"), ZoneOffset.UTC);
        tokens = new TokenConfirmacionTurnoManual(repository, clock,
                "secreto-de-prueba-con-mas-de-32-caracteres", Duration.ofMinutes(5));
        solicitud = new SolicitudCrearTurnoManual(1L, 2L, 3L, 4L,
                Instant.parse("2026-09-08T12:00:00Z"), Instant.parse("2026-09-08T12:30:00Z"),
                false, null, "profesional");
        when(repository.save(any())).thenAnswer(inv -> {
            guardada.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    @Test
    void tokenValidoSeConsumeUnaSolaVez() {
        String token = tokens.emitir(solicitud, List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA), 1, 1);
        when(repository.findByIdForUpdate(guardada.get().getId())).thenReturn(Optional.of(guardada.get()));

        tokens.validarYConsumir(token, solicitud,
                List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA), 1, 1);

        assertThat(guardada.get().isUsada()).isTrue();
        assertThatThrownBy(() -> tokens.validarYConsumir(token, solicitud,
                List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA), 1, 1))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void cambioDeCondicionesInvalidaElToken() {
        String token = tokens.emitir(solicitud, List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA), 1, 1);
        when(repository.findByIdForUpdate(guardada.get().getId())).thenReturn(Optional.of(guardada.get()));

        assertThatThrownBy(() -> tokens.validarYConsumir(token, solicitud,
                List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA), 1, 2))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("condiciones cambiaron");
    }
}
