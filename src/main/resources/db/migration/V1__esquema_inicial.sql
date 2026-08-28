-- =============================================================================
-- MIGRACIÓN INICIAL V1: Estructura de Base de Datos para api-turnos
-- PostgreSQL 17
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. TABLA: cliente
-- -----------------------------------------------------------------------------
CREATE TABLE cliente (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(30) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    notificaciones_habilitadas BOOLEAN NOT NULL DEFAULT TRUE,
    estado_actual VARCHAR(30) NOT NULL,
    estado_anterior VARCHAR(30),
    motivo_inhabilitacion TEXT,
    motivo_baja TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_cliente_numero_documento UNIQUE (numero_documento),
    CONSTRAINT uk_cliente_email UNIQUE (email),
    CONSTRAINT chk_cliente_estado_actual CHECK (estado_actual IN ('ACTIVO', 'INHABILITADO', 'DADO_DE_BAJA')),
    CONSTRAINT chk_cliente_estado_anterior CHECK (estado_anterior IS NULL OR estado_anterior IN ('ACTIVO', 'INHABILITADO', 'DADO_DE_BAJA'))
);

CREATE INDEX idx_cliente_numero_documento ON cliente (numero_documento);
CREATE INDEX idx_cliente_email ON cliente (email);
CREATE INDEX idx_cliente_estado_actual ON cliente (estado_actual);

-- -----------------------------------------------------------------------------
-- 2. TABLA: cliente_historial_estado
-- -----------------------------------------------------------------------------
CREATE TABLE cliente_historial_estado (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    motivo TEXT,
    usuario VARCHAR(100) NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente_historial_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT chk_cliente_historial_estado_nuevo CHECK (estado_nuevo IN ('ACTIVO', 'INHABILITADO', 'DADO_DE_BAJA'))
);

CREATE INDEX idx_cliente_historial_cliente_id ON cliente_historial_estado (cliente_id);
CREATE INDEX idx_cliente_historial_fecha_cambio ON cliente_historial_estado (fecha_cambio);

-- -----------------------------------------------------------------------------
-- 3. TABLA: agenda
-- -----------------------------------------------------------------------------
CREATE TABLE agenda (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    duracion_turno_minutos INTEGER NOT NULL,
    tiempo_entre_turnos_minutos INTEGER NOT NULL DEFAULT 0,
    anticipacion_maxima_dias INTEGER NOT NULL DEFAULT 30,
    anticipacion_minima_horas INTEGER NOT NULL DEFAULT 2,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_agenda_duracion_turno CHECK (duracion_turno_minutos > 0),
    CONSTRAINT chk_agenda_tiempo_entre_turnos CHECK (tiempo_entre_turnos_minutos >= 0),
    CONSTRAINT chk_agenda_anticipacion_maxima CHECK (anticipacion_maxima_dias > 0),
    CONSTRAINT chk_agenda_anticipacion_minima CHECK (anticipacion_minima_horas >= 0)
);

CREATE INDEX idx_agenda_activa ON agenda (activa);

-- -----------------------------------------------------------------------------
-- 4. TABLA: agenda_horario
-- -----------------------------------------------------------------------------
CREATE TABLE agenda_horario (
    id BIGSERIAL PRIMARY KEY,
    agenda_id BIGINT NOT NULL,
    dia_semana INTEGER NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_agenda_horario_agenda FOREIGN KEY (agenda_id) REFERENCES agenda (id) ON DELETE RESTRICT,
    CONSTRAINT chk_agenda_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_agenda_horario_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT uk_agenda_horario_rango UNIQUE (agenda_id, dia_semana, hora_inicio, hora_fin)
);

CREATE INDEX idx_agenda_horario_agenda_dia ON agenda_horario (agenda_id, dia_semana);

