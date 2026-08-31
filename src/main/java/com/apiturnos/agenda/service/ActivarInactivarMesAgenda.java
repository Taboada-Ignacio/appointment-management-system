package com.apiturnos.agenda.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivarInactivarMesAgenda {

    private final MesAgendaRepository mesAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public ActivarInactivarMesAgenda(MesAgendaRepository mesAgendaRepository,
                                      GestorCambioEstado gestorCambioEstado,
                                      RegistradorAuditoria registradorAuditoria) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public void activar(Long profesionalId, Long mesAgendaId, String usuario) {
        MesAgenda mes = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (profesionalId != null && !mes.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mesAgendaId);
        if (estadoActual == null) {
            gestorCambioEstado.registrarCambioInicial(
                    AmbitoEstado.MES_AGENDA, mesAgendaId, "ACTIVO", usuario, "Mes activado");
        } else {
            gestorCambioEstado.registrarCambio(
                    AmbitoEstado.MES_AGENDA, mesAgendaId, "ACTIVO", usuario, "Mes activado", null);
        }

        Long profId = mes.getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("AGENDA", "MesAgenda", mesAgendaId,
                OperacionAuditoria.STATE_CHANGE, usuario, profId, "Mes activado");
    }

    @Transactional
    public void activar(Long mesAgendaId, String usuario) {
        activar(null, mesAgendaId, usuario);
    }

    @Transactional
    public void inactivar(Long profesionalId, Long mesAgendaId, String usuario) {
        MesAgenda mes = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (profesionalId != null && !mes.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mesAgendaId);
        if (estadoActual == null) {
            gestorCambioEstado.registrarCambioInicial(
                    AmbitoEstado.MES_AGENDA, mesAgendaId, "INACTIVO", usuario, "Mes inactivado");
        } else {
            gestorCambioEstado.registrarCambio(
                    AmbitoEstado.MES_AGENDA, mesAgendaId, "INACTIVO", usuario, "Mes inactivado", null);
        }

        Long profId = mes.getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("AGENDA", "MesAgenda", mesAgendaId,
                OperacionAuditoria.STATE_CHANGE, usuario, profId, "Mes inactivado");
    }

    @Transactional
    public void inactivar(Long mesAgendaId, String usuario) {
        inactivar(null, mesAgendaId, usuario);
    }
}
