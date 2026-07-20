package com.example.clinic.common;

import java.time.Instant;

public record AuditEntry(
        String correlationId,
        Long clinicId,
        String actor,
        String action,
        String entityType,
        Long entityId,
        Instant timestamp
) {
    public static AuditEntry of(String correlationId, Long clinicId, String actor,
                                 String action, String entityType, Long entityId) {
        return new AuditEntry(correlationId, clinicId, actor, action, entityType, entityId, Instant.now());
    }
}
