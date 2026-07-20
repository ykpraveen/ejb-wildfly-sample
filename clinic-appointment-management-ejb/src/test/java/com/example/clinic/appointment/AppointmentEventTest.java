package com.example.clinic.appointment;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentEventTest {

    @Test
    void ofFactoryMethodCreatesEvent() {
        Appointment appointment = createTestAppointment();
        Instant before = Instant.now();

        AppointmentEvent event = AppointmentEvent.of("APPOINTMENT_BOOKED", appointment, "corr-abc");

        assertEquals("APPOINTMENT_BOOKED", event.eventType());
        assertEquals(1L, event.appointmentId());
        assertEquals(10L, event.clinicId());
        assertEquals(20L, event.customerId());
        assertEquals(30L, event.doctorId());
        assertEquals("corr-abc", event.correlationId());
        assertNotNull(event.timestamp());
        assertFalse(event.timestamp().isBefore(before));
    }

    @Test
    void recordEqualityWorks() {
        Appointment appointment = createTestAppointment();
        Instant now = Instant.now();

        AppointmentEvent a = new AppointmentEvent("TYPE", 1L, 10L, 20L, 30L, "corr", now);
        AppointmentEvent b = new AppointmentEvent("TYPE", 1L, 10L, 20L, 30L, "corr", now);
        assertEquals(a, b);
    }

    @Test
    void differentCorrelationIdMakesEventsNotEqual() {
        Instant now = Instant.now();
        AppointmentEvent a = new AppointmentEvent("TYPE", 1L, 10L, 20L, 30L, "corr-1", now);
        AppointmentEvent b = new AppointmentEvent("TYPE", 1L, 10L, 20L, 30L, "corr-2", now);
        assertNotEquals(a, b);
    }

    private Appointment createTestAppointment() {
        Appointment appt = new Appointment();
        appt.setId(1L);
        appt.setClinicId(10L);
        appt.setCustomerId(20L);
        appt.setDoctorId(30L);
        appt.setScheduleId(40L);
        return appt;
    }
}
