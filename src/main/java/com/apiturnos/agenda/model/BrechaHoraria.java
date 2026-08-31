package com.apiturnos.agenda.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "brecha_horaria")
public class BrechaHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_agenda_id", nullable = false)
    private DiaAgenda diaAgenda;

    @Column(name = "hora_inicio_atencion", nullable = false)
    private LocalTime horaInicioAtencion;

    @Column(name = "hora_fin_atencion", nullable = false)
    private LocalTime horaFinAtencion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DiaAgenda getDiaAgenda() {
        return diaAgenda;
    }

    public void setDiaAgenda(DiaAgenda diaAgenda) {
        this.diaAgenda = diaAgenda;
    }

    public LocalTime getHoraInicioAtencion() {
        return horaInicioAtencion;
    }

    public void setHoraInicioAtencion(LocalTime horaInicioAtencion) {
        this.horaInicioAtencion = horaInicioAtencion;
    }

    public LocalTime getHoraFinAtencion() {
        return horaFinAtencion;
    }

    public void setHoraFinAtencion(LocalTime horaFinAtencion) {
        this.horaFinAtencion = horaFinAtencion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrechaHoraria)) return false;
        BrechaHoraria that = (BrechaHoraria) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
