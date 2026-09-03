package com.apiturnos.turno.repository;

import com.apiturnos.turno.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Turno t WHERE t.id = :id")
    Optional<Turno> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT t FROM Turno t
            LEFT JOIN FETCH t.diaAgenda d
            LEFT JOIN FETCH d.mesAgenda m
            LEFT JOIN FETCH m.agendaAnual a
            LEFT JOIN FETCH a.profesional p
            LEFT JOIN FETCH t.cliente c
            LEFT JOIN FETCH t.tipoAtencion ta
            WHERE t.id = :id
            """)
    Optional<Turno> findByIdConRelaciones(@Param("id") Long id);

    List<Turno> findByDiaAgendaId(Long diaAgendaId);
    List<Turno> findByDiaAgendaIdIn(List<Long> diaAgendaIds);

    @Query("SELECT t.id FROM Turno t WHERE t.diaAgenda.id IN :diaAgendaIds")
    List<Long> findIdsByDiaAgendaIdIn(@Param("diaAgendaIds") List<Long> diaAgendaIds);

    List<Turno> findByClienteId(Long clienteId);


    @Modifying
    @Query("DELETE FROM Turno t WHERE t.id IN :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);


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

    @Query("""
            SELECT t FROM Turno t
            WHERE t.tipoAtencion.id = :tipoAtencionId
              AND t.inicioEstimado < :fin
              AND t.finEstimado > :inicio
            ORDER BY t.inicioEstimado ASC, t.id ASC
            """)
    List<Turno> findTurnosSolapadosPorTipoAtencion(
            @Param("tipoAtencionId") Long tipoAtencionId,
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin);
}
