INSERT INTO estado (nombre, ambito)
SELECT 'AFECTADO_POR_EXCEPCION', 'TURNO'
WHERE NOT EXISTS (
    SELECT 1 FROM estado WHERE nombre = 'AFECTADO_POR_EXCEPCION' AND ambito = 'TURNO'
);

CREATE TABLE afectacion_turno_excepcion (
    id BIGSERIAL PRIMARY KEY,
    excepcion_agenda_id BIGINT NOT NULL,
    turno_id BIGINT NOT NULL,
    estado_resolucion VARCHAR(30) NOT NULL,
    estado_turno_anterior VARCHAR(50) NOT NULL,
    dia_agenda_anterior_id BIGINT NOT NULL,
    inicio_anterior TIMESTAMP WITH TIME ZONE NOT NULL,
    fin_anterior TIMESTAMP WITH TIME ZONE NOT NULL,
    observacion TEXT,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelto_en TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_afectacion_excepcion FOREIGN KEY (excepcion_agenda_id) REFERENCES excepcion_agenda(id) ON DELETE RESTRICT,
    CONSTRAINT fk_afectacion_turno FOREIGN KEY (turno_id) REFERENCES turno(id) ON DELETE RESTRICT,
    CONSTRAINT fk_afectacion_dia_anterior FOREIGN KEY (dia_agenda_anterior_id) REFERENCES dia_agenda(id) ON DELETE RESTRICT,
    CONSTRAINT uk_afectacion_excepcion_turno UNIQUE (excepcion_agenda_id, turno_id),
    CONSTRAINT chk_afectacion_estado CHECK (estado_resolucion IN ('PENDIENTE', 'DADO_DE_BAJA', 'REPROGRAMADO', 'RESTAURADO'))
);

CREATE INDEX idx_afectacion_estado ON afectacion_turno_excepcion (estado_resolucion, id);
CREATE INDEX idx_afectacion_turno ON afectacion_turno_excepcion (turno_id);
