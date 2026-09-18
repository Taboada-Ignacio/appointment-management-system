package com.apiturnos.autogestion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import static com.apiturnos.autogestion.AutogestionDtos.*;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/enlace-autogestion")
public class EnlaceAutogestionController {
    private final AutogestionService service;
    public EnlaceAutogestionController(AutogestionService service) { this.service=service; }
    @GetMapping
    public Enlace consultar(@PathVariable Long profesionalId,HttpServletRequest request,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        autorizar(profesionalId,request);return service.enlace(profesionalId);
    }
    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Credencial regenerar(@PathVariable Long profesionalId, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        autorizar(profesionalId,request); return service.regenerar(profesionalId);
    }
    @DeleteMapping
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void revocar(@PathVariable Long profesionalId, HttpServletRequest request) {
        autorizar(profesionalId,request); service.revocar(profesionalId);
    }
    private void autorizar(Long profesionalId,HttpServletRequest request) {
        if(request.getUserPrincipal()==null) throw new AutogestionException(401,"Se requiere autenticación profesional");
        if(!request.isUserInRole("PROFESIONAL") || !profesionalId.toString().equals(request.getUserPrincipal().getName()))
            throw new AutogestionException(403,"No podés administrar el enlace de otro profesional");
    }
}