-- -----------------------------------------------------------------------------
-- 5. TABLA: agenda_excepcion (Vacaciones, feriados, bloqueos, días dados de baja)
-- -----------------------------------------------------------------------------
CREATE TABLE agenda_excepcion (
    id BIGSERIAL PRIMARY KEY,
    agenda_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    fecha_inicio TIMESTAMPTZ NOT NULL,
    fecha_fin TIMESTAMPTZ NOT NULL,
    motivo TEXT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agenda_excepcion_agenda FOREIGN KEY (agenda_id) REFERENCES agenda (id) ON DELETE RESTRICT,
    CONSTRAINT chk_agenda_excepcion_tipo CHECK (tipo IN ('VACACIONES', 'FERIADO', 'DIA_DADO_DE_BAJA', 'EXCEPCION_HORARIA', 'OTRO')),
    CONSTRAINT chk_agenda_excepcion_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE INDEX idx_agenda_excepcion_agenda_fechas ON agenda_excepcion (agenda_id, fecha_inicio, fecha_fin);

-- -----------------------------------------------------------------------------
-- 6. TABLA: turno
-- -----------------------------------------------------------------------------
CREATE TABLE turno (
    id BIGSERIAL PRIMARY KEY,
    agenda_id BIGINT NOT NULL,
    cliente_id BIGINT,
    fecha_hora_inicio TIMESTAMPTZ NOT NULL,
    fecha_hora_fin TIMESTAMPTZ NOT NULL,
    estado VARCHAR(30) NOT NULL,
    motivo_baja TEXT,
    origen_baja VARCHAR(50),
    agenda_excepcion_id BIGINT,
    observaciones TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_turno_agenda FOREIGN KEY (agenda_id) REFERENCES agenda (id) ON DELETE RESTRICT,
    CONSTRAINT fk_turno_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT fk_turno_agenda_excepcion FOREIGN KEY (agenda_excepcion_id) REFERENCES agenda_excepcion (id) ON DELETE SET NULL,
    CONSTRAINT chk_turno_fechas CHECK (fecha_hora_fin > fecha_hora_inicio),
    CONSTRAINT chk_turno_estado CHECK (estado IN ('DISPONIBLE', 'ASIGNADO', 'CONFIRMADO', 'CANCELADO', 'COMPLETADO', 'NO_ASISTIO', 'DADO_DE_BAJA')),
    CONSTRAINT chk_turno_baja_datos CHECK (estado != 'DADO_DE_BAJA' OR (motivo_baja IS NOT NULL AND origen_baja IS NOT NULL))
);

CREATE INDEX idx_turno_agenda_fechas ON turno (agenda_id, fecha_hora_inicio, fecha_hora_fin);
CREATE INDEX idx_turno_cliente_id ON turno (cliente_id);
CREATE INDEX idx_turno_estado ON turno (estado);
CREATE INDEX idx_turno_agenda_excepcion_id ON turno (agenda_excepcion_id);

-- -----------------------------------------------------------------------------
-- 7. TABLA: turno_historial (Eventos y cambios de estado del turno)
-- -----------------------------------------------------------------------------
CREATE TABLE turno_historial (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    estado_resultante VARCHAR(30) NOT NULL,
    fecha_hora_inicio_anterior TIMESTAMPTZ,
    fecha_hora_fin_anterior TIMESTAMPTZ,
    fecha_hora_inicio_nueva TIMESTAMPTZ,
    fecha_hora_fin_nueva TIMESTAMPTZ,
    motivo TEXT,
    usuario VARCHAR(100) NOT NULL,
    origen VARCHAR(50),
    fecha_evento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_turno_historial_turno FOREIGN KEY (turno_id) REFERENCES turno (id) ON DELETE RESTRICT,
    CONSTRAINT chk_turno_historial_tipo_evento CHECK (tipo_evento IN ('CREACION', 'ASIGNACION', 'CONFIRMACION', 'REPROGRAMADO', 'CANCELACION', 'BAJA', 'COMPLETADO', 'NO_ASISTIO')),
    CONSTRAINT chk_turno_historial_estado_resultante CHECK (estado_resultante IN ('DISPONIBLE', 'ASIGNADO', 'CONFIRMADO', 'CANCELADO', 'COMPLETADO', 'NO_ASISTIO', 'DADO_DE_BAJA'))
);

CREATE INDEX idx_turno_historial_turno_id ON turno_historial (turno_id);
CREATE INDEX idx_turno_historial_fecha_evento ON turno_historial (fecha_evento);

-- -----------------------------------------------------------------------------
-- 8. TABLA: notificacion
-- -----------------------------------------------------------------------------
CREATE TABLE notificacion (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT,
    cliente_id BIGINT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    canal VARCHAR(30) NOT NULL,
    destinatario VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_programada TIMESTAMPTZ NOT NULL,
    fecha_envio TIMESTAMPTZ,
    error_mensaje TEXT,
    intentos INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificacion_turno FOREIGN KEY (turno_id) REFERENCES turno (id) ON DELETE SET NULL,
    CONSTRAINT fk_notificacion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT chk_notificacion_tipo CHECK (tipo IN ('RECORDATORIO_TURNO', 'CONFIRMACION_TURNO', 'CANCELACION_TURNO', 'REPROGRAMACION_TURNO', 'BAJA_TURNO')),
    CONSTRAINT chk_notificacion_canal CHECK (canal IN ('WHATSAPP', 'EMAIL', 'SMS')),
    CONSTRAINT chk_notificacion_estado CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'CANCELADA'))
);

CREATE INDEX idx_notificacion_estado_fecha ON notificacion (estado, fecha_programada);
CREATE INDEX idx_notificacion_cliente_id ON notificacion (cliente_id);
CREATE INDEX idx_notificacion_turno_id ON notificacion (turno_id);

-- -----------------------------------------------------------------------------
-- 9. TABLA: auditoria_evento
-- -----------------------------------------------------------------------------
CREATE TABLE auditoria_evento (
    id BIGSERIAL PRIMARY KEY,
    modulo VARCHAR(50) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id VARCHAR(50) NOT NULL,
    operacion VARCHAR(30) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    detalles TEXT,
    ip_origen VARCHAR(45),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auditoria_operacion CHECK (operacion IN ('CREATE', 'UPDATE', 'DELETE', 'STATE_CHANGE', 'CANCEL', 'RESCHEDULE'))
);

CREATE INDEX idx_auditoria_entidad ON auditoria_evento (modulo, entidad, entidad_id);
CREATE INDEX idx_auditoria_fecha_hora ON auditoria_evento (fecha_hora);

