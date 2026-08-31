package com.apiturnos.auditoria.repository;

import com.apiturnos.auditoria.model.AuditoriaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {
    List<AuditoriaEvento> findByModuloAndEntidadAndEntidadIdOrderByFechaHoraDesc(String modulo, String entidad, String entidadId);
    List<AuditoriaEvento> findByProfesionalIdOrderByFechaHoraDesc(Long profesionalId);
}
