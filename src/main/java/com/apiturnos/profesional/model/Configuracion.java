package com.apiturnos.profesional.model;

import com.apiturnos.shared.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(name = "configuracion", uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_profesional", columnNames = {"profesional_id"}))
public class Configuracion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Column(name = "cantidad_max_turnos_a_la_vez", nullable = false)
    private Integer cantidadMaxTurnosALaVez = 1;

    @Column(name = "duracion_aproximada_por_turno", nullable = false)
    private Integer duracionAproximadaPorTurno = 30;

    @Column(name = "agenda_solo_manejada_por_profesional", nullable = false)
    private Boolean agendaSoloManejadaPorProfesional = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Profesional getProfesional() {
        return profesional;
    }

    public void setProfesional(Profesional profesional) {
        this.profesional = profesional;
    }

    public Integer getCantidadMaxTurnosALaVez() {
        return cantidadMaxTurnosALaVez;
    }

    public void setCantidadMaxTurnosALaVez(Integer cantidadMaxTurnosALaVez) {
        this.cantidadMaxTurnosALaVez = cantidadMaxTurnosALaVez;
    }

    public Integer getDuracionAproximadaPorTurno() {
        return duracionAproximadaPorTurno;
    }

    public void setDuracionAproximadaPorTurno(Integer duracionAproximadaPorTurno) {
        this.duracionAproximadaPorTurno = duracionAproximadaPorTurno;
    }

    public Boolean getAgendaSoloManejadaPorProfesional() {
        return agendaSoloManejadaPorProfesional;
    }

    public void setAgendaSoloManejadaPorProfesional(Boolean agendaSoloManejadaPorProfesional) {
        this.agendaSoloManejadaPorProfesional = agendaSoloManejadaPorProfesional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Configuracion)) return false;
        Configuracion that = (Configuracion) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
