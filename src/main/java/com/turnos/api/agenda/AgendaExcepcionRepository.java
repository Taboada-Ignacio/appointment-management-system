package com.turnos.api.agenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface AgendaExcepcionRepository extends JpaRepository<AgendaExcepcion, Long> {

    List<AgendaExcepcion> findByAgendaIdAndActivaTrue(Long agendaId);

    List<AgendaExcepcion> findByAgendaIdAndActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
        Long agendaId,
        Instant fechaFin,
        Instant fechaInicio
    );
}

