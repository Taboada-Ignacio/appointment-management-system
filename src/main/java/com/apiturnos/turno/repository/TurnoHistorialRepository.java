package com.apiturnos.turno.repository;

import com.apiturnos.turno.model.TurnoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnoHistorialRepository extends JpaRepository<TurnoHistorial, Long> {
    List<TurnoHistorial> findByTurnoIdOrderByFechaEventoAsc(Long turnoId);
}

