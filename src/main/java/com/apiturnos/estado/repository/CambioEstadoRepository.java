package com.apiturnos.estado.repository;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {
    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    Optional<CambioEstado> findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDesc(AmbitoEstado ambito, Long entidadId);

    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    List<CambioEstado> findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(AmbitoEstado ambito, Long entidadId);
}
