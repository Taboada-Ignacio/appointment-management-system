package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.DiaAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaAgendaRepository extends JpaRepository<DiaAgenda, Long> {
    List<DiaAgenda> findByMesAgendaId(Long mesAgendaId);
    Optional<DiaAgenda> findByMesAgendaIdAndFecha(Long mesAgendaId, LocalDate fecha);
    List<DiaAgenda> findByFechaBetween(LocalDate desde, LocalDate hasta);

    @Query("SELECT d FROM DiaAgenda d JOIN FETCH d.mesAgenda m JOIN FETCH m.agendaAnual a JOIN FETCH a.profesional p WHERE d.id = :id")
    Optional<DiaAgenda> findByIdWithMesAndProfesional(@Param("id") Long id);

    @Query("SELECT d FROM DiaAgenda d JOIN d.mesAgenda m JOIN m.agendaAnual a " +
           "WHERE a.profesional.id = :profesionalId AND d.fecha BETWEEN :desde AND :hasta")
    List<DiaAgenda> findByProfesionalIdAndFechaBetween(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("SELECT d FROM DiaAgenda d JOIN FETCH d.mesAgenda m JOIN FETCH m.agendaAnual a " +
           "JOIN FETCH a.profesional p WHERE p.id = :profesionalId AND d.fecha = :fecha")
    Optional<DiaAgenda> findByProfesionalIdAndFecha(
            @Param("profesionalId") Long profesionalId,
            @Param("fecha") LocalDate fecha);
}
