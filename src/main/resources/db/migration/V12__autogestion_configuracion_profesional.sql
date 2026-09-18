-- Los nuevos intervalos usan la configuración general del profesional.
ALTER TABLE autogestion_intervalo ALTER COLUMN tipo_id DROP NOT NULL;
