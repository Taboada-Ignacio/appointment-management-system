package com.apiturnos.estado.service;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.model.Estado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.repository.EstadoRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.TransicionEstadoInvalidaException;
import com.apiturnos.turno.model.MotivoBajaTurno;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GestorCambioEstado {

    private final EstadoRepository estadoRepository;
    private final CambioEstadoRepository cambioEstadoRepository;

    private static final Map<AmbitoEstado, Map<String, Set<String>>> TRANSICIONES = Map.of(
        AmbitoEstado.CLIENTE, Map.of(
            "PENDIENTE_DE_VERIFICACION", Set.of("HABILITADO"),
            "HABILITADO", Set.of("INHABILITADO", "DADO_DE_BAJA"),
            "INHABILITADO", Set.of("HABILITADO", "DADO_DE_BAJA"),
            "DADO_DE_BAJA", Set.of("HABILITADO", "PENDIENTE_DE_VERIFICACION", "INHABILITADO", "REQUIERE_APROBACION")
        ),
        AmbitoEstado.TURNO, Map.of(
            "PENDIENTE_DE_APROBACION", Set.of("ASIGNADO", "CANCELADO"),
            "ASIGNADO", Set.of("REPROGRAMADO", "CANCELADO", "COMPLETADO", "NO_ASISTIO", "DADO_DE_BAJA", "CONFIRMADO"),
            "REPROGRAMADO", Set.of("ASIGNADO"),
            "CONFIRMADO", Set.of("CANCELADO", "COMPLETADO", "NO_ASISTIO")
        ),
        AmbitoEstado.MES_AGENDA, Map.of(
            "ACTIVO", Set.of("INACTIVO"),
            "INACTIVO", Set.of("ACTIVO")
        ),
        AmbitoEstado.DIA_AGENDA, Map.of(
            "ACTIVO", Set.of("INACTIVO", "EN_TRANSCURSO", "FINALIZADO"),
            "INACTIVO", Set.of("ACTIVO"),
            "EN_TRANSCURSO", Set.of("FINALIZADO")
        )
    );

    public GestorCambioEstado(EstadoRepository estadoRepository, CambioEstadoRepository cambioEstadoRepository) {
        this.estadoRepository = estadoRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
    }

    public Optional<CambioEstado> obtenerCambioEstadoActual(AmbitoEstado ambito, Long entidadId) {
        return cambioEstadoRepository.findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDesc(ambito, entidadId);
    }

    public String obtenerNombreEstadoActual(AmbitoEstado ambito, Long entidadId) {
        return obtenerCambioEstadoActual(ambito, entidadId)
                .map(ce -> ce.getEstado().getNombre())
                .orElse(null);
    }

    public List<CambioEstado> obtenerHistorial(AmbitoEstado ambito, Long entidadId) {
        return cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(ambito, entidadId);
    }

    public CambioEstado registrarCambioInicial(AmbitoEstado ambito, Long entidadId, String nombreEstado,
                                                String usuario, String observacion) {
        Estado estado = buscarEstado(nombreEstado, ambito);
        CambioEstado cambio = new CambioEstado();
        cambio.setEstado(estado);
        cambio.setAmbito(ambito);
        cambio.setEntidadId(entidadId);
        cambio.setFechaHoraInicio(Instant.now());
        cambio.setUsuario(usuario);
        cambio.setObservacion(observacion);
        return cambioEstadoRepository.save(cambio);
    }

    public CambioEstado registrarCambio(AmbitoEstado ambito, Long entidadId, String nombreEstadoDestino,
                                         String usuario, String observacion, MotivoBajaTurno motivo) {
        String estadoActual = obtenerNombreEstadoActual(ambito, entidadId);
        if (estadoActual == null) {
            throw new EstadoInvalidoException("No existe estado actual para " + ambito + " con id " + entidadId);
        }
        validarTransicion(ambito, estadoActual, nombreEstadoDestino);
        finalizarCambioAnterior(ambito, entidadId);

        Estado estadoDestino = buscarEstado(nombreEstadoDestino, ambito);
        CambioEstado cambio = new CambioEstado();
        cambio.setEstado(estadoDestino);
        cambio.setAmbito(ambito);
        cambio.setEntidadId(entidadId);
        cambio.setFechaHoraInicio(Instant.now());
        cambio.setUsuario(usuario);
        cambio.setObservacion(observacion);
        cambio.setMotivoBajaTurno(motivo);
        return cambioEstadoRepository.save(cambio);
    }

    public void validarTransicion(AmbitoEstado ambito, String estadoActual, String estadoDestino) {
        Map<String, Set<String>> transiciones = TRANSICIONES.get(ambito);
        if (transiciones == null) {
            throw new TransicionEstadoInvalidaException(estadoActual, estadoDestino, ambito.name());
        }
        Set<String> destinos = transiciones.get(estadoActual);
        if (destinos == null || !destinos.contains(estadoDestino)) {
            throw new TransicionEstadoInvalidaException(estadoActual, estadoDestino, ambito.name());
        }
    }

    private void finalizarCambioAnterior(AmbitoEstado ambito, Long entidadId) {
        obtenerCambioEstadoActual(ambito, entidadId).ifPresent(anterior -> {
            if (anterior.getFechaHoraFin() == null) {
                anterior.setFechaHoraFin(Instant.now());
                cambioEstadoRepository.save(anterior);
            }
        });
    }

    private Estado buscarEstado(String nombre, AmbitoEstado ambito) {
        return estadoRepository.findByNombreAndAmbito(nombre, ambito)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Estado '" + nombre + "' con ámbito '" + ambito + "' no encontrado en el catálogo"));
    }
}
