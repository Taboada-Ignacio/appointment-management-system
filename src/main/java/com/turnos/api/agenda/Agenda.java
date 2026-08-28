package com.turnos.api.agenda;

import com.turnos.api.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agenda")
public class Agenda extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "duracion_turno_minutos", nullable = false)
    private Integer duracionTurnoMinutos;

    @Column(name = "tiempo_entre_turnos_minutos", nullable = false)
    private Integer tiempoEntreTurnosMinutos = 0;

    @Column(name = "anticipacion_maxima_dias", nullable = false)
    private Integer anticipacionMaximaDias = 30;

    @Column(name = "anticipacion_minima_horas", nullable = false)
    private Integer anticipacionMinimaHoras = 2;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    public Agenda() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDuracionTurnoMinutos() {
        return duracionTurnoMinutos;
    }

    public void setDuracionTurnoMinutos(Integer duracionTurnoMinutos) {
        this.duracionTurnoMinutos = duracionTurnoMinutos;
    }

    public Integer getTiempoEntreTurnosMinutos() {
        return tiempoEntreTurnosMinutos;
    }

    public void setTiempoEntreTurnosMinutos(Integer tiempoEntreTurnosMinutos) {
        this.tiempoEntreTurnosMinutos = tiempoEntreTurnosMinutos;
    }

    public Integer getAnticipacionMaximaDias() {
        return anticipacionMaximaDias;
    }

    public void setAnticipacionMaximaDias(Integer anticipacionMaximaDias) {
        this.anticipacionMaximaDias = anticipacionMaximaDias;
    }

    public Integer getAnticipacionMinimaHoras() {
        return anticipacionMinimaHoras;
    }

    public void setAnticipacionMinimaHoras(Integer anticipacionMinimaHoras) {
        this.anticipacionMinimaHoras = anticipacionMinimaHoras;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }
}

