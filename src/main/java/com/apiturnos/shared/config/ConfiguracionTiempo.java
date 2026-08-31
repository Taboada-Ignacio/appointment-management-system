package com.apiturnos.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ConfiguracionTiempo {

    @Bean
    Clock clock(@Value("${turnos.zona-horaria:America/Argentina/Buenos_Aires}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
