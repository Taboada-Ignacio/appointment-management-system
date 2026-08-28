package com.turnos.api.turno;

import com.turnos.api.agenda.Agenda;
import com.turnos.api.agenda.AgendaExcepcion;
import com.turnos.api.cliente.Cliente;
import com.turnos.api.shared.AuditableEntity;
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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "turno")
public class Turno extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agenda_id", nullable = false)
    private Agenda agenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private Instant fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private Instant fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoTurno estado = EstadoTurno.DISPONIBLE;

    @Column(name = "motivo_baja", columnDefinition = "TEXT")
    private String motivoBaja;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_baja", length = 50)
    private OrigenBaja origenBaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agenda_excepcion_id")
    private AgendaExcepcion agendaExcepcion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    public Turno() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Agenda getAgenda() {
        return agenda;
    }

    public void setAgenda(Agenda agenda) {
        this.agenda = agenda;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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

    public EstadoTurno getEstado() {
        return estado;
    }

    public void setEstado(EstadoTurno estado) {
        this.estado = estado;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }

    public OrigenBaja getOrigenBaja() {
        return origenBaja;
    }

    public void setOrigenBaja(OrigenBaja origenBaja) {
        this.origenBaja = origenBaja;
    }

    public AgendaExcepcion getAgendaExcepcion() {
        return agendaExcepcion;
    }

    public void setAgendaExcepcion(AgendaExcepcion agendaExcepcion) {
        this.agendaExcepcion = agendaExcepcion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

