package com.apiturnos.turno.model;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.shared.model.AuditableEntity;
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
import java.util.Objects;

@Entity
@Table(name = "turno")
public class Turno extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_agenda_id", nullable = false)
    private DiaAgenda diaAgenda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "inicio_estimado", nullable = false)
    private Instant inicioEstimado;

    @Column(name = "fin_estimado", nullable = false)
    private Instant finEstimado;

    @Column(name = "inicio_real")
    private Instant inicioReal;

    @Column(name = "fin_real")
    private Instant finReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 30)
    private OrigenTurno origen;

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

    public DiaAgenda getDiaAgenda() {
        return diaAgenda;
    }

    public void setDiaAgenda(DiaAgenda diaAgenda) {
        this.diaAgenda = diaAgenda;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Instant getInicioEstimado() {
        return inicioEstimado;
    }

    public void setInicioEstimado(Instant inicioEstimado) {
        this.inicioEstimado = inicioEstimado;
    }

    public Instant getFinEstimado() {
        return finEstimado;
    }

    public void setFinEstimado(Instant finEstimado) {
        this.finEstimado = finEstimado;
    }

    public Instant getInicioReal() {
        return inicioReal;
    }

    public void setInicioReal(Instant inicioReal) {
        this.inicioReal = inicioReal;
    }

    public Instant getFinReal() {
        return finReal;
    }

    public void setFinReal(Instant finReal) {
        this.finReal = finReal;
    }

    public OrigenTurno getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenTurno origen) {
        this.origen = origen;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Turno turno = (Turno) o;
        return id != null && Objects.equals(id, turno.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

