package com.example.clinic.appointment;

import java.time.Instant;

public record AppointmentEvent(
        String eventType,
        Long appointmentId,
        Long clinicId,
        Long customerId,
        Long doctorId,
        String correlationId,
        String actor,
        Instant timestamp
) {
    public static AppointmentEvent of(String eventType, Appointment appointment, String correlationId, String actor) {
        return new AppointmentEvent(
                eventType,
                appointment.getId(),
                appointment.getClinicId(),
                appointment.getCustomerId(),
                appointment.getDoctorId(),
                correlationId,
                actor,
                Instant.now()
        );
    }
}
