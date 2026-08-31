package com.apiturnos.turno.repository;

import com.apiturnos.turno.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByDiaAgendaId(Long diaAgendaId);
    List<Turno> findByClienteId(Long clienteId);

    @Query("""
            SELECT t FROM Turno t
            JOIN FETCH t.diaAgenda d
            JOIN FETCH d.mesAgenda m
            JOIN FETCH m.agendaAnual a
            JOIN FETCH a.profesional p
            JOIN FETCH t.cliente c
            WHERE p.id = :profesionalId
              AND d.fecha BETWEEN :desde AND :hasta
            ORDER BY d.fecha ASC, t.inicioEstimado ASC, t.id ASC
            """)
    List<Turno> findByProfesionalAndFechaBetween(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT t FROM Turno t
            JOIN FETCH t.diaAgenda d
            JOIN FETCH d.mesAgenda m
            JOIN FETCH m.agendaAnual a
            JOIN FETCH a.profesional p
            JOIN FETCH t.cliente c
            WHERE p.id = :profesionalId
              AND d.fecha = :fecha
              AND t.inicioEstimado < :fin
              AND t.finEstimado > :inicio
            ORDER BY t.inicioEstimado ASC, t.id ASC
            """)
    List<Turno> findIntersectandoFranja(
            @Param("profesionalId") Long profesionalId,
            @Param("fecha") LocalDate fecha,
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin);
}

