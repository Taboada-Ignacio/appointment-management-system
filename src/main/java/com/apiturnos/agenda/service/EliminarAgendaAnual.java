package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Year;
import java.util.List;

@Service
public class EliminarAgendaAnual {

    private final AgendaAnualRepository agendaAnualRepository;
    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final TurnoRepository turnoRepository;
    private final TurnoHistorialRepository turnoHistorialRepository;
    private final NotificacionRepository notificacionRepository;
    private final CambioEstadoRepository cambioEstadoRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;
    private final EntityManager entityManager;
    private final Clock clock;

    public EliminarAgendaAnual(AgendaAnualRepository agendaAnualRepository,
                               MesAgendaRepository mesAgendaRepository,
                               DiaAgendaRepository diaAgendaRepository,
                               BrechaHorariaRepository brechaHorariaRepository,
                               TurnoRepository turnoRepository,
                               TurnoHistorialRepository turnoHistorialRepository,
                               NotificacionRepository notificacionRepository,
                               CambioEstadoRepository cambioEstadoRepository,
                               ProfesionalRepository profesionalRepository,
                               RegistradorAuditoria registradorAuditoria,
                               EntityManager entityManager,
                               Clock clock) {
        this.agendaAnualRepository = agendaAnualRepository;
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.turnoRepository = turnoRepository;
        this.turnoHistorialRepository = turnoHistorialRepository;
        this.notificacionRepository = notificacionRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public void ejecutarAnioActual(Long profesionalId, String usuario) {
        int anioActual = Year.now(clock).getValue();
        ejecutar(profesionalId, anioActual, usuario);
    }

    @Transactional
    public void ejecutar(Long profesionalId, Integer anio, String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        final int anioEfectivo = (anio != null) ? anio : Year.now(clock).getValue();

        if (!profesionalRepository.existsById(profesionalId)) {
            throw new EntidadNoEncontradaException("Profesional", profesionalId);
        }

        AgendaAnual agenda = agendaAnualRepository.findByProfesionalIdAndAnio(profesionalId, anioEfectivo)
                .orElseThrow(() -> new EntidadNoEncontradaException("AgendaAnual para año " + anioEfectivo + " del profesional " + profesionalId + " no encontrada"));

        Long agendaId = agenda.getId();

        List<Long> mesAgendaIds = mesAgendaRepository.findIdsByAgendaAnualId(agendaId);
        if (!mesAgendaIds.isEmpty()) {
            List<Long> diaAgendaIds = diaAgendaRepository.findIdsByMesAgendaIdIn(mesAgendaIds);

            if (!diaAgendaIds.isEmpty()) {
                List<Long> turnoIds = turnoRepository.findIdsByDiaAgendaIdIn(diaAgendaIds);

                if (!turnoIds.isEmpty()) {
                    // 1. Desvincular notificaciones asociadas a los turnos
                    notificacionRepository.desvincularTurnos(turnoIds);

                    // 2. Eliminar historial de reprogramaciones de turnos
                    turnoHistorialRepository.deleteByTurnoIdIn(turnoIds);

                    // 3. Eliminar cambios de estado de turnos
                    cambioEstadoRepository.deleteByAmbitoAndEntidadIdIn(AmbitoEstado.TURNO, turnoIds);

                    // 4. Eliminar turnos
                    turnoRepository.deleteAllByIdIn(turnoIds);
                }

                // 5. Desvincular referencias históricas a los días que se van a eliminar
                turnoHistorialRepository.desvincularDiasAnteriores(diaAgendaIds);

                // 6. Eliminar brechas horarias de los días
                brechaHorariaRepository.deleteByDiaAgendaIdIn(diaAgendaIds);

                // 7. Eliminar cambios de estado de días de agenda
                cambioEstadoRepository.deleteByAmbitoAndEntidadIdIn(AmbitoEstado.DIA_AGENDA, diaAgendaIds);

                // 8. Eliminar días de agenda
                diaAgendaRepository.deleteAllByIdIn(diaAgendaIds);
            }

            // 9. Eliminar cambios de estado de meses de agenda
            cambioEstadoRepository.deleteByAmbitoAndEntidadIdIn(AmbitoEstado.MES_AGENDA, mesAgendaIds);

            // 10. Eliminar meses de agenda
            mesAgendaRepository.deleteByAgendaAnualId(agendaId);
        }

        // Limpiar el contexto de persistencia antes de eliminar la agenda para evitar conflictos de sesión
        entityManager.flush();
        entityManager.clear();

        // 11. Eliminar la agenda anual
        agendaAnualRepository.deleteByIdDirecto(agendaId);

        // 12. Registrar en auditoría
        registradorAuditoria.registrar("AGENDA", "AgendaAnual", agendaId,
                OperacionAuditoria.DELETE, usuario, profesionalId,
                "Eliminación en cascada de agenda anual del año " + anioEfectivo);
    }
}
