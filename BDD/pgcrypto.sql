-- =========================
-- Extensions (opcional)
-- =========================
-- Para UUIDs si querés generarlos en DB:
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================
-- 1) Sesiones / Attribution
-- =========================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT,
  full_name TEXT,
  country TEXT,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE attributions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id TEXT UNIQUE,
  utm_source TEXT,
  utm_medium TEXT,
  utm_campaign TEXT,
  utm_term TEXT,
  utm_content TEXT,
  gclid TEXT,
  fbclid TEXT,
  landing_path TEXT,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE landing_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  attribution_id UUID REFERENCES attributions(id),
  event_type TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  attribution_id UUID REFERENCES attributions(id),
  stripe_session_id TEXT UNIQUE,
  stripe_payment_intent TEXT,
  amount NUMERIC,
  currency TEXT,
  status TEXT,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE integrations_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  integration TEXT NOT NULL,
  reference_id TEXT,
  status TEXT,
  http_status INT,
  latency_ms INT,
  request_payload JSONB,
  response_payload JSONB,
  error_message TEXT,
  created_at TIMESTAMP DEFAULT now()
);

-- Indexes que ayudan MUCHO
CREATE INDEX idx_attributions_gclid ON attributions(gclid);
CREATE INDEX idx_attributions_fbclid ON attributions(fbclid);
CREATE INDEX idx_attributions_utm_campaign ON attributions(utm_campaign);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_integrations_integration ON integrations_log(integration);
