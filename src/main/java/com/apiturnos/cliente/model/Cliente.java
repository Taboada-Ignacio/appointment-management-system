package com.apiturnos.cliente.model;

import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.model.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "cliente", uniqueConstraints = @UniqueConstraint(name = "uk_cliente_profesional_documento", columnNames = {"profesional_id", "numero_documento"}))
public class Cliente extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "telefono", nullable = false, length = 50)
    private String telefono;

    @Column(name = "notificaciones_habilitadas", nullable = false)
    private Boolean notificacionesHabilitadas = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Profesional getProfesional() {
        return profesional;
    }

    public void setProfesional(Profesional profesional) {
        this.profesional = profesional;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Boolean getNotificacionesHabilitadas() {
        return notificacionesHabilitadas;
    }

    public void setNotificacionesHabilitadas(Boolean notificacionesHabilitadas) {
        this.notificacionesHabilitadas = notificacionesHabilitadas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        Cliente that = (Cliente) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
