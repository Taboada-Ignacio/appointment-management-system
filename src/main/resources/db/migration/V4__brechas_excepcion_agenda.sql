-- =============================================================================
-- Migración V4: Soporte de múltiples brechas por excepción e índice de estado único.
-- =============================================================================

-- Tabla hija para soportar múltiples intervalos horarios por excepción (e.g. MODIFICACION_HORARIO)
CREATE TABLE brecha_excepcion (
    id BIGSERIAL PRIMARY KEY,
    excepcion_agenda_id BIGINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT fk_brecha_excepcion_excepcion
        FOREIGN KEY (excepcion_agenda_id)
        REFERENCES excepcion_agenda (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_brecha_excepcion_horas
        CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_brecha_excepcion_excepcion
    ON brecha_excepcion (excepcion_agenda_id);

-- Índice único parcial para garantizar a nivel de base de datos que cada entidad
-- tiene como máximo un único CambioEstado abierto (activo) a la vez.
CREATE UNIQUE INDEX idx_cambio_estado_actual_unico
    ON cambio_estado (ambito, entidad_id)
    WHERE fecha_hora_fin IS NULL;

-- Ajustar constraint de horas en excepcion_agenda para permitir excepciones
-- horarias donde los intervalos se definan a través de brecha_excepcion.
ALTER TABLE excepcion_agenda
    DROP CONSTRAINT chk_excepcion_agenda_tipo_horas;

ALTER TABLE excepcion_agenda
    ADD CONSTRAINT chk_excepcion_agenda_tipo_horas CHECK (
        (
            tipo IN (
                'BLOQUEO_HORARIO',
                'HABILITACION_EXTRAORDINARIA',
                'MODIFICACION_HORARIO',
                'EXCEPCION_HORARIA'
            )
        )
        OR
        (
            tipo IN (
                'DIA_NO_LABORABLE',
                'VACACIONES',
                'FERIADO',
                'DIA_DADO_DE_BAJA',
                'OTRO'
            )
            AND hora_inicio IS NULL
            AND hora_fin IS NULL
        )
    );

