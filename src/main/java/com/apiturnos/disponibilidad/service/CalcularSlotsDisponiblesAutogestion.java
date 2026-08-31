package com.apiturnos.disponibilidad.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.disponibilidad.dto.SlotDisponibleDto;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalcularSlotsDisponiblesAutogestion {

    private final TipoAtencionRepository tipoAtencionRepository;
    private final CalcularDisponibilidadDia calcularDisponibilidadDia;
    private final VerificarCapacidadTipoAtencion verificarCapacidadTipoAtencion;
    private final ZoneId zoneId;

    public CalcularSlotsDisponiblesAutogestion(
            TipoAtencionRepository tipoAtencionRepository,
            CalcularDisponibilidadDia calcularDisponibilidadDia,
            VerificarCapacidadTipoAtencion verificarCapacidadTipoAtencion,
            @Value("${turnos.zona-horaria:America/Argentina/Buenos_Aires}") String zonaHoraria) {
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.calcularDisponibilidadDia = calcularDisponibilidadDia;
        this.verificarCapacidadTipoAtencion = verificarCapacidadTipoAtencion;
        this.zoneId = ZoneId.of(zonaHoraria);
    }

    @Transactional(readOnly = true)
    public List<SlotDisponibleDto> ejecutar(Long profesionalId, Long tipoAtencionId, LocalDate fecha) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (tipoAtencionId == null) {
            throw new NegocioException("El ID del tipo de atención es obligatorio");
        }
        if (fecha == null) {
            throw new NegocioException("La fecha es obligatoria");
        }

        TipoAtencion tipo = tipoAtencionRepository.findById(tipoAtencionId)
                .orElseThrow(() -> new EntidadNoEncontradaException("TipoAtencion", tipoAtencionId));

        if (!tipo.getProfesional().getId().equals(profesionalId)) {
            throw new TipoAtencionNoPerteneceProfesionalException(tipoAtencionId, profesionalId);
        }

        if (!tipo.isActivo()) {
            return List.of();
        }

        List<IntervaloHorario> intervalosEfectivos = calcularDisponibilidadDia.ejecutar(profesionalId, fecha);
        if (intervalosEfectivos.isEmpty()) {
            return List.of();
        }

        int duracion = tipo.getDuracionMinutos();
        List<SlotDisponibleDto> slotsDisponibles = new ArrayList<>();

        for (IntervaloHorario franja : intervalosEfectivos) {
            LocalTime actual = franja.inicio();
            while (!actual.plusMinutes(duracion).isAfter(franja.fin())) {
                LocalTime slotInicio = actual;
                LocalTime slotFin = actual.plusMinutes(duracion);

                Instant instantInicio = fecha.atTime(slotInicio).atZone(zoneId).toInstant();
                Instant instantFin = fecha.atTime(slotFin).atZone(zoneId).toInstant();

                VerificarCapacidadTipoAtencion.ResultadoCapacidad resultado =
                        verificarCapacidadTipoAtencion.evaluar(tipo, instantInicio, instantFin, null);

                if (resultado.disponible()) {
                    slotsDisponibles.add(new SlotDisponibleDto(
                            slotInicio,
                            slotFin,
                            duracion,
                            resultado.turnosConcurrentes(),
                            resultado.capacidadMaxima()
                    ));
                }

                actual = actual.plusMinutes(duracion);
            }
        }

        return slotsDisponibles;
    }
}

