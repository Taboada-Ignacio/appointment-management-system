package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.DecisionTurnoAfectadoRequestDto;
import com.apiturnos.agenda.dto.TipoDecisionTurnoAfectado;
import com.apiturnos.agenda.model.AfectacionTurnoExcepcion;
import com.apiturnos.agenda.model.EstadoResolucionAfectacion;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.AfectacionTurnoExcepcionRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.service.PoliticaTransicionesTurno;
import com.apiturnos.turno.service.ReprogramarTurno;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AplicarExcepcionConResoluciones {
    private final ProfesionalRepository profesionalRepository;
    private final ExcepcionAgendaRepository excepcionRepository;
    private final AfectacionTurnoExcepcionRepository afectacionRepository;
    private final ValidadorExcepcionAgenda validador;
    private final EvaluarImpactoExcepcionAgenda evaluarImpacto;
    private final TokenImpactoExcepcionAgenda tokenImpacto;
    private final GestorCambioEstado gestorEstados;
    private final ProcesarBajasTurnosPorExcepcion procesarBajas;
    private final ReprogramarTurno reprogramarTurno;
    private final RegistradorAuditoria auditoria;
    private final DetectorCoincidenciasExcepcionAgenda detectorCoincidencias;
    private final SincronizarEstadoDiasPorExcepcion sincronizarDias;

    public AplicarExcepcionConResoluciones(
            ProfesionalRepository profesionalRepository,
            ExcepcionAgendaRepository excepcionRepository,
            AfectacionTurnoExcepcionRepository afectacionRepository,
            ValidadorExcepcionAgenda validador,
            EvaluarImpactoExcepcionAgenda evaluarImpacto,
            TokenImpactoExcepcionAgenda tokenImpacto,
            GestorCambioEstado gestorEstados,
            ProcesarBajasTurnosPorExcepcion procesarBajas,
            ReprogramarTurno reprogramarTurno,
            RegistradorAuditoria auditoria,
            DetectorCoincidenciasExcepcionAgenda detectorCoincidencias,
            SincronizarEstadoDiasPorExcepcion sincronizarDias) {
        this.profesionalRepository = profesionalRepository;
        this.excepcionRepository = excepcionRepository;
        this.afectacionRepository = afectacionRepository;
        this.validador = validador;
        this.evaluarImpacto = evaluarImpacto;
        this.tokenImpacto = tokenImpacto;
        this.gestorEstados = gestorEstados;
        this.procesarBajas = procesarBajas;
        this.reprogramarTurno = reprogramarTurno;
        this.auditoria = auditoria;
        this.detectorCoincidencias = detectorCoincidencias;
        this.sincronizarDias = sincronizarDias;
    }

    @Transactional
    public ResultadoAplicacionExcepcionAgenda ejecutar(
            Long profesionalId,
            SolicitudExcepcionAgenda solicitud,
            String previewToken,
            List<DecisionTurnoAfectadoRequestDto> decisiones,
            String usuario) {
        validador.validar(solicitud);
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));
        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setProfesional(profesional);
        excepcion.setActiva(true);
        AplicarExcepcionAgenda.copiarDatos(excepcion, solicitud);
        detectorCoincidencias.validar(profesionalId, excepcion);

        List<Turno> afectados = evaluarImpacto.previsualizar(excepcion);
        String tokenActual = tokenImpacto.generar(solicitud, afectados);
        if (previewToken == null || !previewToken.equals(tokenActual)) {
            throw new ConcurrencyFailureException(
                    "El impacto de la excepción cambió. Revise nuevamente los turnos afectados");
        }

        Map<Long, DecisionTurnoAfectadoRequestDto> decisionesPorTurno = validarDecisiones(afectados, decisiones);
        excepcion = excepcionRepository.save(excepcion);
        Map<Long, AfectacionTurnoExcepcion> relaciones = registrarAfectaciones(
                excepcion, afectados, decisionesPorTurno, usuario);

        List<Turno> paraBaja = afectados.stream()
                .filter(t -> decision(decisionesPorTurno, t.getId()) == TipoDecisionTurnoAfectado.DAR_DE_BAJA)
                .toList();
        procesarBajas.ejecutar(excepcion, paraBaja, usuario);
        paraBaja.forEach(turno -> resolver(relaciones.get(turno.getId()), EstadoResolucionAfectacion.DADO_DE_BAJA));

        for (Turno turno : afectados) {
            DecisionTurnoAfectadoRequestDto elegida = decisionesPorTurno.get(turno.getId());
            if (elegida != null && elegida.decision() == TipoDecisionTurnoAfectado.REPROGRAMAR) {
                validarReprogramacion(elegida);
                reprogramarTurno.ejecutar(
                        turno.getId(), elegida.nuevoDiaAgendaId(), elegida.nuevoInicio(), elegida.nuevoFin(),
                        motivo(excepcion, elegida.observacion()), usuario);
                resolver(relaciones.get(turno.getId()), EstadoResolucionAfectacion.REPROGRAMADO);
            }
        }

        sincronizarDias.reconciliar(profesionalId,
                SincronizarEstadoDiasPorExcepcion.fechasEfectivas(excepcion), usuario);

        auditoria.registrar("AGENDA", "ExcepcionAgenda", excepcion.getId(), OperacionAuditoria.CREATE,
                usuario, profesionalId, "EXCEPCION_CON_RESOLUCIONES: afectados=" + afectados.size()
                        + "; bajas=" + paraBaja.size());
        return new ResultadoAplicacionExcepcionAgenda(excepcion, afectados);
    }

    private Map<Long, DecisionTurnoAfectadoRequestDto> validarDecisiones(
            List<Turno> afectados, List<DecisionTurnoAfectadoRequestDto> decisiones) {
        Set<Long> ids = afectados.stream().map(Turno::getId).collect(Collectors.toSet());
        Map<Long, DecisionTurnoAfectadoRequestDto> resultado = new HashMap<>();
        if (decisiones == null) return resultado;
        for (DecisionTurnoAfectadoRequestDto decision : decisiones) {
            if (!ids.contains(decision.turnoId())) {
                throw new IllegalArgumentException("El turno " + decision.turnoId() + " no pertenece al impacto vigente");
            }
            if (resultado.put(decision.turnoId(), decision) != null) {
                throw new IllegalArgumentException("El turno " + decision.turnoId() + " tiene decisiones duplicadas");
            }
        }
        return resultado;
    }

    private Map<Long, AfectacionTurnoExcepcion> registrarAfectaciones(
            ExcepcionAgenda excepcion, List<Turno> turnos,
            Map<Long, DecisionTurnoAfectadoRequestDto> decisiones, String usuario) {
        Map<Long, AfectacionTurnoExcepcion> resultado = new HashMap<>();
        for (Turno turno : turnos) {
            AfectacionTurnoExcepcion afectacion = new AfectacionTurnoExcepcion();
            afectacion.setExcepcionAgenda(excepcion);
            afectacion.setTurno(turno);
            afectacion.setEstadoTurnoAnterior(
                    gestorEstados.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turno.getId()));
            afectacion.setDiaAgendaAnterior(turno.getDiaAgenda());
            afectacion.setInicioAnterior(turno.getInicioEstimado());
            afectacion.setFinAnterior(turno.getFinEstimado());
            DecisionTurnoAfectadoRequestDto elegida = decisiones.get(turno.getId());
            afectacion.setObservacion(elegida == null ? null : elegida.observacion());
            afectacion.setEstadoResolucion(EstadoResolucionAfectacion.PENDIENTE);
            afectacion = afectacionRepository.save(afectacion);
            resultado.put(turno.getId(), afectacion);
            if (decision(decisiones, turno.getId()) == TipoDecisionTurnoAfectado.PENDIENTE) {
                gestorEstados.registrarCambio(AmbitoEstado.TURNO, turno.getId(),
                        PoliticaTransicionesTurno.AFECTADO_POR_EXCEPCION, usuario,
                        "Pendiente de resolución por excepción " + excepcion.getId(), null);
            }
        }
        return resultado;
    }

    private TipoDecisionTurnoAfectado decision(
            Map<Long, DecisionTurnoAfectadoRequestDto> decisiones, Long turnoId) {
        DecisionTurnoAfectadoRequestDto decision = decisiones.get(turnoId);
        return decision == null ? TipoDecisionTurnoAfectado.PENDIENTE : decision.decision();
    }

    private void validarReprogramacion(DecisionTurnoAfectadoRequestDto d) {
        if (d.nuevoDiaAgendaId() == null || d.nuevoInicio() == null || d.nuevoFin() == null) {
            throw new IllegalArgumentException("Reprogramar requiere nuevo día, inicio y fin");
        }
    }

    private void resolver(AfectacionTurnoExcepcion afectacion, EstadoResolucionAfectacion estado) {
        afectacion.setEstadoResolucion(estado);
        afectacion.setResueltoEn(Instant.now());
        afectacionRepository.save(afectacion);
    }

    private String motivo(ExcepcionAgenda excepcion, String observacion) {
        return "Excepción de agenda " + excepcion.getTipo() + ": " + excepcion.getMotivo()
                + (observacion == null || observacion.isBlank() ? "" : ". " + observacion.trim());
    }
}
