package com.example.clinic.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorTest {

    @Test
    void recordFieldsAreAccessible() {
        ApiError error = new ApiError("REQUEST_ERROR", "Invalid input", "abc-123");
        assertEquals("REQUEST_ERROR", error.code());
        assertEquals("Invalid input", error.message());
        assertEquals("abc-123", error.correlationId());
    }

    @Test
    void equalsAndHashCodeWork() {
        ApiError a = new ApiError("CODE", "msg", "id");
        ApiError b = new ApiError("CODE", "msg", "id");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualWhenFieldsDiffer() {
        ApiError a = new ApiError("CODE", "msg1", "id");
        ApiError b = new ApiError("CODE", "msg2", "id");
        assertNotEquals(a, b);
    }
}
