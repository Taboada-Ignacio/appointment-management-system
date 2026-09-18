package com.apiturnos.autogestion;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import static com.apiturnos.autogestion.AutogestionDtos.*;

@RestController
@RequestMapping("/api/autogestion")
public class AutogestionController {
    private final AutogestionService service;
    public AutogestionController(AutogestionService service) { this.service=service; }
    @PostMapping("/sesiones")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Credencial abrir(@Valid @RequestBody Abrir request) { return service.abrir(request.token()); }
    @GetMapping("/sesion")
    public EstadoSesion estado(@RequestHeader("Authorization") String auth) { return service.estadoSesion(bearer(auth)); }
    @PostMapping("/cliente/identificacion")
    public ClientePublico identificar(@RequestHeader("Authorization") String auth, @Valid @RequestBody Identificar request) {
        return service.identificar(bearer(auth),request);
    }
    @PostMapping("/cliente")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ClientePublico nuevo(@RequestHeader("Authorization") String auth, @Valid @RequestBody Nuevo request) {
        return service.registrar(bearer(auth),request);
    }
    @PutMapping("/cliente/contacto")
    public ClientePublico contacto(@RequestHeader("Authorization") String auth, @Valid @RequestBody Contacto request) {
        return service.contacto(bearer(auth),request);
    }
    @GetMapping("/configuracion")
    public ConfiguracionPublica configuracion(@RequestHeader("Authorization") String auth) { return service.configuracion(bearer(auth)); }
    @GetMapping("/meses")
    public List<YearMonth> meses(@RequestHeader("Authorization") String auth) { return service.mesesActivos(bearer(auth)); }
    @GetMapping("/dias")
    public List<DiaPublico> dias(@RequestHeader("Authorization") String auth, @RequestParam(required=false) String mes) {
        YearMonth elegido;
        try { elegido=mes==null?null:YearMonth.parse(mes); }
        catch(java.time.format.DateTimeParseException e) { throw new AutogestionException(400,"Mes inválido; usá AAAA-MM"); }
        return service.diasMes(bearer(auth),elegido);
    }
    @GetMapping("/fechas")
    public List<LocalDate> fechas(@RequestHeader("Authorization") String auth, @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return service.fechas(bearer(auth),desde,hasta);
    }
    @GetMapping("/brechas")
    public List<Brecha> brechas(@RequestHeader("Authorization") String auth, @RequestParam LocalDate fecha) { return service.brechas(bearer(auth),fecha); }
    @PostMapping("/intervalos")
    public Credencial elegir(@RequestHeader("Authorization") String auth, @Valid @RequestBody Elegir request) {
        return service.elegir(bearer(auth),request);
    }
    @GetMapping("/turnos")
    public List<TurnoPublico> misTurnos(@RequestHeader("Authorization") String auth) { return service.misTurnos(bearer(auth)); }
    @PostMapping("/turnos")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Resultado confirmar(@RequestHeader("Authorization") String auth,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody Confirmar request) {
        return service.confirmar(bearer(auth),key,request);
    }
    // Deliberately retain a tombstone rather than an unprotected alternative reservation flow.
    @RequestMapping("/profesionales/{id}/**")
    public void legacy() { throw new AutogestionException(410,"Usá el flujo tokenizado de autogestión"); }

    static String bearer(String auth) {
        if(auth==null || !auth.startsWith("Bearer ")) throw new AutogestionException(401,"Se requiere una sesión");
        String token=auth.substring(7); TokensAutogestion.validar(token); return token;
    }
}
