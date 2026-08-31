package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExcepcionAgendaRepository extends JpaRepository<ExcepcionAgenda, Long> {
    List<ExcepcionAgenda> findByProfesionalId(Long profesionalId);
    List<ExcepcionAgenda> findByProfesionalIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
        Long profesionalId, LocalDate fechaFin, LocalDate fechaInicio);
}
