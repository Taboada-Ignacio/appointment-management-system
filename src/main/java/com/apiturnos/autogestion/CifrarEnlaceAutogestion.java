package com.apiturnos.autogestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CifrarEnlaceAutogestion {
    private final String clave;
    public CifrarEnlaceAutogestion(@Value("${turnos.autogestion.clave-enlaces:}") String clave) { this.clave=clave; }
    private SecretKeySpec key() {
        try {
            byte[] value=Base64.getDecoder().decode(clave);
            if(value.length!=32) throw new IllegalArgumentException();
            return new SecretKeySpec(value,"AES");
        } catch(IllegalArgumentException e) {
            throw new AutogestionException(503,"La recuperación de enlaces aún no está configurada");
        }
    }
    public String cifrar(String token) {
        var key=key();
        try {
            byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));
            return Base64.getEncoder().encodeToString(iv)+"."+Base64.getEncoder().encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch(Exception e) { throw new IllegalStateException("No se pudo cifrar el enlace",e); }
    }
    public String descifrar(String value) {
        var key=key();
        try {
            String[] parts=value.split("\\.");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);
        } catch(Exception e) { throw new AutogestionException(503,"No se pudo recuperar el enlace. Revisá la configuración de cifrado."); }
    }
}
