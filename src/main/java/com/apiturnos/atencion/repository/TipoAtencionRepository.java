package com.apiturnos.atencion.repository;

import com.apiturnos.atencion.model.TipoAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoAtencionRepository extends JpaRepository<TipoAtencion, Long> {

    List<TipoAtencion> findByProfesionalIdOrderByIdAsc(Long profesionalId);

    List<TipoAtencion> findByProfesionalIdAndActivoTrueOrderByIdAsc(Long profesionalId);

    Optional<TipoAtencion> findByIdAndProfesionalId(Long id, Long profesionalId);

    boolean existsByProfesionalIdAndNombreIgnoreCase(Long profesionalId, String nombre);

    boolean existsByProfesionalIdAndNombreIgnoreCaseAndIdNot(Long profesionalId, String nombre, Long id);
}

