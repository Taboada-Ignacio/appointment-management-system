package com.apiturnos.agenda.model;

import com.apiturnos.turno.model.Turno;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "afectacion_turno_excepcion",
        uniqueConstraints = @UniqueConstraint(name = "uk_afectacion_excepcion_turno", columnNames = {"excepcion_agenda_id", "turno_id"}))
public class AfectacionTurnoExcepcion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "excepcion_agenda_id", nullable = false)
    private ExcepcionAgenda excepcionAgenda;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resolucion", nullable = false, length = 30)
    private EstadoResolucionAfectacion estadoResolucion;
    @Column(name = "estado_turno_anterior", nullable = false, length = 50)
    private String estadoTurnoAnterior;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_agenda_anterior_id", nullable = false)
    private DiaAgenda diaAgendaAnterior;
    @Column(name = "inicio_anterior", nullable = false)
    private Instant inicioAnterior;
    @Column(name = "fin_anterior", nullable = false)
    private Instant finAnterior;
    @Column(columnDefinition = "TEXT")
    private String observacion;
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;
    @Column(name = "resuelto_en")
    private Instant resueltoEn;

    @PrePersist void crear() { if (creadoEn == null) creadoEn = Instant.now(); }
    public Long getId() { return id; }
    public ExcepcionAgenda getExcepcionAgenda() { return excepcionAgenda; }
    public void setExcepcionAgenda(ExcepcionAgenda value) { excepcionAgenda = value; }
    public Turno getTurno() { return turno; }
    public void setTurno(Turno value) { turno = value; }
    public EstadoResolucionAfectacion getEstadoResolucion() { return estadoResolucion; }
    public void setEstadoResolucion(EstadoResolucionAfectacion value) { estadoResolucion = value; }
    public String getEstadoTurnoAnterior() { return estadoTurnoAnterior; }
    public void setEstadoTurnoAnterior(String value) { estadoTurnoAnterior = value; }
    public DiaAgenda getDiaAgendaAnterior() { return diaAgendaAnterior; }
    public void setDiaAgendaAnterior(DiaAgenda value) { diaAgendaAnterior = value; }
    public Instant getInicioAnterior() { return inicioAnterior; }
    public void setInicioAnterior(Instant value) { inicioAnterior = value; }
    public Instant getFinAnterior() { return finAnterior; }
    public void setFinAnterior(Instant value) { finAnterior = value; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String value) { observacion = value; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getResueltoEn() { return resueltoEn; }
    public void setResueltoEn(Instant value) { resueltoEn = value; }
}
