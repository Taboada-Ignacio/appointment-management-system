package com.apiturnos.turno.dto;

import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.service.HorarioSugeridoTurnoManual;

import java.time.LocalTime;
import java.util.List;

public class HorarioSugeridoResponseDto {

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer duracionMinutos;
    private Integer turnosConcurrentes;
    private Integer capacidadSimultanea;
    private List<AdvertenciaTurnoManual> advertencias;

    public HorarioSugeridoResponseDto() {
    }

    public HorarioSugeridoResponseDto(HorarioSugeridoTurnoManual sugerido) {
        if (sugerido != null) {
            this.horaInicio = sugerido.horaInicio();
            this.horaFin = sugerido.horaFin();
            this.duracionMinutos = sugerido.duracionMinutos();
            this.turnosConcurrentes = sugerido.turnosConcurrentes();
            this.capacidadSimultanea = sugerido.capacidadSimultanea();
            this.advertencias = sugerido.advertencias();
        }
    }

    public HorarioSugeridoResponseDto(LocalTime horaInicio, LocalTime horaFin, Integer duracionMinutos,
                                      Integer turnosConcurrentes, Integer capacidadSimultanea,
                                      List<AdvertenciaTurnoManual> advertencias) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionMinutos = duracionMinutos;
        this.turnosConcurrentes = turnosConcurrentes;
        this.capacidadSimultanea = capacidadSimultanea;
        this.advertencias = advertencias != null ? List.copyOf(advertencias) : List.of();
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Integer getTurnosConcurrentes() {
        return turnosConcurrentes;
    }

    public void setTurnosConcurrentes(Integer turnosConcurrentes) {
        this.turnosConcurrentes = turnosConcurrentes;
    }

    public Integer getCapacidadSimultanea() {
        return capacidadSimultanea;
    }

    public void setCapacidadSimultanea(Integer capacidadSimultanea) {
        this.capacidadSimultanea = capacidadSimultanea;
    }

    public List<AdvertenciaTurnoManual> getAdvertencias() {
        return advertencias;
    }

    public void setAdvertencias(List<AdvertenciaTurnoManual> advertencias) {
        this.advertencias = advertencias;
    }
}

