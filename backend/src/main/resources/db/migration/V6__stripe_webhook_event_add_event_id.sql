ALTER TABLE stripe_webhook_event
    ADD COLUMN IF NOT EXISTS event_id UUID;

CREATE INDEX IF NOT EXISTS idx_stripe_webhook_event_event_id
    ON stripe_webhook_event(event_id);
