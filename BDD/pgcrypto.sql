-- ==========================================================
-- BDD bootstrap script (esquema actual)
-- ==========================================================
-- Uso recomendado:
-- 1) entornos locales de prueba/manuales
-- 2) referencia rapida del modelo final
--
-- Fuente de verdad en entornos reales:
-- backend/src/main/resources/db/migration/V1..V7
--
-- Nota:
-- La app genera UUIDs desde backend. Se habilita pgcrypto para
-- inserciones manuales opcionales con gen_random_uuid().

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ==========================================================
-- 1) Tracking session
-- ==========================================================
CREATE TABLE IF NOT EXISTS tracking_session (
    event_id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    utm_source VARCHAR(255),
    utm_medium VARCHAR(255),
    utm_campaign VARCHAR(255),
    utm_term VARCHAR(255),
    utm_content VARCHAR(255),
    gclid VARCHAR(255),
    fbclid VARCHAR(255),
    landing_path VARCHAR(1024),
    user_agent VARCHAR(1024),
    ip_hash VARCHAR(64)
);

-- ==========================================================
-- 2) Tracking event
-- ==========================================================
CREATE TABLE IF NOT EXISTS tracking_event (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES tracking_session(event_id),
    event_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    currency VARCHAR(16),
    value NUMERIC,
    payload_json TEXT
);

-- ==========================================================
-- 3) Orders (estado final con business_status)
-- ==========================================================
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    event_id UUID REFERENCES tracking_session(event_id),
    stripe_session_id VARCHAR(255) NOT NULL,
    payment_intent_id VARCHAR(255),
    amount NUMERIC NOT NULL,
    currency VARCHAR(16) NOT NULL,
    status VARCHAR(64) NOT NULL,
    business_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL
);

-- ==========================================================
-- 4) Stripe webhook event (estado final con event_id)
-- ==========================================================
CREATE TABLE IF NOT EXISTS stripe_webhook_event (
    stripe_event_id VARCHAR(255) PRIMARY KEY,
    event_id UUID,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    error TEXT
);

-- ==========================================================
-- 5) Integrations log (jsonb)
-- ==========================================================
CREATE TABLE IF NOT EXISTS integrations_log (
    id UUID PRIMARY KEY,
    integration VARCHAR(64) NOT NULL,
    reference_id VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    http_status INT,
    latency_ms INT,
    request_payload JSONB,
    response_payload JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL
);

-- ==========================================================
-- 6) Indexes y constraints operativas
-- ==========================================================
CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_stripe_session_id
    ON orders(stripe_session_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_payment_intent_id
    ON orders(payment_intent_id)
    WHERE payment_intent_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tracking_event_event_id
    ON tracking_event(event_id);

CREATE INDEX IF NOT EXISTS idx_tracking_event_created_at
    ON tracking_event(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tracking_event_type
    ON tracking_event(event_type);

CREATE INDEX IF NOT EXISTS idx_tracking_session_created_at
    ON tracking_session(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_event_id
    ON orders(event_id);

CREATE INDEX IF NOT EXISTS idx_orders_created_at
    ON orders(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_business_status
    ON orders(business_status);

CREATE INDEX IF NOT EXISTS idx_stripe_webhook_event_event_id
    ON stripe_webhook_event(event_id);

CREATE INDEX IF NOT EXISTS idx_integrations_log_created_at
    ON integrations_log(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_integrations_log_integration
    ON integrations_log(integration);

CREATE INDEX IF NOT EXISTS idx_integrations_log_reference_id
    ON integrations_log(reference_id);

-- ==========================================================
-- 7) Limpieza opcional de tablas legadas (referencia V4)
-- ==========================================================
-- DROP TABLE IF EXISTS payments CASCADE;
-- DROP TABLE IF EXISTS landing_events CASCADE;
-- DROP TABLE IF EXISTS attributions CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;
