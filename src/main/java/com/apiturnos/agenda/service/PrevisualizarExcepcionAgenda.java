package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.ExcepcionAgendaInvalidaException;
import com.apiturnos.turno.model.Turno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrevisualizarExcepcionAgenda {

    private final ProfesionalRepository profesionalRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final ValidadorExcepcionAgenda validador;
    private final EvaluarImpactoExcepcionAgenda evaluarImpacto;
    private final DetectorCoincidenciasExcepcionAgenda detectorCoincidencias;

    public PrevisualizarExcepcionAgenda(
            ProfesionalRepository profesionalRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            ValidadorExcepcionAgenda validador,
            EvaluarImpactoExcepcionAgenda evaluarImpacto,
            DetectorCoincidenciasExcepcionAgenda detectorCoincidencias) {
        this.profesionalRepository = profesionalRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.validador = validador;
        this.evaluarImpacto = evaluarImpacto;
        this.detectorCoincidencias = detectorCoincidencias;
    }

    @Transactional(readOnly = true)
    public List<Turno> nueva(Long profesionalId, SolicitudExcepcionAgenda solicitud) {
        validador.validar(solicitud);
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));
        ExcepcionAgenda candidata = construirCandidata(null, profesional, solicitud);
        detectorCoincidencias.validar(profesionalId, candidata);
        return evaluarImpacto.previsualizar(candidata);
    }

    @Transactional(readOnly = true)
    public List<Turno> modificacion(
            Long profesionalId,
            Long excepcionId,
            SolicitudExcepcionAgenda solicitud) {
        validador.validar(solicitud);
        ExcepcionAgenda existente = excepcionAgendaRepository
                .findByIdAndProfesionalId(excepcionId, profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("ExcepcionAgenda", excepcionId));
        if (!existente.isActiva()) {
            throw new ExcepcionAgendaInvalidaException("No se puede modificar una excepción cancelada");
        }
        ExcepcionAgenda candidata = construirCandidata(excepcionId, existente.getProfesional(), solicitud);
        detectorCoincidencias.validar(profesionalId, candidata);
        return evaluarImpacto.previsualizar(candidata);
    }

    private ExcepcionAgenda construirCandidata(
            Long id,
            Profesional profesional,
            SolicitudExcepcionAgenda solicitud) {
        ExcepcionAgenda candidata = new ExcepcionAgenda();
        candidata.setId(id);
        candidata.setProfesional(profesional);
        candidata.setActiva(true);
        AplicarExcepcionAgenda.copiarDatos(candidata, solicitud);
        return candidata;
    }
}
