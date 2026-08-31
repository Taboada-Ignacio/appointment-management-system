package com.apiturnos.estado.repository;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    Optional<CambioEstado> findFirstByAmbitoAndEntidadIdAndFechaHoraFinIsNullOrderByIdDesc(AmbitoEstado ambito, Long entidadId);

    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    Optional<CambioEstado> findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDesc(AmbitoEstado ambito, Long entidadId);

    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    Optional<CambioEstado> findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDescIdDesc(AmbitoEstado ambito, Long entidadId);

    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    List<CambioEstado> findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(AmbitoEstado ambito, Long entidadId);

    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    List<CambioEstado> findByAmbitoAndEntidadIdOrderByFechaHoraInicioAscIdAsc(AmbitoEstado ambito, Long entidadId);

    @EntityGraph(attributePaths = {"estado", "motivoBajaTurno"})
    @Query("SELECT ce FROM CambioEstado ce WHERE ce.ambito = :ambito AND ce.entidadId IN :entidadIds AND ce.fechaHoraFin IS NULL ORDER BY ce.id DESC")
    List<CambioEstado> findCurrentByAmbitoAndEntidadIds(@Param("ambito") AmbitoEstado ambito, @Param("entidadIds") List<Long> entidadIds);
}
