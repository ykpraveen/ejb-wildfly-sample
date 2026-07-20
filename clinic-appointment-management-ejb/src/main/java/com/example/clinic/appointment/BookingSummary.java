package com.example.clinic.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingSummary(
        String sessionId,
        Long clinicId,
        Long customerId,
        Long doctorId,
        String doctorName,
        Long scheduleId,
        LocalDate scheduleDate,
        LocalTime scheduleStartTime,
        LocalTime scheduleEndTime,
        LocalTime selectedTime,
        String notes,
        String status
) {
}
