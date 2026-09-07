package com.apiturnos.turno.service;

import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.ConfirmacionAdvertenciaTurno;
import com.apiturnos.turno.repository.ConfirmacionAdvertenciaTurnoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class TokenConfirmacionTurnoManual {
    private final ConfirmacionAdvertenciaTurnoRepository repository;
    private final Clock clock;
    private final byte[] secreto;
    private final Duration vigencia;

    public TokenConfirmacionTurnoManual(ConfirmacionAdvertenciaTurnoRepository repository, Clock clock,
            @Value("${turnos.confirmacion.secreto:cambiar-este-secreto-en-produccion-32b}") String secreto,
            @Value("${turnos.confirmacion.vigencia:PT5M}") Duration vigencia) {
        this.repository = repository;
        this.clock = clock;
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalArgumentException("TURNOS_CONFIRMACION_SECRETO debe tener al menos 32 caracteres");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.vigencia = vigencia;
    }

    public String emitir(SolicitudCrearTurnoManual solicitud, List<AdvertenciaTurnoManual> advertencias,
                         int capacidad, int concurrencia) {
        UUID id = UUID.randomUUID();
        Instant vence = clock.instant().plus(vigencia);
        String payload = contenido(id, vence, solicitud, advertencias, capacidad, concurrencia);
        String token = codificar(payload) + "." + codificar(firmar(payload));
        ConfirmacionAdvertenciaTurno confirmacion = new ConfirmacionAdvertenciaTurno();
        confirmacion.setId(id);
        confirmacion.setHuella(huella(token));
        confirmacion.setVenceEn(vence);
        repository.save(confirmacion);
        return token;
    }

    public void validarYConsumir(String token, SolicitudCrearTurnoManual solicitud,
                                 List<AdvertenciaTurnoManual> advertencias, int capacidad, int concurrencia) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 2) throw new IllegalArgumentException();
            String payload = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(firmar(payload), Base64.getUrlDecoder().decode(partes[1]))) {
                throw new IllegalArgumentException();
            }
            String[] campos = payload.split("\\|", -1);
            UUID id = UUID.fromString(campos[0]);
            Instant vence = Instant.parse(campos[1]);
            String esperado = contenido(id, vence, solicitud, advertencias, capacidad, concurrencia);
            ConfirmacionAdvertenciaTurno guardada = repository.findByIdForUpdate(id)
                    .orElseThrow(IllegalArgumentException::new);
            if (!payload.equals(esperado) || guardada.isUsada() || !guardada.getVenceEn().isAfter(clock.instant())
                    || !MessageDigest.isEqual(guardada.getHuella().getBytes(StandardCharsets.UTF_8),
                    huella(token).getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException();
            }
            guardada.setUsada(true);
            guardada.setUsadaEn(clock.instant());
        } catch (RuntimeException ex) {
            throw new NegocioException("La confirmación venció, fue utilizada o las condiciones cambiaron; valide nuevamente");
        }
    }

    private String contenido(UUID id, Instant vence, SolicitudCrearTurnoManual s,
                             List<AdvertenciaTurnoManual> advertencias, int capacidad, int concurrencia) {
        String avisos = advertencias.stream().sorted(Comparator.comparing(Enum::name))
                .map(Enum::name).reduce((a, b) -> a + "," + b).orElse("");
        return String.join("|", id.toString(), vence.toString(), String.valueOf(s.profesionalId()),
                String.valueOf(s.diaAgendaId()), String.valueOf(s.clienteId()), String.valueOf(s.tipoAtencionId()),
                s.inicioEstimado().toString(), s.finEstimado().toString(), String.valueOf(capacidad),
                String.valueOf(concurrencia), avisos);
    }

    private byte[] firmar(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return mac.doFinal(valor.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) { throw new IllegalStateException("No se pudo firmar la confirmación", ex); }
    }
    private String huella(String valor) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private String codificar(String valor) { return codificar(valor.getBytes(StandardCharsets.UTF_8)); }
    private String codificar(byte[] valor) { return Base64.getUrlEncoder().withoutPadding().encodeToString(valor); }
}
