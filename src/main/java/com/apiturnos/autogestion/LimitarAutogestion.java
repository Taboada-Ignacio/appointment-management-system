package com.apiturnos.autogestion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class LimitarAutogestion {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public LimitarAutogestion(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    // Commit attempts even if identification or reservation subsequently fails.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean permitido(String bucket, int max) {
        long window = clock.instant().getEpochSecond() / 60;
        jdbc.update("DELETE FROM autogestion_limite WHERE ventana < ?", window - 60);
        Integer attempts = jdbc.queryForObject("""
            INSERT INTO autogestion_limite(huella, ventana, intentos) VALUES (?, ?, 1)
            ON CONFLICT (huella, ventana) DO UPDATE SET intentos = autogestion_limite.intentos + 1
            RETURNING intentos
            """, Integer.class, TokensAutogestion.huella(bucket), window);
        return attempts != null && attempts <= max;
    }
}
