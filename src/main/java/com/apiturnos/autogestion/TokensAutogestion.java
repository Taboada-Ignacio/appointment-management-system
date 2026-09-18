package com.apiturnos.autogestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokensAutogestion {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TokensAutogestion() {}
    public static String nuevo() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static String huella(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String validar(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{43}"))
            throw new AutogestionException(401, "Credencial inválida o vencida");
        return huella(value);
    }
    public static String telefono(String value) {
        String normalized = value == null ? "" : value.replaceAll("[\\s()+.-]", "");
        if (!normalized.matches("[0-9]{6,20}")) throw new AutogestionException(400, "Teléfono inválido");
        return normalized;
    }
}
