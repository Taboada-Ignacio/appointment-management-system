package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.ProfesionalDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditarProfesional {

    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public EditarProfesional(ProfesionalRepository profesionalRepository,
                             RegistradorAuditoria registradorAuditoria) {
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Profesional ejecutar(Long id, String nombre, String apellido, String email,
                                String telefono, String especialidad, String usuario) {
        if (id == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
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

        Profesional profesional = profesionalRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", id));

        String emailNormalizado = email.trim().toLowerCase();

        if (!emailNormalizado.equalsIgnoreCase(profesional.getEmail()) &&
                profesionalRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, id)) {
            throw new ProfesionalDuplicadoException(emailNormalizado);
        }

        profesional.setNombre(nombre.trim());
        profesional.setApellido(apellido.trim());
        profesional.setEmail(emailNormalizado);
        profesional.setTelefono(telefono.trim());
        profesional.setEspecialidad(especialidad != null && !especialidad.isBlank() ? especialidad.trim() : null);

        profesional = profesionalRepository.save(profesional);

        registradorAuditoria.registrar("PROFESIONAL", "Profesional", profesional.getId(),
                OperacionAuditoria.UPDATE, usuario, profesional.getId(), "Datos de profesional actualizados");

        return profesional;
    }
}
