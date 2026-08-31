package com.apiturnos.estado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(name = "estado", uniqueConstraints = @UniqueConstraint(name = "uk_estado_nombre_ambito", columnNames = {"nombre", "ambito"}))
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambito", nullable = false, length = 20)
    private AmbitoEstado ambito;

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

    public AmbitoEstado getAmbito() {
        return ambito;
    }

    public void setAmbito(AmbitoEstado ambito) {
        this.ambito = ambito;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Estado)) return false;
        Estado estado = (Estado) o;
        return id != null && id.equals(estado.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
