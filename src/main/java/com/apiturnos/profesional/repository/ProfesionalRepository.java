package com.apiturnos.profesional.repository;

import com.apiturnos.profesional.model.Profesional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {
    Optional<Profesional> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("SELECT p FROM Profesional p WHERE " +
           "(:busquedaPattern IS NULL OR " +
           " LOWER(p.nombre) LIKE :busquedaPattern OR " +
           " LOWER(p.apellido) LIKE :busquedaPattern OR " +
           " LOWER(p.email) LIKE :busquedaPattern OR " +
           " LOWER(p.especialidad) LIKE :busquedaPattern) AND " +
           "(:especialidadPattern IS NULL OR LOWER(p.especialidad) LIKE :especialidadPattern)")
    Page<Profesional> buscar(
            @Param("busquedaPattern") String busquedaPattern,
            @Param("especialidadPattern") String especialidadPattern,
            Pageable pageable);
}
