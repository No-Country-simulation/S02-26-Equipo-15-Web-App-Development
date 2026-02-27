-- DEV ONLY: reset de tablas TrackSure
-- Ejecutar solo en entorno local/de prueba.

DROP TABLE IF EXISTS integrations_log;
DROP TABLE IF EXISTS stripe_webhook_event;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS tracking_event;
DROP TABLE IF EXISTS tracking_session;
