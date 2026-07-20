package com.example.clinic.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditEntryTest {

    @Test
    void ofFactoryMethodCreatesEntry() {
        Instant before = Instant.now();
        AuditEntry entry = AuditEntry.of("corr-1", 1L, "admin", "BOOK", "Appointment", 42L);
        Instant after = Instant.now();

        assertEquals("corr-1", entry.correlationId());
        assertEquals(1L, entry.clinicId());
        assertEquals("admin", entry.actor());
        assertEquals("BOOK", entry.action());
        assertEquals("Appointment", entry.entityType());
        assertEquals(42L, entry.entityId());
        assertNotNull(entry.timestamp());
        assertFalse(entry.timestamp().isBefore(before));
        assertFalse(entry.timestamp().isAfter(after));
    }

    @Test
    void constructorCreatesEntry() {
        Instant ts = Instant.parse("2025-01-01T00:00:00Z");
        AuditEntry entry = new AuditEntry("c1", 1L, "actor", "DEL", "User", 10L, ts);
        assertEquals(ts, entry.timestamp());
    }
}
