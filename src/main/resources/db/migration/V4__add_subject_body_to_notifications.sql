SET search_path TO tenant_template;
ALTER TABLE notifications ADD COLUMN subject VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE notifications ADD COLUMN body    TEXT         NOT NULL DEFAULT '';

SET search_path TO tenant_local;
ALTER TABLE notifications ADD COLUMN subject VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE notifications ADD COLUMN body    TEXT         NOT NULL DEFAULT '';
