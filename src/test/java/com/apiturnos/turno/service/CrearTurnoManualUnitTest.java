package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.MotivoRechazoTurnoManual;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearTurnoManualUnitTest {

    @Mock private ValidadorCrearTurnoManual validador;
    @Mock private TurnoRepository turnoRepository;
    @Mock private GestorCambioEstado gestorCambioEstado;
    @Mock private RegistradorAuditoria registradorAuditoria;
    @Mock private RegistradorNotificacion registradorNotificacion;

    private CrearTurnoManual casoDeUso;
    private DiaAgenda dia;
    private Cliente cliente;
    private TipoAtencion tipo;
    private DatosConfirmacionTurnoManual datos;

    @BeforeEach
    void setUp() {
        casoDeUso = new CrearTurnoManual(
                validador,
                turnoRepository,
                gestorCambioEstado,
                registradorAuditoria,
                registradorNotificacion);

        dia = new DiaAgenda();
        dia.setId(10L);
        dia.setFecha(LocalDate.of(2026, 9, 10));

        cliente = new Cliente();
        cliente.setId(20L);
        cliente.setNombre("Ana");
        cliente.setApellido("Pérez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("30111222");
        cliente.setTelefono("+5491112345678");
        cliente.setNotificacionesHabilitadas(true);

        tipo = new TipoAtencion();
        tipo.setId(30L);
        tipo.setNombre("Consulta");

        datos = new DatosConfirmacionTurnoManual(
                cliente.getId(), cliente.getNombre(), cliente.getApellido(),
                cliente.getTipoDocumento(), cliente.getNumeroDocumento(), dia.getFecha(),
                LocalTime.of(9, 0), LocalTime.of(9, 30), tipo.getId(), tipo.getNombre());

        lenient().when(turnoRepository.save(any(Turno.class))).thenAnswer(invocacion -> {
            Turno turno = invocacion.getArgument(0);
            turno.setId(100L);
            return turno;
        });
    }

    @Test
    void dentroDeBrechaSinAdvertenciasCreaTurno() {
        SolicitudCrearTurnoManual solicitud = solicitud(false);
        when(validador.validar(solicitud)).thenReturn(contexto(List.of()));

        ResultadoCrearTurnoManual resultado = casoDeUso.ejecutar(solicitud);

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.requiereConfirmacion()).isFalse();
        assertThat(resultado.advertencias()).isEmpty();
        assertThat(resultado.turnoId()).isEqualTo(100L);

        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        verify(turnoRepository).save(captor.capture());
        assertThat(captor.getValue().getDiaAgenda()).isSameAs(dia);
        assertThat(captor.getValue().getCliente()).isSameAs(cliente);
        assertThat(captor.getValue().getTipoAtencion()).isSameAs(tipo);
        assertThat(captor.getValue().getOrigen()).isEqualTo(OrigenTurno.PROFESIONAL);
    }

    @Test
    void fueraDeBrechaSinConfirmacionNoPersiste() {
        SolicitudCrearTurnoManual solicitud = solicitud(false);
        when(validador.validar(solicitud)).thenReturn(
                contexto(List.of(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA)));

        ResultadoCrearTurnoManual resultado = casoDeUso.ejecutar(solicitud);

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.puedeCrear()).isTrue();
        assertThat(resultado.requiereConfirmacion()).isTrue();
        assertThat(resultado.datosConfirmacion().numeroDocumento()).isEqualTo("30111222");
        verify(turnoRepository, never()).save(any());
        verifyNoInteractions(gestorCambioEstado, registradorAuditoria, registradorNotificacion);
    }

    @Test
    void fueraDeBrechaConfirmadoPersiste() {
        SolicitudCrearTurnoManual solicitud = solicitud(true);
        when(validador.validar(solicitud)).thenReturn(
                contexto(List.of(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA)));

        ResultadoCrearTurnoManual resultado = casoDeUso.ejecutar(solicitud);

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.advertencias())
                .containsExactly(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA);
        verify(turnoRepository).save(any(Turno.class));
    }

    @Test
    void sobrecapacidadSinConfirmacionNoPersiste() {
        SolicitudCrearTurnoManual solicitud = solicitud(false);
        when(validador.validar(solicitud)).thenReturn(
                contexto(List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA)));

        ResultadoCrearTurnoManual resultado = casoDeUso.ejecutar(solicitud);

        assertThat(resultado.requiereConfirmacion()).isTrue();
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void sobrecapacidadConfirmadaPersiste() {
        SolicitudCrearTurnoManual solicitud = solicitud(true);
        when(validador.validar(solicitud)).thenReturn(
                contexto(List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA)));

        ResultadoCrearTurnoManual resultado = casoDeUso.ejecutar(solicitud);

        assertThat(resultado.creado()).isTrue();
        verify(turnoRepository).save(any(Turno.class));
    }

    @Test
    void devuelveMultiplesAdvertenciasEnUnaSolaPrevalidacion() {
        SolicitudCrearTurnoManual solicitud = solicitud(false);
        List<AdvertenciaTurnoManual> advertencias = List.of(
                AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA,
                AdvertenciaTurnoManual.CAPACIDAD_SUPERADA);
        when(validador.validar(solicitud)).thenReturn(contexto(advertencias));

        ResultadoCrearTurnoManual resultado = casoDeUso.ejecutar(solicitud);

        assertThat(resultado.advertencias()).containsExactlyElementsOf(advertencias);
        assertThat(resultado.requiereConfirmacion()).isTrue();
    }

    @Test
    void confirmacionRevalidaYNoConfiaEnElResultadoAnterior() {
        SolicitudCrearTurnoManual prevalidacion = solicitud(false);
        SolicitudCrearTurnoManual confirmacion = solicitud(true);
        when(validador.validar(prevalidacion)).thenReturn(
                contexto(List.of(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA)));
        when(validador.validar(confirmacion)).thenThrow(new TurnoManualNoPermitidoException(
                MotivoRechazoTurnoManual.HORARIO_BLOQUEADO_POR_EXCEPCION,
                "Se agregó un bloqueo entre ambas operaciones"));

        assertThat(casoDeUso.ejecutar(prevalidacion).requiereConfirmacion()).isTrue();
        assertThatThrownBy(() -> casoDeUso.ejecutar(confirmacion))
                .isInstanceOf(TurnoManualNoPermitidoException.class);

        verify(validador).validar(prevalidacion);
        verify(validador).validar(confirmacion);
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void creacionRegistraCambioEstadoInicialAsignado() {
        SolicitudCrearTurnoManual solicitud = solicitud(false);
        when(validador.validar(solicitud)).thenReturn(contexto(List.of()));

        casoDeUso.ejecutar(solicitud);

        verify(gestorCambioEstado).registrarCambioInicial(
                AmbitoEstado.TURNO,
                100L,
                "ASIGNADO",
                "profesional@test",
                "Turno creado manualmente por el profesional");
    }

    @Test
    void creacionRegistraAuditoriaConAdvertenciasConfirmadas() {
        SolicitudCrearTurnoManual solicitud = solicitud(true);
        when(validador.validar(solicitud)).thenReturn(
                contexto(List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA)));

        casoDeUso.ejecutar(solicitud);

        verify(registradorAuditoria).registrar(
                eq("TURNO"), eq("Turno"), eq(100L), eq(OperacionAuditoria.CREATE),
                eq("profesional@test"), eq(1L),
                contains("CAPACIDAD_SUPERADA"));
    }

    @Test
    void creacionDelegaPersistenciaDeNotificacionSinEnvioExterno() {
        SolicitudCrearTurnoManual solicitud = solicitud(false);
        when(validador.validar(solicitud)).thenReturn(contexto(List.of()));

        casoDeUso.ejecutar(solicitud);

        verify(registradorNotificacion).registrarSiCorresponde(
                eq(cliente), any(Turno.class), eq(TipoNotificacion.CONFIRMACION_TURNO),
                contains("09:00"));
    }

    private ValidadorCrearTurnoManual.ContextoValidado contexto(
            List<AdvertenciaTurnoManual> advertencias) {
        return new ValidadorCrearTurnoManual.ContextoValidado(
                dia, cliente, tipo, advertencias, datos);
    }

    private SolicitudCrearTurnoManual solicitud(boolean confirmar) {
        return new SolicitudCrearTurnoManual(
                1L,
                dia.getId(),
                cliente.getId(),
                tipo.getId(),
                Instant.parse("2026-09-10T09:00:00Z"),
                Instant.parse("2026-09-10T09:30:00Z"),
                confirmar,
                "Control",
                "profesional@test");
    }
}
