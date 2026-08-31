package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExcepcionAgendaRepository extends JpaRepository<ExcepcionAgenda, Long> {
    List<ExcepcionAgenda> findByProfesionalId(Long profesionalId);

    List<ExcepcionAgenda> findByProfesionalIdAndActivaTrueOrderByFechaInicioAscIdAsc(Long profesionalId);

    Optional<ExcepcionAgenda> findByIdAndProfesionalId(Long id, Long profesionalId);

    List<ExcepcionAgenda> findByProfesionalIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
        Long profesionalId, LocalDate fechaFin, LocalDate fechaInicio);

    @Query("""
            SELECT e FROM ExcepcionAgenda e
            WHERE e.profesional.id = :profesionalId
              AND e.activa = true
              AND e.fechaInicio <= :fecha
              AND e.fechaFin >= :fecha
            ORDER BY e.fechaInicio ASC, e.fechaFin ASC, e.id ASC
            """)
    List<ExcepcionAgenda> findActivasAplicablesAFecha(
            @Param("profesionalId") Long profesionalId,
            @Param("fecha") LocalDate fecha);

    @Query("""
            SELECT e FROM ExcepcionAgenda e
            WHERE e.profesional.id = :profesionalId
              AND e.activa = true
              AND e.fechaInicio <= :hasta
              AND e.fechaFin >= :desde
            ORDER BY e.fechaInicio ASC, e.fechaFin ASC, e.id ASC
            """)
    List<ExcepcionAgenda> findActivasIntersectandoRango(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
