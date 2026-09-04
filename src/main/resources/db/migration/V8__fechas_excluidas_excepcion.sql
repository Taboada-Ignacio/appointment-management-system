CREATE TABLE excepcion_agenda_fecha_excluida (
    excepcion_agenda_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    CONSTRAINT fk_fecha_excluida_excepcion FOREIGN KEY (excepcion_agenda_id) REFERENCES excepcion_agenda(id) ON DELETE CASCADE,
    CONSTRAINT uk_fecha_excluida UNIQUE (excepcion_agenda_id, fecha)
);
