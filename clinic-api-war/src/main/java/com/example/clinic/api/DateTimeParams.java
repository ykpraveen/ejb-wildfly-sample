package com.example.clinic.api;

import jakarta.ws.rs.BadRequestException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * LocalDate/LocalTime.parse throws an unchecked DateTimeParseException on bad input, which the
 * REST layer would otherwise let fall through to a generic 500 instead of a 400.
 */
public final class DateTimeParams {
    private DateTimeParams() {
    }

    public static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BadRequestException(fieldName + " must be a valid ISO date (yyyy-MM-dd): " + value);
        }
    }

    public static LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BadRequestException(fieldName + " must be a valid ISO time (HH:mm[:ss]): " + value);
        }
    }
}
