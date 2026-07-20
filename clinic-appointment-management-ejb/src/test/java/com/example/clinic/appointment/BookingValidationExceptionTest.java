package com.example.clinic.appointment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingValidationExceptionTest {

    @Test
    void defaultMessageAndCode() {
        BookingValidationException ex = new BookingValidationException("field required");
        assertEquals("field required", ex.getMessage());
        assertEquals("REQUEST_ERROR", ex.getErrorCode());
    }

    @Test
    void customErrorCode() {
        BookingValidationException ex = new BookingValidationException("not found", "NOT_FOUND");
        assertEquals("not found", ex.getMessage());
        assertEquals("NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void isRuntimeException() {
        BookingValidationException ex = new BookingValidationException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
