package com.apiturnos.agenda.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dia_agenda", uniqueConstraints = @UniqueConstraint(name = "uk_dia_agenda_mes_fecha", columnNames = {"mes_agenda_id", "fecha"}))
public class DiaAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mes_agenda_id", nullable = false)
    private MesAgenda mesAgenda;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MesAgenda getMesAgenda() {
        return mesAgenda;
    }

    public void setMesAgenda(MesAgenda mesAgenda) {
        this.mesAgenda = mesAgenda;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiaAgenda)) return false;
        DiaAgenda that = (DiaAgenda) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
