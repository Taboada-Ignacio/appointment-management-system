-- =============================================================================
-- MIGRACIÓN V2: Refactorización del modelo de dominio
-- Implementa la jerarquía: Profesional → AgendaAnual → MesAgenda → DiaAgenda → Turno
-- Modelo unificado de estados: Estado + CambioEstado con ámbito
-- PostgreSQL 17
-- =============================================================================
-- IMPORTANTE: Esta migración asume que NO existen datos productivos (Fase 1).
-- Las tablas obsoletas se eliminan y recrean con la nueva estructura.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- FASE 1: Eliminar foreign keys de tablas que se van a modificar/eliminar
-- -----------------------------------------------------------------------------

ALTER TABLE notificacion DROP CONSTRAINT fk_notificacion_turno;
ALTER TABLE notificacion DROP CONSTRAINT fk_notificacion_cliente;

ALTER TABLE turno DROP CONSTRAINT fk_turno_agenda;
ALTER TABLE turno DROP CONSTRAINT fk_turno_cliente;
ALTER TABLE turno DROP CONSTRAINT fk_turno_agenda_excepcion;

ALTER TABLE turno_historial DROP CONSTRAINT fk_turno_historial_turno;

ALTER TABLE agenda_horario DROP CONSTRAINT fk_agenda_horario_agenda;

ALTER TABLE agenda_excepcion DROP CONSTRAINT fk_agenda_excepcion_agenda;

ALTER TABLE cliente_historial_estado DROP CONSTRAINT fk_cliente_historial_cliente;

-- -----------------------------------------------------------------------------
-- FASE 2: Eliminar tablas obsoletas
-- -----------------------------------------------------------------------------

DROP TABLE turno_historial;
DROP TABLE turno;
DROP TABLE cliente_historial_estado;
DROP TABLE agenda_horario;
DROP TABLE agenda_excepcion;
DROP TABLE agenda;

-- -----------------------------------------------------------------------------
-- FASE 3: Tabla profesional
-- -----------------------------------------------------------------------------

CREATE TABLE profesional (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    especialidad VARCHAR(100),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_profesional_email UNIQUE (email)
);

CREATE INDEX idx_profesional_email ON profesional (email);

-- -----------------------------------------------------------------------------
-- FASE 4: Tabla configuracion (1:1 con profesional)
-- -----------------------------------------------------------------------------

CREATE TABLE configuracion (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    cantidad_max_turnos_a_la_vez INTEGER NOT NULL DEFAULT 1,
    duracion_aproximada_por_turno INTEGER NOT NULL DEFAULT 30,
    agenda_solo_manejada_por_profesional BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_configuracion_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE RESTRICT,
    CONSTRAINT uk_configuracion_profesional UNIQUE (profesional_id),
    CONSTRAINT chk_configuracion_max_turnos CHECK (cantidad_max_turnos_a_la_vez > 0),
    CONSTRAINT chk_configuracion_duracion CHECK (duracion_aproximada_por_turno > 0)
);

-- -----------------------------------------------------------------------------
-- FASE 5: Tabla agenda_anual
-- -----------------------------------------------------------------------------

CREATE TABLE agenda_anual (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    anio INTEGER NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agenda_anual_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE RESTRICT,
    CONSTRAINT uk_agenda_anual_profesional_anio UNIQUE (profesional_id, anio),
    CONSTRAINT chk_agenda_anual_anio CHECK (anio >= 2000 AND anio <= 2100)
);

-- -----------------------------------------------------------------------------
-- FASE 6: Tabla mes_agenda
-- -----------------------------------------------------------------------------

