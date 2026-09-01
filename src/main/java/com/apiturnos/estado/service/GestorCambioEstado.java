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
import com.apiturnos.turno.service.PoliticaTransicionesTurno;
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
    private final PoliticaTransicionesTurno politicaTransicionesTurno;

    private static final Map<AmbitoEstado, Map<String, Set<String>>> TRANSICIONES = Map.of(
        AmbitoEstado.CLIENTE, Map.of(
            "PENDIENTE_DE_VERIFICACION", Set.of("HABILITADO", "DADO_DE_BAJA"),
            "HABILITADO", Set.of("REQUIERE_APROBACION", "INHABILITADO", "DADO_DE_BAJA"),
            "REQUIERE_APROBACION", Set.of("HABILITADO", "INHABILITADO", "DADO_DE_BAJA"),
            "INHABILITADO", Set.of("HABILITADO", "REQUIERE_APROBACION", "DADO_DE_BAJA"),
            "DADO_DE_BAJA", Set.of("HABILITADO", "PENDIENTE_DE_VERIFICACION", "INHABILITADO", "REQUIERE_APROBACION")
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

    public GestorCambioEstado(EstadoRepository estadoRepository,
                              CambioEstadoRepository cambioEstadoRepository,
                              PoliticaTransicionesTurno politicaTransicionesTurno) {
        this.estadoRepository = estadoRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.politicaTransicionesTurno = politicaTransicionesTurno;
    }

    public Optional<CambioEstado> obtenerCambioEstadoActual(AmbitoEstado ambito, Long entidadId) {
        return cambioEstadoRepository
                .findFirstByAmbitoAndEntidadIdAndFechaHoraFinIsNullOrderByIdDesc(ambito, entidadId)
                .or(() -> cambioEstadoRepository.findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDescIdDesc(ambito, entidadId));
    }

    public String obtenerNombreEstadoActual(AmbitoEstado ambito, Long entidadId) {
        return obtenerCambioEstadoActual(ambito, entidadId)
                .map(ce -> ce.getEstado().getNombre())
                .orElse(null);
    }

    public List<CambioEstado> obtenerHistorial(AmbitoEstado ambito, Long entidadId) {
        return cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAscIdAsc(ambito, entidadId);
    }

    public String obtenerEstadoAnteriorADadoDeBaja(AmbitoEstado ambito, Long entidadId) {
        List<CambioEstado> historial = obtenerHistorial(ambito, entidadId);
        String estadoAnterior = "HABILITADO"; // fallback por defecto
        for (int i = historial.size() - 1; i >= 0; i--) {
            String nombre = historial.get(i).getEstado().getNombre();
            if (!"DADO_DE_BAJA".equals(nombre)) {
                estadoAnterior = nombre;
                break;
            }
        }
        return estadoAnterior;
    }

    public Map<Long, String> obtenerEstadosActualesPorEntidades(AmbitoEstado ambito, List<Long> entidadIds) {
        if (entidadIds == null || entidadIds.isEmpty()) {
            return Map.of();
        }
        List<CambioEstado> activos = cambioEstadoRepository.findCurrentByAmbitoAndEntidadIds(ambito, entidadIds);
        Map<Long, String> resultado = new java.util.HashMap<>();
        for (CambioEstado ce : activos) {
            resultado.put(ce.getEntidadId(), ce.getEstado().getNombre());
        }
        // Para entidades sin fechaHoraFin null (si hubiera alguna), fallback individual determinístico
        for (Long id : entidadIds) {
            if (!resultado.containsKey(id)) {
                String actual = obtenerNombreEstadoActual(ambito, id);
                if (actual != null) {
                    resultado.put(id, actual);
                }
            }
        }
        return resultado;
    }

    public CambioEstado registrarCambioInicial(AmbitoEstado ambito, Long entidadId, String nombreEstado,
                                                String usuario, String observacion) {
        if (obtenerCambioEstadoActual(ambito, entidadId).isPresent()) {
            throw new EstadoInvalidoException(
                    "Ya existe un estado para " + ambito + " con id " + entidadId);
        }
        if (ambito == AmbitoEstado.TURNO) {
            politicaTransicionesTurno.validarEstadoInicial(nombreEstado, entidadId);
        }
        Estado estado = buscarEstado(nombreEstado, ambito);
        CambioEstado cambio = new CambioEstado();
        cambio.setEstado(estado);
        cambio.setAmbito(ambito);
        cambio.setEntidadId(entidadId);
        cambio.setFechaHoraInicio(Instant.now());
        cambio.setUsuario(usuario);
        cambio.setObservacion(observacion);
        CambioEstado guardado = cambioEstadoRepository.save(cambio);
        cambioEstadoRepository.flush();
        return guardado;
    }

    public CambioEstado registrarCambio(AmbitoEstado ambito, Long entidadId, String nombreEstadoDestino,
                                         String usuario, String observacion, MotivoBajaTurno motivo) {
        String estadoActual = obtenerNombreEstadoActual(ambito, entidadId);
        if (estadoActual == null) {
            throw new EstadoInvalidoException("No existe estado actual para " + ambito + " con id " + entidadId);
        }
        validarTransicion(ambito, entidadId, estadoActual, nombreEstadoDestino, motivo);
        Instant instanteTransicion = Instant.now();
        finalizarCambioAnterior(ambito, entidadId, instanteTransicion);

        Estado estadoDestino = buscarEstado(nombreEstadoDestino, ambito);
        CambioEstado cambio = new CambioEstado();
        cambio.setEstado(estadoDestino);
        cambio.setAmbito(ambito);
        cambio.setEntidadId(entidadId);
        cambio.setFechaHoraInicio(instanteTransicion);
        cambio.setUsuario(usuario);
        cambio.setObservacion(observacion);
        cambio.setMotivoBajaTurno(motivo);
        CambioEstado guardado = cambioEstadoRepository.save(cambio);
        cambioEstadoRepository.flush();
        return guardado;
    }

    public void validarTransicion(AmbitoEstado ambito, String estadoActual, String estadoDestino) {
        if (ambito == AmbitoEstado.TURNO) {
            politicaTransicionesTurno.validarTransicion(null, estadoActual, estadoDestino);
            return;
        }
        validarTransicion(ambito, null, estadoActual, estadoDestino, null);
    }

    private void validarTransicion(AmbitoEstado ambito, Long entidadId, String estadoActual,
                                   String estadoDestino, MotivoBajaTurno motivo) {
        if (ambito == AmbitoEstado.TURNO) {
            politicaTransicionesTurno.validarTransicion(
                    entidadId, estadoActual, estadoDestino, motivo);
            return;
        }
        Map<String, Set<String>> transiciones = TRANSICIONES.get(ambito);
        if (transiciones == null) {
            throw new TransicionEstadoInvalidaException(estadoActual, estadoDestino, ambito.name());
        }
        Set<String> destinos = transiciones.get(estadoActual);
        if (destinos == null || !destinos.contains(estadoDestino)) {
            throw new TransicionEstadoInvalidaException(estadoActual, estadoDestino, ambito.name());
        }
    }

    private void finalizarCambioAnterior(AmbitoEstado ambito, Long entidadId, Instant fechaHoraFin) {
        obtenerCambioEstadoActual(ambito, entidadId).ifPresent(anterior -> {
            if (anterior.getFechaHoraFin() == null) {
                anterior.setFechaHoraFin(fechaHoraFin);
                cambioEstadoRepository.save(anterior);
                cambioEstadoRepository.flush();
            }
        });
    }

    private Estado buscarEstado(String nombre, AmbitoEstado ambito) {
        return estadoRepository.findByNombreAndAmbito(nombre, ambito)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Estado '" + nombre + "' con ámbito '" + ambito + "' no encontrado en el catálogo"));
    }
}
