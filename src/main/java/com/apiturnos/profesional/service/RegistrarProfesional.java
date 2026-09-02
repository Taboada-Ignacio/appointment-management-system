package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.ProfesionalDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarProfesional {

    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public RegistrarProfesional(ProfesionalRepository profesionalRepository,
                                RegistradorAuditoria registradorAuditoria) {
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Profesional ejecutar(String nombre, String apellido, String email,
                                String telefono, String especialidad, String usuario) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre del profesional es obligatorio");
        }
        if (apellido == null || apellido.isBlank()) {
            throw new NegocioException("El apellido del profesional es obligatorio");
        }
        if (email == null || email.isBlank()) {
            throw new NegocioException("El email del profesional es obligatorio");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new NegocioException("El teléfono del profesional es obligatorio");
        }

        String emailNormalizado = email.trim().toLowerCase();

        if (profesionalRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ProfesionalDuplicadoException(emailNormalizado);
        }

        Profesional profesional = new Profesional();
        profesional.setNombre(nombre.trim());
        profesional.setApellido(apellido.trim());
        profesional.setEmail(emailNormalizado);
        profesional.setTelefono(telefono.trim());
        profesional.setEspecialidad(especialidad != null && !especialidad.isBlank() ? especialidad.trim() : null);

        profesional = profesionalRepository.save(profesional);

        registradorAuditoria.registrar("PROFESIONAL", "Profesional", profesional.getId(),
                OperacionAuditoria.CREATE, usuario, profesional.getId(), "Alta de profesional");

        return profesional;
    }
}
