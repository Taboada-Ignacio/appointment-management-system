package com.apiturnos.notificacion.repository;

import com.apiturnos.notificacion.model.EstadoNotificacion;
import com.apiturnos.notificacion.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByEstadoAndFechaProgramadaLessThanEqual(EstadoNotificacion estado, Instant fecha);
    List<Notificacion> findByClienteId(Long clienteId);
    List<Notificacion> findByTurnoId(Long turnoId);
}
