package com.apiturnos.agenda.model;

import com.apiturnos.profesional.model.Profesional;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "agenda_anual", uniqueConstraints = @UniqueConstraint(name = "uk_agenda_anual_profesional_anio", columnNames = {"profesional_id", "anio"}))
public class AgendaAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = Instant.now();
        }
    }

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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgendaAnual)) return false;
        AgendaAnual that = (AgendaAnual) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
