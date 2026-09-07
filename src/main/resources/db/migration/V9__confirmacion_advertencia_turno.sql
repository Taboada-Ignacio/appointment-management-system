CREATE TABLE confirmacion_advertencia_turno (
    id UUID PRIMARY KEY,
    huella VARCHAR(64) NOT NULL,
    vence_en TIMESTAMPTZ NOT NULL,
    usada BOOLEAN NOT NULL DEFAULT FALSE,
    usada_en TIMESTAMPTZ
);
CREATE INDEX idx_confirmacion_advertencia_vence ON confirmacion_advertencia_turno (vence_en);