CREATE TABLE mes_agenda (
    id BIGSERIAL PRIMARY KEY,
    agenda_anual_id BIGINT NOT NULL,
    nro_mes INTEGER NOT NULL,
    repetir_configuracion BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_mes_agenda_agenda_anual FOREIGN KEY (agenda_anual_id) REFERENCES agenda_anual (id) ON DELETE RESTRICT,
    CONSTRAINT uk_mes_agenda_agenda_mes UNIQUE (agenda_anual_id, nro_mes),
    CONSTRAINT chk_mes_agenda_nro_mes CHECK (nro_mes BETWEEN 1 AND 12)
);

-- -----------------------------------------------------------------------------
-- FASE 7: Tabla dia_agenda
-- -----------------------------------------------------------------------------

CREATE TABLE dia_agenda (
    id BIGSERIAL PRIMARY KEY,
    mes_agenda_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    CONSTRAINT fk_dia_agenda_mes_agenda FOREIGN KEY (mes_agenda_id) REFERENCES mes_agenda (id) ON DELETE RESTRICT,
    CONSTRAINT uk_dia_agenda_mes_fecha UNIQUE (mes_agenda_id, fecha)
);

CREATE INDEX idx_dia_agenda_fecha ON dia_agenda (fecha);

-- -----------------------------------------------------------------------------
-- FASE 8: Tabla brecha_horaria
-- -----------------------------------------------------------------------------

CREATE TABLE brecha_horaria (
    id BIGSERIAL PRIMARY KEY,
    dia_agenda_id BIGINT NOT NULL,
    hora_inicio_atencion TIME NOT NULL,
    hora_fin_atencion TIME NOT NULL,
    CONSTRAINT fk_brecha_horaria_dia_agenda FOREIGN KEY (dia_agenda_id) REFERENCES dia_agenda (id) ON DELETE RESTRICT,
    CONSTRAINT chk_brecha_horaria_horas CHECK (hora_fin_atencion > hora_inicio_atencion)
);

CREATE INDEX idx_brecha_horaria_dia_agenda ON brecha_horaria (dia_agenda_id);

-- -----------------------------------------------------------------------------
-- FASE 9: Tabla estado (catálogo con ámbito)
-- -----------------------------------------------------------------------------

CREATE TABLE estado (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    ambito VARCHAR(20) NOT NULL,
    CONSTRAINT uk_estado_nombre_ambito UNIQUE (nombre, ambito),
    CONSTRAINT chk_estado_ambito CHECK (ambito IN ('CLIENTE', 'TURNO', 'DIA_AGENDA', 'MES_AGENDA'))
);

-- -----------------------------------------------------------------------------
-- FASE 10: Tabla motivo_baja_turno
-- -----------------------------------------------------------------------------

CREATE TABLE motivo_baja_turno (
    id BIGSERIAL PRIMARY KEY,
    motivo VARCHAR(255) NOT NULL
);

-- -----------------------------------------------------------------------------
-- FASE 11: Tabla cambio_estado (historial unificado)
-- -----------------------------------------------------------------------------

