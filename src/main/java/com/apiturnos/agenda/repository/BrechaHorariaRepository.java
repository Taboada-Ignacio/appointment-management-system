package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.BrechaHoraria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BrechaHorariaRepository extends JpaRepository<BrechaHoraria, Long> {
    List<BrechaHoraria> findByDiaAgendaId(Long diaAgendaId);
    List<BrechaHoraria> findByDiaAgendaIdInOrderByDiaAgendaIdAscHoraInicioAtencionAsc(List<Long> diaAgendaIds);
}
