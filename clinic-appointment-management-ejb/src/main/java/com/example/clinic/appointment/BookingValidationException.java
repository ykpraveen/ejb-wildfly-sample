package com.example.clinic.appointment;

public class BookingValidationException extends RuntimeException {

    private final String errorCode;

    public BookingValidationException(String message) {
        super(message);
        this.errorCode = "REQUEST_ERROR";
    }

    public BookingValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
