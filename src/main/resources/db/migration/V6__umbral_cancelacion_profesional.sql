-- Umbral previo al inicio del turno que separa la eliminación anticipada
-- de la cancelación que conserva historial.
ALTER TABLE configuracion
    ADD COLUMN umbral_cancelacion_horas INTEGER NOT NULL DEFAULT 24,
    ADD CONSTRAINT chk_configuracion_umbral_cancelacion
        CHECK (umbral_cancelacion_horas >= 0);
