CREATE TABLE webhooks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    target_url TEXT NOT NULL,
    event_type VARCHAR(64) NOT NULL DEFAULT 'ticket.status_changed',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhooks_enabled ON webhooks(enabled);

CREATE TABLE secrets (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(64) NOT NULL UNIQUE,
    value TEXT NOT NULL
);
