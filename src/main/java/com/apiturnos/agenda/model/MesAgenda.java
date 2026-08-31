package com.apiturnos.agenda.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mes_agenda", uniqueConstraints = @UniqueConstraint(name = "uk_mes_agenda_agenda_mes", columnNames = {"agenda_anual_id", "nro_mes"}))
public class MesAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agenda_anual_id", nullable = false)
    private AgendaAnual agendaAnual;

    @Column(name = "nro_mes", nullable = false)
    private Integer nroMes;

    @Column(name = "repetir_configuracion", nullable = false)
    private Boolean repetirConfiguracion = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AgendaAnual getAgendaAnual() {
        return agendaAnual;
    }

    public void setAgendaAnual(AgendaAnual agendaAnual) {
        this.agendaAnual = agendaAnual;
    }

    public Integer getNroMes() {
        return nroMes;
    }

    public void setNroMes(Integer nroMes) {
        this.nroMes = nroMes;
    }

    public Boolean getRepetirConfiguracion() {
        return repetirConfiguracion;
    }

    public void setRepetirConfiguracion(Boolean repetirConfiguracion) {
        this.repetirConfiguracion = repetirConfiguracion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MesAgenda)) return false;
        MesAgenda that = (MesAgenda) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
