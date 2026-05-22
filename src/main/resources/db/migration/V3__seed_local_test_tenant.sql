-- Creates a local test tenant and API key for development/Swagger testing.
-- Raw key: notify-engine-local-test-key
-- SHA-256:  6ac6de0bb82df7cbbc69358e9a65d46357812c244452b9240a9e3e87a8a96288

SET search_path TO system;

INSERT INTO tenants (id, name, schema_name, status)
VALUES ('00000000-0000-0000-0000-000000000001', 'Local Test Tenant', 'tenant_local', 'ACTIVE');

INSERT INTO api_keys (tenant_id, key_hash, key_prefix, active)
VALUES ('00000000-0000-0000-0000-000000000001',
        '6ac6de0bb82df7cbbc69358e9a65d46357812c244452b9240a9e3e87a8a96288',
        'notify-e',
        true);

-- Provision the tenant schema and notifications table
CREATE SCHEMA IF NOT EXISTS tenant_local;
SET search_path TO tenant_local;

CREATE TABLE notifications (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    type               VARCHAR(10)  NOT NULL CHECK (type IN ('EMAIL', 'SMS')),
    recipient_email    VARCHAR(255),
    recipient_phone    VARCHAR(50),
    recipient_name     VARCHAR(255),
    template_variables JSONB,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_status     ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
