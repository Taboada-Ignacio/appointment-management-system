package com.apiturnos.cliente.repository;

import com.apiturnos.cliente.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByProfesionalId(Long profesionalId);
    Optional<Cliente> findByIdAndProfesionalId(Long id, Long profesionalId);
    Optional<Cliente> findByProfesionalIdAndNumeroDocumento(Long profesionalId, String numeroDocumento);
    boolean existsByProfesionalIdAndNumeroDocumento(Long profesionalId, String numeroDocumento);
    boolean existsByProfesionalIdAndNumeroDocumentoAndIdNot(Long profesionalId, String numeroDocumento, Long id);

    List<Cliente> findByProfesionalIdAndApellidoContainingIgnoreCase(Long profesionalId, String apellido);
    List<Cliente> findByProfesionalIdAndNombreContainingIgnoreCase(Long profesionalId, String nombre);

    @Query("SELECT c FROM Cliente c WHERE c.profesional.id = :profesionalId " +
           "AND (:nombrePattern IS NULL OR LOWER(c.nombre) LIKE :nombrePattern) " +
           "AND (:apellidoPattern IS NULL OR LOWER(c.apellido) LIKE :apellidoPattern) " +
           "AND (:dniPattern IS NULL OR c.numeroDocumento LIKE :dniPattern) " +
           "AND (:estadoNombre IS NULL OR EXISTS (" +
           "    SELECT ce.id FROM CambioEstado ce " +
           "    WHERE ce.ambito = com.apiturnos.estado.model.AmbitoEstado.CLIENTE " +
           "      AND ce.entidadId = c.id " +
           "      AND ce.fechaHoraFin IS NULL " +
           "      AND ce.estado.nombre = :estadoNombre" +
           "))")
    Page<Cliente> buscarCartera(
            @Param("profesionalId") Long profesionalId,
            @Param("nombrePattern") String nombrePattern,
            @Param("apellidoPattern") String apellidoPattern,
            @Param("dniPattern") String dniPattern,
            @Param("estadoNombre") String estadoNombre,
            Pageable pageable);
}
