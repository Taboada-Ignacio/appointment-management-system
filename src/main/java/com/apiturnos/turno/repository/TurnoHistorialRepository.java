package com.apiturnos.turno.repository;

import com.apiturnos.turno.model.TurnoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnoHistorialRepository extends JpaRepository<TurnoHistorial, Long> {
    List<TurnoHistorial> findByTurnoIdOrderByFechaEventoAsc(Long turnoId);

    void deleteByTurnoId(Long turnoId);

    @Modifying
    @Query("DELETE FROM TurnoHistorial th WHERE th.turno.id IN :turnoIds")
    void deleteByTurnoIdIn(@Param("turnoIds") List<Long> turnoIds);

    @Modifying
    @Query("UPDATE TurnoHistorial th SET th.diaAgendaAnterior = null WHERE th.diaAgendaAnterior.id IN :diaAgendaIds")
    void desvincularDiasAnteriores(@Param("diaAgendaIds") List<Long> diaAgendaIds);
}


