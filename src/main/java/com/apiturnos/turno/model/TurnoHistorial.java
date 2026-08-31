package com.apiturnos.turno.model;

import com.apiturnos.agenda.model.DiaAgenda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "turno_historial")
public class TurnoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dia_agenda_anterior_id")
    private DiaAgenda diaAgendaAnterior;

    @Column(name = "inicio_estimado_anterior")
    private Instant inicioEstimadoAnterior;

    @Column(name = "fin_estimado_anterior")
    private Instant finEstimadoAnterior;

    @Column(name = "inicio_estimado_nuevo")
    private Instant inicioEstimadoNuevo;

    @Column(name = "fin_estimado_nuevo")
    private Instant finEstimadoNuevo;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

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

    public DiaAgenda getDiaAgendaAnterior() {
        return diaAgendaAnterior;
    }

    public void setDiaAgendaAnterior(DiaAgenda diaAgendaAnterior) {
        this.diaAgendaAnterior = diaAgendaAnterior;
    }

    public Instant getInicioEstimadoAnterior() {
        return inicioEstimadoAnterior;
    }

    public void setInicioEstimadoAnterior(Instant inicioEstimadoAnterior) {
        this.inicioEstimadoAnterior = inicioEstimadoAnterior;
    }

    public Instant getFinEstimadoAnterior() {
        return finEstimadoAnterior;
    }

    public void setFinEstimadoAnterior(Instant finEstimadoAnterior) {
        this.finEstimadoAnterior = finEstimadoAnterior;
    }

    public Instant getInicioEstimadoNuevo() {
        return inicioEstimadoNuevo;
    }

    public void setInicioEstimadoNuevo(Instant inicioEstimadoNuevo) {
        this.inicioEstimadoNuevo = inicioEstimadoNuevo;
    }

    public Instant getFinEstimadoNuevo() {
        return finEstimadoNuevo;
    }

    public void setFinEstimadoNuevo(Instant finEstimadoNuevo) {
        this.finEstimadoNuevo = finEstimadoNuevo;
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

    public Instant getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(Instant fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TurnoHistorial that = (TurnoHistorial) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

