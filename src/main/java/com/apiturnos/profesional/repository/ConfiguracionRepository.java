package com.apiturnos.profesional.repository;

import com.apiturnos.profesional.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {
    Optional<Configuracion> findByProfesionalId(Long profesionalId);
}
