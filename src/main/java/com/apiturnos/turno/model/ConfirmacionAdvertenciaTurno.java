package com.apiturnos.turno.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "confirmacion_advertencia_turno")
public class ConfirmacionAdvertenciaTurno {
    @Id private UUID id;
    @Column(nullable = false, length = 64) private String huella;
    @Column(name = "vence_en", nullable = false) private Instant venceEn;
    @Column(nullable = false) private boolean usada;
    @Column(name = "usada_en") private Instant usadaEn;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getHuella() { return huella; }
    public void setHuella(String huella) { this.huella = huella; }
    public Instant getVenceEn() { return venceEn; }
    public void setVenceEn(Instant venceEn) { this.venceEn = venceEn; }
    public boolean isUsada() { return usada; }
    public void setUsada(boolean usada) { this.usada = usada; }
    public Instant getUsadaEn() { return usadaEn; }
    public void setUsadaEn(Instant usadaEn) { this.usadaEn = usadaEn; }
}
