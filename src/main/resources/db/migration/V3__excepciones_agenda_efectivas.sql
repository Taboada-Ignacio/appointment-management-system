-- =============================================================================
-- Excepciones de agenda calculadas sobre la configuración base.
-- =============================================================================

ALTER TABLE excepcion_agenda
    ADD COLUMN hora_inicio TIME,
    ADD COLUMN hora_fin TIME,
    ADD COLUMN activa BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fecha_modificacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- La implementación anterior trataba EXCEPCION_HORARIA sin horas como cierre
-- completo. Se conserva esa semántica al migrar datos ya existentes.
UPDATE excepcion_agenda
SET tipo = 'DIA_NO_LABORABLE'
WHERE tipo = 'EXCEPCION_HORARIA';

ALTER TABLE excepcion_agenda
    DROP CONSTRAINT chk_excepcion_agenda_tipo;

ALTER TABLE excepcion_agenda
    ADD CONSTRAINT chk_excepcion_agenda_tipo CHECK (tipo IN (
        'DIA_NO_LABORABLE',
        'VACACIONES',
        'BLOQUEO_HORARIO',
        'HABILITACION_EXTRAORDINARIA',
        'MODIFICACION_HORARIO',
        'FERIADO',
        'DIA_DADO_DE_BAJA',
        'EXCEPCION_HORARIA',
        'OTRO'
    )),
    ADD CONSTRAINT chk_excepcion_agenda_horas CHECK (
        (hora_inicio IS NULL AND hora_fin IS NULL)
        OR
        (hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_fin > hora_inicio)
    ),
    ADD CONSTRAINT chk_excepcion_agenda_tipo_horas CHECK (
        (
            tipo IN (
                'BLOQUEO_HORARIO',
                'HABILITACION_EXTRAORDINARIA',
                'MODIFICACION_HORARIO',
                'EXCEPCION_HORARIA'
            )
            AND hora_inicio IS NOT NULL
            AND hora_fin IS NOT NULL
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

CREATE INDEX idx_excepcion_agenda_activas_rango
    ON excepcion_agenda (profesional_id, fecha_inicio, fecha_fin, id)
    WHERE activa = TRUE;

ALTER TABLE motivo_baja_turno
    ADD COLUMN excepcion_agenda_id BIGINT,
    ADD CONSTRAINT fk_motivo_baja_turno_excepcion
        FOREIGN KEY (excepcion_agenda_id)
        REFERENCES excepcion_agenda (id)
        ON DELETE RESTRICT;

CREATE INDEX idx_motivo_baja_turno_excepcion
    ON motivo_baja_turno (excepcion_agenda_id);

CREATE INDEX idx_turno_dia_intervalo
    ON turno (dia_agenda_id, inicio_estimado, fin_estimado);
