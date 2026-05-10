CREATE SCHEMA IF NOT EXISTS tenant_template;
SET search_path TO tenant_template;

CREATE TABLE notifications (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    type             VARCHAR(10)   NOT NULL CHECK (type IN ('EMAIL', 'SMS')),
    recipient_email  VARCHAR(255),
    recipient_phone  VARCHAR(50),
    recipient_name   VARCHAR(255),
    template_variables JSONB,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                   CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
