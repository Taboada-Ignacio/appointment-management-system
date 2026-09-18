package com.apiturnos.autogestion;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AutogestionDtos {
    private AutogestionDtos() {}
    public record Abrir(@NotBlank @Size(max=43) String token) {}
    public record Identificar(@NotBlank @Pattern(regexp="[0-9]{6,12}") String dni) {}
    public record Nuevo(@NotBlank @Size(max=100) String nombre,
                        @NotBlank @Size(max=100) String apellido,
                        @NotBlank @Email @Size(max=150) String email,
                        @NotBlank @Pattern(regexp="[+0-9() .-]{6,50}") String telefono) {}
    public record Contacto(@NotBlank @Email @Size(max=150) String email,
                           @NotBlank @Size(max=50) String telefono,
                           @Size(max=100) String nombre, @Size(max=100) String apellido) {
        public Contacto(String email, String telefono) { this(email,telefono,null,null); }
    }
    public record Elegir(@NotNull LocalDate fecha,
                         @NotNull LocalTime horaInicio) {}
    public record Confirmar(@NotBlank @Size(max=43) String referencia,
                            @Size(max=1000) String observaciones) {}
    public record Credencial(String token, Instant vence) {}
    public record ClientePublico(String estado, boolean puedeReservar, String nombre,
                                 String apellido, String email, String telefono) {}
    public record DiaPublico(Long id, LocalDate fecha, String estadoActual, boolean seleccionable) {}
    public record ConfiguracionPublica(int duracionMinutos, int capacidadSimultanea) {}
    public record Horario(LocalTime inicio, LocalTime fin) {}
    public record Brecha(LocalTime inicio, LocalTime fin, List<Horario> intervalos) {}
    public record Resultado(Long turnoId, String estado, Instant inicio, Instant fin) {}
    public record TurnoPublico(Long turnoId, String estado, Instant inicio, Instant fin, String tipoAtencion) {}
    public record Enlace(boolean activo, boolean recuperable, String token) {}
    public record ProfesionalPublico(String nombre, String apellido, String especialidad, String telefono, String zonaHoraria) {}
    public record EstadoSesion(ProfesionalPublico profesional, ClientePublico cliente, String dni, Resultado resultado, String claveResultado) {}
}
