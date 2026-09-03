package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.MesAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesAgendaRepository extends JpaRepository<MesAgenda, Long> {
    List<MesAgenda> findByAgendaAnualId(Long agendaAnualId);

    @Query("SELECT m.id FROM MesAgenda m WHERE m.agendaAnual.id = :agendaAnualId")
    List<Long> findIdsByAgendaAnualId(@Param("agendaAnualId") Long agendaAnualId);

    Optional<MesAgenda> findByAgendaAnualIdAndNroMes(Long agendaAnualId, Integer nroMes);


    @Modifying
    @Query("DELETE FROM MesAgenda m WHERE m.agendaAnual.id = :agendaAnualId")
    void deleteByAgendaAnualId(@Param("agendaAnualId") Long agendaAnualId);

    @Query("SELECT m FROM MesAgenda m JOIN FETCH m.agendaAnual a JOIN FETCH a.profesional p WHERE m.id = :id")
    Optional<MesAgenda> findByIdWithAgendaAndProfesional(@Param("id") Long id);
}

