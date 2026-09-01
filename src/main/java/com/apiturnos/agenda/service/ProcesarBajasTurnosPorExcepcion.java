package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.service.DarDeBajaTurno;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcesarBajasTurnosPorExcepcion {

    private static final int LONGITUD_MAXIMA_MOTIVO = 255;

    private final MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    private final DarDeBajaTurno darDeBajaTurno;

    public ProcesarBajasTurnosPorExcepcion(
            MotivoBajaTurnoRepository motivoBajaTurnoRepository,
            DarDeBajaTurno darDeBajaTurno) {
        this.motivoBajaTurnoRepository = motivoBajaTurnoRepository;
        this.darDeBajaTurno = darDeBajaTurno;
    }

    /**
     * Procesa las bajas lógicas de los turnos afectados.
     * <p>
     * Crea un {@link MotivoBajaTurno} nuevo por cada ejecución efectiva que produzca bajas,
     * compartiéndolo entre todos los turnos afectados en esta ejecución y vinculándolo a la excepción,
     * garantizando la inmutabilidad de la causa histórica ante modificaciones posteriores de la excepción.
     * </p>
     */
    public void ejecutar(ExcepcionAgenda excepcion, List<Turno> turnosAfectados, String usuario) {
        if (turnosAfectados.isEmpty()) {
            return;
        }

        MotivoBajaTurno motivo = crearNuevoMotivo(excepcion);
        for (Turno turno : turnosAfectados) {
            String observacion = "Baja por excepción de agenda " + excepcion.getTipo()
                    + " (excepción " + excepcion.getId() + "): " + excepcion.getMotivo();

            darDeBajaTurno.ejecutar(turno.getId(), motivo, observacion, usuario);
        }
    }

    private MotivoBajaTurno crearNuevoMotivo(ExcepcionAgenda excepcion) {
        String texto = "Excepción de agenda " + excepcion.getTipo() + ": " + excepcion.getMotivo();
        if (texto.length() > LONGITUD_MAXIMA_MOTIVO) {
            texto = texto.substring(0, LONGITUD_MAXIMA_MOTIVO);
        }
        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo(texto);
        motivo.setExcepcionAgenda(excepcion);
        return motivoBajaTurnoRepository.save(motivo);
    }
}
