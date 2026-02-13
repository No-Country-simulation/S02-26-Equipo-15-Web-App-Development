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

CREATE TABLE IF NOT EXISTS tracking_event (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES tracking_session(event_id),
    event_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    currency VARCHAR(16),
    value NUMERIC,
    payload_json TEXT
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    event_id UUID REFERENCES tracking_session(event_id),
    stripe_session_id VARCHAR(255) NOT NULL,
    payment_intent_id VARCHAR(255),
    amount NUMERIC NOT NULL,
    currency VARCHAR(16) NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS stripe_webhook_event (
    stripe_event_id VARCHAR(255) PRIMARY KEY,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    error TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_stripe_session_id ON orders(stripe_session_id);
CREATE INDEX IF NOT EXISTS idx_tracking_event_event_id ON tracking_event(event_id);
CREATE INDEX IF NOT EXISTS idx_tracking_event_created_at ON tracking_event(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tracking_event_type ON tracking_event(event_type);
CREATE INDEX IF NOT EXISTS idx_tracking_session_created_at ON tracking_session(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_event_id ON orders(event_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);
