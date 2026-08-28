package com.turnos.api.turno;

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

@Entity
@Table(name = "turno_historial")
public class TurnoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 50)
    private TipoEventoTurno tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resultante", nullable = false, length = 30)
    private EstadoTurno estadoResultante;

    @Column(name = "fecha_hora_inicio_anterior")
    private Instant fechaHoraInicioAnterior;

    @Column(name = "fecha_hora_fin_anterior")
    private Instant fechaHoraFinAnterior;

    @Column(name = "fecha_hora_inicio_nueva")
    private Instant fechaHoraInicioNueva;

    @Column(name = "fecha_hora_fin_nueva")
    private Instant fechaHoraFinNueva;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "origen", length = 50)
    private String origen;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEvento == null) {
            this.fechaEvento = Instant.now();
        }
    }

    public TurnoHistorial() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public TipoEventoTurno getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEventoTurno tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public EstadoTurno getEstadoResultante() {
        return estadoResultante;
    }

    public void setEstadoResultante(EstadoTurno estadoResultante) {
        this.estadoResultante = estadoResultante;
    }

    public Instant getFechaHoraInicioAnterior() {
        return fechaHoraInicioAnterior;
    }

    public void setFechaHoraInicioAnterior(Instant fechaHoraInicioAnterior) {
        this.fechaHoraInicioAnterior = fechaHoraInicioAnterior;
    }

    public Instant getFechaHoraFinAnterior() {
        return fechaHoraFinAnterior;
    }

    public void setFechaHoraFinAnterior(Instant fechaHoraFinAnterior) {
        this.fechaHoraFinAnterior = fechaHoraFinAnterior;
    }

    public Instant getFechaHoraInicioNueva() {
        return fechaHoraInicioNueva;
    }

    public void setFechaHoraInicioNueva(Instant fechaHoraInicioNueva) {
        this.fechaHoraInicioNueva = fechaHoraInicioNueva;
    }

    public Instant getFechaHoraFinNueva() {
        return fechaHoraFinNueva;
    }

    public void setFechaHoraFinNueva(Instant fechaHoraFinNueva) {
        this.fechaHoraFinNueva = fechaHoraFinNueva;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Instant getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(Instant fechaEvento) {
        this.fechaEvento = fechaEvento;
    }
}

