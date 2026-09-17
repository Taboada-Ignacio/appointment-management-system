package com.apiturnos.turno.repository;

import com.apiturnos.turno.model.MotivoBajaTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MotivoBajaTurnoRepository extends JpaRepository<MotivoBajaTurno, Long> {
    Optional<MotivoBajaTurno> findFirstByExcepcionAgendaIdOrderByIdAsc(Long excepcionAgendaId);

    @Modifying
    @Query("UPDATE MotivoBajaTurno m SET m.excepcionAgenda = null WHERE m.excepcionAgenda.id = :excepcionId")
    void desvincularExcepcion(@Param("excepcionId") Long excepcionId);
}

