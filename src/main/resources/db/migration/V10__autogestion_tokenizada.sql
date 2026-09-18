CREATE TABLE autogestion_enlace (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL REFERENCES profesional(id) ON DELETE CASCADE,
    huella VARCHAR(64) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_autogestion_enlace_activo ON autogestion_enlace(profesional_id) WHERE activo;

CREATE TABLE autogestion_sesion (
    huella VARCHAR(64) PRIMARY KEY,
    enlace_id BIGINT NOT NULL REFERENCES autogestion_enlace(id) ON DELETE CASCADE,
    vence TIMESTAMPTZ NOT NULL,
    cliente_id BIGINT REFERENCES cliente(id) ON DELETE SET NULL,
    dni VARCHAR(30),
    telefono VARCHAR(50)
);
CREATE TABLE autogestion_intervalo (
    huella VARCHAR(64) PRIMARY KEY,
    sesion VARCHAR(64) NOT NULL REFERENCES autogestion_sesion(huella) ON DELETE CASCADE,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    dia_id BIGINT NOT NULL REFERENCES dia_agenda(id) ON DELETE CASCADE,
    tipo_id BIGINT NOT NULL REFERENCES tipo_atencion(id) ON DELETE CASCADE,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL,
    vence TIMESTAMPTZ NOT NULL,
    turno_id BIGINT REFERENCES turno(id) ON DELETE CASCADE
);
CREATE TABLE autogestion_confirmacion (
    sesion VARCHAR(64) NOT NULL REFERENCES autogestion_sesion(huella) ON DELETE CASCADE,
    clave VARCHAR(100) NOT NULL,
    intervalo VARCHAR(64) NOT NULL REFERENCES autogestion_intervalo(huella) ON DELETE CASCADE,
    observaciones VARCHAR(1000) NOT NULL,
    turno_id BIGINT NOT NULL REFERENCES turno(id) ON DELETE CASCADE,
    estado VARCHAR(50) NOT NULL,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (sesion, clave)
);
CREATE TABLE autogestion_limite (
    huella VARCHAR(64) NOT NULL,
    ventana BIGINT NOT NULL,
    intentos INTEGER NOT NULL,
    PRIMARY KEY (huella, ventana)
);
CREATE INDEX ix_autogestion_limite_ventana ON autogestion_limite(ventana);
