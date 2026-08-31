package com.apiturnos.auditoria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "auditoria_evento")
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;

    @Column(name = "entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id", nullable = false, length = 50)
    private String entidadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacion", nullable = false, length = 30)
    private OperacionAuditoria operacion;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "profesional_id")
    private Long profesionalId;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @PrePersist
    protected void onCreate() {
        if (this.fechaHora == null) {
            this.fechaHora = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(String entidadId) {
        this.entidadId = entidadId;
    }

    public OperacionAuditoria getOperacion() {
        return operacion;
    }

    public void setOperacion(OperacionAuditoria operacion) {
        this.operacion = operacion;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getDetalles() {
        return detalles;
    }

    public void setDetalles(String detalles) {
        this.detalles = detalles;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Long profesionalId) {
        this.profesionalId = profesionalId;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Instant fechaHora) {
        this.fechaHora = fechaHora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditoriaEvento)) return false;
        AuditoriaEvento that = (AuditoriaEvento) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
