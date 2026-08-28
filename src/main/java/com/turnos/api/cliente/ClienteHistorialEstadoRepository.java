package com.turnos.api.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteHistorialEstadoRepository extends JpaRepository<ClienteHistorialEstado, Long> {

    List<ClienteHistorialEstado> findByClienteIdOrderByFechaCambioDesc(Long clienteId);
}

