package com.apiturnos.agenda.service;

import com.apiturnos.turno.model.Turno;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class TokenImpactoExcepcionAgenda {
    public String generar(SolicitudExcepcionAgenda solicitud, List<Turno> turnos) {
        String contenido = solicitud.fechaInicio() + "|" + solicitud.fechaFin() + "|"
                + solicitud.tipo() + "|" + solicitud.obtenerIntervalos() + "|" + solicitud.fechasExcluidas() + "|"
                + turnos.stream()
                    .sorted(java.util.Comparator.comparing(Turno::getId))
                    .map(t -> t.getId() + ":" + t.getInicioEstimado() + ":" + t.getFinEstimado())
                    .toList();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
