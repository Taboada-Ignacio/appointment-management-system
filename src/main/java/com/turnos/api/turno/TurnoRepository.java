package com.turnos.api.turno;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByAgendaIdAndFechaHoraInicioBetween(Long agendaId, Instant inicio, Instant fin);

    List<Turno> findByClienteId(Long clienteId);

    List<Turno> findByAgendaExcepcionId(Long agendaExcepcionId);

    List<Turno> findByEstado(EstadoTurno estado);
}