CREATE TABLE cambio_estado (
    id BIGSERIAL PRIMARY KEY,
    estado_id BIGINT NOT NULL,
    ambito VARCHAR(20) NOT NULL,
    entidad_id BIGINT NOT NULL,
    fecha_hora_inicio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_hora_fin TIMESTAMPTZ,
    motivo_baja_turno_id BIGINT,
    usuario VARCHAR(100),
    observacion TEXT,
    CONSTRAINT fk_cambio_estado_estado FOREIGN KEY (estado_id) REFERENCES estado (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cambio_estado_motivo FOREIGN KEY (motivo_baja_turno_id) REFERENCES motivo_baja_turno (id) ON DELETE SET NULL,
    CONSTRAINT chk_cambio_estado_ambito CHECK (ambito IN ('CLIENTE', 'TURNO', 'DIA_AGENDA', 'MES_AGENDA'))
);

CREATE INDEX idx_cambio_estado_entidad ON cambio_estado (ambito, entidad_id, fecha_hora_inicio DESC);
CREATE INDEX idx_cambio_estado_estado ON cambio_estado (estado_id);
CREATE INDEX idx_cambio_estado_fecha ON cambio_estado (fecha_hora_inicio);

-- -----------------------------------------------------------------------------
-- FASE 12: Tabla excepcion_agenda (pertenece a Profesional)
-- -----------------------------------------------------------------------------

CREATE TABLE excepcion_agenda (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    motivo TEXT NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_excepcion_agenda_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE RESTRICT,
    CONSTRAINT chk_excepcion_agenda_tipo CHECK (tipo IN ('VACACIONES', 'FERIADO', 'DIA_DADO_DE_BAJA', 'EXCEPCION_HORARIA', 'OTRO')),
    CONSTRAINT chk_excepcion_agenda_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE INDEX idx_excepcion_agenda_profesional_fechas ON excepcion_agenda (profesional_id, fecha_inicio, fecha_fin);

-- -----------------------------------------------------------------------------
-- FASE 13: Modificar tabla cliente
-- -----------------------------------------------------------------------------

-- Eliminar constraints obsoletos
ALTER TABLE cliente DROP CONSTRAINT uk_cliente_numero_documento;
ALTER TABLE cliente DROP CONSTRAINT uk_cliente_email;
ALTER TABLE cliente DROP CONSTRAINT chk_cliente_estado_actual;
ALTER TABLE cliente DROP CONSTRAINT chk_cliente_estado_anterior;

-- Eliminar índices obsoletos
DROP INDEX idx_cliente_numero_documento;
DROP INDEX idx_cliente_email;
DROP INDEX idx_cliente_estado_actual;

-- Agregar FK a profesional
ALTER TABLE cliente ADD COLUMN profesional_id BIGINT NOT NULL;
ALTER TABLE cliente ADD CONSTRAINT fk_cliente_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE RESTRICT;

-- Eliminar columnas de estado embebido
ALTER TABLE cliente DROP COLUMN estado_actual;
ALTER TABLE cliente DROP COLUMN estado_anterior;
ALTER TABLE cliente DROP COLUMN motivo_inhabilitacion;
ALTER TABLE cliente DROP COLUMN motivo_baja;

-- Nueva unicidad compuesta
ALTER TABLE cliente ADD CONSTRAINT uk_cliente_profesional_documento UNIQUE (profesional_id, numero_documento);

-- Recrear índices
CREATE INDEX idx_cliente_profesional ON cliente (profesional_id);
CREATE INDEX idx_cliente_numero_documento ON cliente (numero_documento);

-- Restaurar FK de notificacion a cliente
ALTER TABLE notificacion ADD CONSTRAINT fk_notificacion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT;

-- -----------------------------------------------------------------------------
-- FASE 14: Tabla turno (nueva estructura)
-- -----------------------------------------------------------------------------

CREATE TABLE turno (
    id BIGSERIAL PRIMARY KEY,
    dia_agenda_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    inicio_estimado TIMESTAMPTZ NOT NULL,
    fin_estimado TIMESTAMPTZ NOT NULL,
    inicio_real TIMESTAMPTZ,
    fin_real TIMESTAMPTZ,
    origen VARCHAR(30) NOT NULL,
    observaciones TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_turno_dia_agenda FOREIGN KEY (dia_agenda_id) REFERENCES dia_agenda (id) ON DELETE RESTRICT,
    CONSTRAINT fk_turno_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT chk_turno_fechas_estimadas CHECK (fin_estimado > inicio_estimado),
    CONSTRAINT chk_turno_origen CHECK (origen IN ('PROFESIONAL', 'CLIENTE_AUTOGESTION'))
);

CREATE INDEX idx_turno_dia_agenda ON turno (dia_agenda_id);
CREATE INDEX idx_turno_cliente ON turno (cliente_id);
CREATE INDEX idx_turno_inicio_estimado ON turno (inicio_estimado);

-- Restaurar FK de notificacion a turno
ALTER TABLE notificacion ADD CONSTRAINT fk_notificacion_turno FOREIGN KEY (turno_id) REFERENCES turno (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- FASE 15: Tabla turno_historial (simplificada: solo datos de reprogramación)
-- -----------------------------------------------------------------------------

CREATE TABLE turno_historial (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL,
    dia_agenda_anterior_id BIGINT,
    inicio_estimado_anterior TIMESTAMPTZ,
    fin_estimado_anterior TIMESTAMPTZ,
    inicio_estimado_nuevo TIMESTAMPTZ,
    fin_estimado_nuevo TIMESTAMPTZ,
    motivo TEXT,
    usuario VARCHAR(100) NOT NULL,
    fecha_evento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_turno_historial_turno FOREIGN KEY (turno_id) REFERENCES turno (id) ON DELETE RESTRICT,
    CONSTRAINT fk_turno_historial_dia_anterior FOREIGN KEY (dia_agenda_anterior_id) REFERENCES dia_agenda (id) ON DELETE SET NULL
);

CREATE INDEX idx_turno_historial_turno ON turno_historial (turno_id);
CREATE INDEX idx_turno_historial_fecha ON turno_historial (fecha_evento);

-- -----------------------------------------------------------------------------
-- FASE 16: Modificar tabla auditoria_evento
-- -----------------------------------------------------------------------------

ALTER TABLE auditoria_evento ADD COLUMN profesional_id BIGINT;
CREATE INDEX idx_auditoria_profesional ON auditoria_evento (profesional_id);

-- -----------------------------------------------------------------------------
-- FASE 17: Insertar catálogo de estados
-- -----------------------------------------------------------------------------

-- Estados del Cliente
INSERT INTO estado (nombre, ambito) VALUES ('HABILITADO', 'CLIENTE');
INSERT INTO estado (nombre, ambito) VALUES ('PENDIENTE_DE_VERIFICACION', 'CLIENTE');
INSERT INTO estado (nombre, ambito) VALUES ('REQUIERE_APROBACION', 'CLIENTE');
INSERT INTO estado (nombre, ambito) VALUES ('INHABILITADO', 'CLIENTE');
INSERT INTO estado (nombre, ambito) VALUES ('DADO_DE_BAJA', 'CLIENTE');

-- Estados del Turno
INSERT INTO estado (nombre, ambito) VALUES ('ASIGNADO', 'TURNO');
INSERT INTO estado (nombre, ambito) VALUES ('PENDIENTE_DE_APROBACION', 'TURNO');
INSERT INTO estado (nombre, ambito) VALUES ('CONFIRMADO', 'TURNO');
INSERT INTO estado (nombre, ambito) VALUES ('REPROGRAMADO', 'TURNO');
INSERT INTO estado (nombre, ambito) VALUES ('CANCELADO', 'TURNO');
INSERT INTO estado (nombre, ambito) VALUES ('COMPLETADO', 'TURNO');
INSERT INTO estado (nombre, ambito) VALUES ('NO_ASISTIO', 'TURNO');
INSERT INTO estado (nombre, ambito) VALUES ('DADO_DE_BAJA', 'TURNO');

-- Estados del DiaAgenda
INSERT INTO estado (nombre, ambito) VALUES ('ACTIVO', 'DIA_AGENDA');
INSERT INTO estado (nombre, ambito) VALUES ('INACTIVO', 'DIA_AGENDA');
INSERT INTO estado (nombre, ambito) VALUES ('EN_TRANSCURSO', 'DIA_AGENDA');
INSERT INTO estado (nombre, ambito) VALUES ('FINALIZADO', 'DIA_AGENDA');

-- Estados del MesAgenda
INSERT INTO estado (nombre, ambito) VALUES ('ACTIVO', 'MES_AGENDA');
INSERT INTO estado (nombre, ambito) VALUES ('INACTIVO', 'MES_AGENDA');
