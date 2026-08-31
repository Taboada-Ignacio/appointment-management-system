package com.apiturnos.turno.repository;

import com.apiturnos.turno.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByDiaAgendaId(Long diaAgendaId);
    List<Turno> findByClienteId(Long clienteId);
}

