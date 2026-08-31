-- =============================================================================
-- V5: Entidad TipoAtencion y Capacidad Simultánea
-- =============================================================================

CREATE TABLE tipo_atencion (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    duracion_minutos INT NOT NULL,
    capacidad_simultanea INT NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tipo_atencion_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE RESTRICT,
    CONSTRAINT chk_tipo_atencion_duracion CHECK (duracion_minutos > 0),
    CONSTRAINT chk_tipo_atencion_capacidad CHECK (capacidad_simultanea >= 1)
);

CREATE INDEX idx_tipo_atencion_profesional ON tipo_atencion (profesional_id);
CREATE INDEX idx_tipo_atencion_profesional_activo ON tipo_atencion (profesional_id, activo);

-- Asociar Turno con TipoAtencion
ALTER TABLE turno ADD COLUMN tipo_atencion_id BIGINT;
ALTER TABLE turno ADD CONSTRAINT fk_turno_tipo_atencion FOREIGN KEY (tipo_atencion_id) REFERENCES tipo_atencion (id) ON DELETE RESTRICT;
CREATE INDEX idx_turno_tipo_atencion ON turno (tipo_atencion_id);

