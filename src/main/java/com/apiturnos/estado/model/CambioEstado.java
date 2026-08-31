package com.apiturnos.estado.model;

import com.apiturnos.turno.model.MotivoBajaTurno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "cambio_estado")
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambito", nullable = false, length = 20)
    private AmbitoEstado ambito;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private Instant fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private Instant fechaHoraFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_baja_turno_id")
    private MotivoBajaTurno motivoBajaTurno;

    @Column(name = "usuario", length = 100)
    private String usuario;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaHoraInicio == null) {
            this.fechaHoraInicio = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public AmbitoEstado getAmbito() {
        return ambito;
    }

    public void setAmbito(AmbitoEstado ambito) {
        this.ambito = ambito;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public Instant getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(Instant fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public Instant getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(Instant fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public MotivoBajaTurno getMotivoBajaTurno() {
        return motivoBajaTurno;
    }

    public void setMotivoBajaTurno(MotivoBajaTurno motivoBajaTurno) {
        this.motivoBajaTurno = motivoBajaTurno;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CambioEstado)) return false;
        CambioEstado that = (CambioEstado) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
