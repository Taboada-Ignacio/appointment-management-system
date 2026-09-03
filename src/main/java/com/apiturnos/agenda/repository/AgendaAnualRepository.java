package com.apiturnos.agenda.repository;

import com.apiturnos.agenda.model.AgendaAnual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendaAnualRepository extends JpaRepository<AgendaAnual, Long> {
    List<AgendaAnual> findByProfesionalId(Long profesionalId);
    Optional<AgendaAnual> findByProfesionalIdAndAnio(Long profesionalId, Integer anio);

    @Modifying
    @Query("DELETE FROM AgendaAnual a WHERE a.id = :id")
    void deleteByIdDirecto(@Param("id") Long id);
}

