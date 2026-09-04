package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.AfectacionTurnoExcepcion;
import com.apiturnos.agenda.model.EstadoResolucionAfectacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AfectacionTurnoExcepcionRepository extends JpaRepository<AfectacionTurnoExcepcion, Long> {
    Optional<AfectacionTurnoExcepcion> findByIdAndExcepcionAgendaProfesionalId(Long id, Long profesionalId);
    List<AfectacionTurnoExcepcion> findByExcepcionAgendaIdOrderByIdAsc(Long excepcionId);

    @Query("""
        SELECT a FROM AfectacionTurnoExcepcion a
        JOIN FETCH a.excepcionAgenda e
        JOIN FETCH a.turno t JOIN FETCH t.cliente
        JOIN FETCH t.diaAgenda d
        WHERE e.profesional.id = :profesionalId
          AND (:estado IS NULL OR a.estadoResolucion = :estado)
        ORDER BY e.fechaInicio DESC, e.id DESC, a.id ASC
        """)
    List<AfectacionTurnoExcepcion> listar(
            @Param("profesionalId") Long profesionalId,
            @Param("estado") EstadoResolucionAfectacion estado);
}
