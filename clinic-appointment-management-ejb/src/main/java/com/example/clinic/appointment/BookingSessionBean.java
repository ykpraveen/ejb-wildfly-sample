package com.example.clinic.appointment;

import com.example.clinic.doctor.Doctor;
import com.example.clinic.doctor.DoctorManagementService;
import com.example.clinic.schedule.DoctorSchedule;
import com.example.clinic.schedule.ScheduleManagementService;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Stateful
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class BookingSessionBean {

    @EJB
    private DoctorManagementService doctorService;

    @EJB
    private ScheduleManagementService scheduleService;

    @EJB
    private AppointmentManagementService appointmentService;

    private Long clinicId;
    private Long customerId;
    private Long doctorId;
    private String doctorName;
    private Long scheduleId;
    private LocalDate scheduleDate;
    private LocalTime scheduleStartTime;
    private LocalTime scheduleEndTime;
    private LocalTime selectedTime;
    private String notes;
    private boolean completed;

    @Lock(LockType.WRITE)
    public void startBooking(Long clinicId, Long customerId) {
        if (clinicId == null || clinicId < 1) {
            throw new BookingValidationException("clinicId is required");
        }
        if (customerId == null || customerId < 1) {
            throw new BookingValidationException("customerId is required");
        }
        this.clinicId = clinicId;
        this.customerId = customerId;
        this.completed = false;
    }

    @Lock(LockType.WRITE)
    public void selectDoctor(Long doctorId) {
        ensureStarted();
        if (doctorId == null || doctorId < 1) {
            throw new BookingValidationException("doctorId is required");
        }
        Doctor doctor = doctorService.findById(clinicId, doctorId);
        this.doctorId = doctorId;
        this.doctorName = doctor.getFullName();
        this.scheduleId = null;
        this.scheduleDate = null;
        this.scheduleStartTime = null;
        this.scheduleEndTime = null;
        this.selectedTime = null;
    }

    @Lock(LockType.WRITE)
    public void selectSchedule(Long scheduleId) {
        ensureStarted();
        ensureDoctorSelected();
        if (scheduleId == null || scheduleId < 1) {
            throw new BookingValidationException("scheduleId is required");
        }
        DoctorSchedule schedule = scheduleService.findById(clinicId, scheduleId);
        if (!schedule.getDoctorId().equals(doctorId)) {
            throw new BookingValidationException("schedule does not belong to the selected doctor");
        }
        this.scheduleId = scheduleId;
        this.scheduleDate = schedule.getAvailableDate();
        this.scheduleStartTime = schedule.getStartTime();
        this.scheduleEndTime = schedule.getEndTime();
        this.selectedTime = null;
    }

    @Lock(LockType.WRITE)
    public void selectTime(LocalTime time) {
        ensureStarted();
        ensureScheduleSelected();
        if (time == null) {
            throw new BookingValidationException("appointmentTime is required");
        }
        if (time.isBefore(scheduleStartTime) || !time.isBefore(scheduleEndTime)) {
            throw new BookingValidationException("appointmentTime must be within schedule window ("
                    + scheduleStartTime + " - " + scheduleEndTime + ")");
        }
        this.selectedTime = time;
    }

    @Lock(LockType.WRITE)
    public void addNotes(String notes) {
        ensureStarted();
        this.notes = notes;
    }

    @Lock(LockType.READ)
    public BookingSummary getSummary(String sessionId) {
        ensureStarted();
        String status = completed ? "COMPLETED" : "IN_PROGRESS";
        return new BookingSummary(
                sessionId,
                clinicId,
                customerId,
                doctorId,
                doctorName,
                scheduleId,
                scheduleDate,
                scheduleStartTime,
                scheduleEndTime,
                selectedTime,
                notes,
                status
        );
    }

    @Lock(LockType.WRITE)
    @Remove
    public Appointment confirmBooking(String createdBy) {
        ensureStarted();
        ensureDoctorSelected();
        ensureScheduleSelected();
        if (selectedTime == null) {
            throw new BookingValidationException("appointmentTime is required");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new BookingValidationException("createdBy is required");
        }
        Appointment appointment = appointmentService.bookAppointment(
                clinicId, customerId, doctorId, scheduleId,
                scheduleDate, selectedTime, scheduleStartTime, scheduleEndTime,
                notes, createdBy
        );
        this.completed = true;
        return appointment;
    }

    @Lock(LockType.WRITE)
    @Remove
    public void cancel() {
        this.completed = true;
    }

    private void ensureStarted() {
        if (clinicId == null) {
            throw new BookingValidationException("booking session has not been started");
        }
    }

    private void ensureDoctorSelected() {
        if (doctorId == null) {
            throw new BookingValidationException("doctor has not been selected");
        }
    }

    private void ensureScheduleSelected() {
        if (scheduleId == null) {
            throw new BookingValidationException("schedule has not been selected");
        }
    }
}
