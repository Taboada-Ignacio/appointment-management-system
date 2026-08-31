package com.apiturnos.agenda.model;

import com.apiturnos.profesional.model.Profesional;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "excepcion_agenda")
public class ExcepcionAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoExcepcion tipo;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @OneToMany(mappedBy = "excepcionAgenda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<BrechaExcepcion> brechas = new java.util.ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private Instant fechaModificacion;

    @PrePersist
    protected void onCreate() {
        Instant ahora = Instant.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = ahora;
        }
        this.fechaModificacion = ahora;
        if (this.activa == null) {
            this.activa = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = Instant.now();
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public TipoExcepcion getTipo() {
        return tipo;
    }

    public void setTipo(TipoExcepcion tipo) {
        this.tipo = tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
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

    public Boolean getActiva() {
        return activa;
    }

    public boolean isActiva() {
        return Boolean.TRUE.equals(activa);
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaModificacion() {
        return fechaModificacion;
    }

    public java.util.List<BrechaExcepcion> getBrechas() {
        return brechas;
    }

    public void setBrechas(java.util.List<BrechaExcepcion> brechas) {
        this.brechas = brechas;
    }

    public void agregarBrecha(LocalTime inicio, LocalTime fin) {
        BrechaExcepcion brecha = new BrechaExcepcion(this, inicio, fin);
        this.brechas.add(brecha);
    }

    public void limpiarBrechas() {
        this.brechas.clear();
    }

    public java.util.List<com.apiturnos.disponibilidad.model.IntervaloHorario> obtenerIntervalos() {
        if (brechas != null && !brechas.isEmpty()) {
            return brechas.stream()
                    .map(b -> new com.apiturnos.disponibilidad.model.IntervaloHorario(b.getHoraInicio(), b.getHoraFin()))
                    .toList();
        }
        if (horaInicio != null && horaFin != null) {
            return java.util.List.of(new com.apiturnos.disponibilidad.model.IntervaloHorario(horaInicio, horaFin));
        }
        return java.util.List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExcepcionAgenda that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
