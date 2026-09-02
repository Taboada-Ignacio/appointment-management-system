package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivarInactivarDiaAgenda {

    private final DiaAgendaRepository diaAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public ActivarInactivarDiaAgenda(DiaAgendaRepository diaAgendaRepository,
                                     GestorCambioEstado gestorCambioEstado,
                                     RegistradorAuditoria registradorAuditoria) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public void activar(Long profesionalId, Long diaAgendaId, String usuario) {
        DiaAgenda dia = diaAgendaRepository.findById(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));

        if (profesionalId != null && !dia.getMesAgenda().getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(diaAgendaId, profesionalId);
        }

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, diaAgendaId);
        if (estadoActual == null) {
            gestorCambioEstado.registrarCambioInicial(
                    AmbitoEstado.DIA_AGENDA, diaAgendaId, "ACTIVO", usuario, "Día activado");
        } else if (!"ACTIVO".equals(estadoActual)) {
            gestorCambioEstado.registrarCambio(
                    AmbitoEstado.DIA_AGENDA, diaAgendaId, "ACTIVO", usuario, "Día activado", null);
        }

        Long profId = dia.getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("AGENDA", "DiaAgenda", diaAgendaId,
                OperacionAuditoria.STATE_CHANGE, usuario, profId, "Día activado");
    }

    @Transactional
    public void inactivar(Long profesionalId, Long diaAgendaId, String usuario) {
        DiaAgenda dia = diaAgendaRepository.findById(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));

        if (profesionalId != null && !dia.getMesAgenda().getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(diaAgendaId, profesionalId);
        }

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, diaAgendaId);
        if (estadoActual == null) {
            gestorCambioEstado.registrarCambioInicial(
                    AmbitoEstado.DIA_AGENDA, diaAgendaId, "INACTIVO", usuario, "Día inactivado");
        } else if (!"INACTIVO".equals(estadoActual)) {
            gestorCambioEstado.registrarCambio(
                    AmbitoEstado.DIA_AGENDA, diaAgendaId, "INACTIVO", usuario, "Día inactivado", null);
        }

        Long profId = dia.getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("AGENDA", "DiaAgenda", diaAgendaId,
                OperacionAuditoria.STATE_CHANGE, usuario, profId, "Día inactivado");
    }
}

