package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.DiaAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaAgendaRepository extends JpaRepository<DiaAgenda, Long> {
    List<DiaAgenda> findByMesAgendaId(Long mesAgendaId);
    Optional<DiaAgenda> findByMesAgendaIdAndFecha(Long mesAgendaId, LocalDate fecha);
    List<DiaAgenda> findByFechaBetween(LocalDate desde, LocalDate hasta);
}
