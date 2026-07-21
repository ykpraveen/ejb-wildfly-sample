CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clinic_id BIGINT NOT NULL,
    actor VARCHAR(80) NOT NULL,
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id BIGINT NOT NULL,
    details VARCHAR(500),
    correlation_id VARCHAR(36),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_log_entity ON audit_log (clinic_id, entity_type, entity_id);
CREATE INDEX idx_audit_log_created ON audit_log (clinic_id, created_at);
