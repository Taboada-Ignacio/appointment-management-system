package com.apiturnos.estado.repository;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long> {
    List<Estado> findByAmbito(AmbitoEstado ambito);
    Optional<Estado> findByNombreAndAmbito(String nombre, AmbitoEstado ambito);
}
