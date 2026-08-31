package com.apiturnos.profesional.repository;

import com.apiturnos.profesional.model.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {
    Optional<Profesional> findByEmail(String email);
}
