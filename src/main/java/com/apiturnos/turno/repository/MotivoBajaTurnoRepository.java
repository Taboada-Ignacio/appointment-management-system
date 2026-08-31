package com.apiturnos.turno.repository;

import com.apiturnos.turno.model.MotivoBajaTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MotivoBajaTurnoRepository extends JpaRepository<MotivoBajaTurno, Long> {
    Optional<MotivoBajaTurno> findFirstByExcepcionAgendaIdOrderByIdAsc(Long excepcionAgendaId);
}

