package com.apiturnos.cliente.repository;

import com.apiturnos.cliente.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByProfesionalId(Long profesionalId);
    Optional<Cliente> findByProfesionalIdAndNumeroDocumento(Long profesionalId, String numeroDocumento);
    boolean existsByProfesionalIdAndNumeroDocumento(Long profesionalId, String numeroDocumento);
}
