-- Remove legacy schema from previous backend version.
-- Current application model uses:
-- tracking_session, tracking_event, orders, stripe_webhook_event, integrations_log.

DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS landing_events CASCADE;
DROP TABLE IF EXISTS attributions CASCADE;
DROP TABLE IF EXISTS users CASCADE;
