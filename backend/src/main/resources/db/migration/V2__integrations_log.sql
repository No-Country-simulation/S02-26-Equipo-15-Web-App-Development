CREATE TABLE IF NOT EXISTS integrations_log (
    id UUID PRIMARY KEY,
    integration VARCHAR(64) NOT NULL,
    reference_id VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    http_status INT,
    latency_ms INT,
    request_payload TEXT,
    response_payload TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_integrations_log_created_at ON integrations_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_integrations_log_integration ON integrations_log(integration);
CREATE INDEX IF NOT EXISTS idx_integrations_log_reference_id ON integrations_log(reference_id);
