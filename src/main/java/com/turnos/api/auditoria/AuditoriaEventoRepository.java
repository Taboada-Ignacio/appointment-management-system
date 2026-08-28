package com.turnos.api.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    List<AuditoriaEvento> findByModuloAndEntidadAndEntidadIdOrderByFechaHoraDesc(
        String modulo,
        String entidad,
        String entidadId
    );
}

