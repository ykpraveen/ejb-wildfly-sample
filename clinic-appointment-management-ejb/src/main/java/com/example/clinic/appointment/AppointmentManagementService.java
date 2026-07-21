package com.example.clinic.appointment;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import com.example.clinic.common.CorrelationIdFilter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class AppointmentManagementService {
    private static final Logger LOG = Logger.getLogger(AppointmentManagementService.class.getName());

    @PersistenceContext(unitName = "appointmentMgmtPU")
    private EntityManager em;

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "java:jboss/exported/jms/queue/AppointmentEvents")
    private Queue appointmentEventsQueue;

    private final Jsonb jsonb = JsonbBuilder.create();

    private static final int MIN_LEAD_TIME_MINUTES = 60;
    private static final int CANCELLATION_CUTOFF_MINUTES = 120;
    private static final int MAX_RESCHEDULE_COUNT = 3;
    private static final int MAX_DAILY_BOOKINGS_PER_CUSTOMER = 5;

    @Transactional
    public Appointment bookAppointment(Long clinicId, Long customerId, Long doctorId, Long scheduleId,
                                        LocalDate appointmentDate, LocalTime appointmentTime, LocalTime scheduleWindowStart,
                                        LocalTime scheduleWindowEnd, Integer scheduleCapacity, String notes, String createdBy) {
        if (clinicId == null || customerId == null || doctorId == null || scheduleId == null || appointmentTime == null) {
            throw new BadRequestException("clinicId, customerId, doctorId, scheduleId, and appointmentTime are required");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new BadRequestException("createdBy is required");
        }

        if (scheduleWindowStart != null && scheduleWindowEnd != null) {
            if (appointmentTime.isBefore(scheduleWindowStart) || !appointmentTime.isBefore(scheduleWindowEnd)) {
                throw new BadRequestException("appointmentTime must be within schedule window (" + scheduleWindowStart + " - " + scheduleWindowEnd + ")");
            }
        }

        LocalDate appointmentDateActual = appointmentDate != null ? appointmentDate : LocalDate.now();
        Instant appointmentInstant = appointmentDateActual.atTime(appointmentTime).atZone(ZoneOffset.UTC).toInstant();
        if (ChronoUnit.MINUTES.between(Instant.now(), appointmentInstant) < MIN_LEAD_TIME_MINUTES) {
            throw new BadRequestException("appointment must be at least " + MIN_LEAD_TIME_MINUTES + " minutes in advance");
        }

        Long dailyCount = em.createQuery(
                        "SELECT COUNT(a) FROM Appointment a WHERE a.clinicId = :clinicId AND a.customerId = :customerId AND a.appointmentDate = :date AND a.status != :cancelledStatus AND a.deleted = false",
                        Long.class)
                .setParameter("clinicId", clinicId)
                .setParameter("customerId", customerId)
                .setParameter("date", appointmentDateActual)
                .setParameter("cancelledStatus", AppointmentStatus.CANCELLED)
                .getSingleResult();
        if (dailyCount >= MAX_DAILY_BOOKINGS_PER_CUSTOMER) {
            throw new BadRequestException("daily booking limit reached for customer");
        }

        Long duplicateCount = em.createQuery(
                        "SELECT COUNT(a) FROM Appointment a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId AND a.scheduleId = :scheduleId AND a.appointmentTime = :time AND a.status = :status AND a.deleted = false",
                        Long.class)
                .setParameter("clinicId", clinicId)
                .setParameter("doctorId", doctorId)
                .setParameter("scheduleId", scheduleId)
                .setParameter("time", appointmentTime)
                .setParameter("status", AppointmentStatus.BOOKED)
                .getSingleResult();
        if (duplicateCount > 0) {
            throw new BadRequestException("doctor is already booked for this time slot");
        }

        if (scheduleCapacity != null) {
            Long scheduleBookedCount = em.createQuery(
                            "SELECT COUNT(a) FROM Appointment a WHERE a.clinicId = :clinicId AND a.scheduleId = :scheduleId AND a.status = :status AND a.deleted = false",
                            Long.class)
                    .setParameter("clinicId", clinicId)
                    .setParameter("scheduleId", scheduleId)
                    .setParameter("status", AppointmentStatus.BOOKED)
                    .getSingleResult();
            if (scheduleBookedCount >= scheduleCapacity) {
                throw new BadRequestException("schedule has reached its booking capacity");
            }
        }

        Appointment appointment = new Appointment();
        appointment.setClinicId(clinicId);
        appointment.setCustomerId(customerId);
        appointment.setDoctorId(doctorId);
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(appointmentDateActual);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setNotes(notes == null ? null : notes.trim());
        appointment.setCreatedBy(createdBy);
        appointment.setDeleted(false);
        appointment.setRescheduleCount(0);
        appointment.setCreatedAt(Instant.now());
        appointment.setUpdatedAt(Instant.now());
        em.persist(appointment);
        sendEvent("APPOINTMENT_BOOKED", appointment);
        return appointment;
    }

    public List<Appointment> listAppointments(Long clinicId) {
        if (clinicId == null) {
            throw new BadRequestException("clinicId is required");
        }
        return em.createQuery(
                        "SELECT a FROM Appointment a WHERE a.clinicId = :clinicId AND a.deleted = false ORDER BY a.appointmentDate, a.appointmentTime",
                        Appointment.class)
                .setParameter("clinicId", clinicId)
                .getResultList();
    }

    public List<Appointment> listAppointmentsByCustomer(Long clinicId, Long customerId) {
        if (clinicId == null || customerId == null) {
            throw new BadRequestException("clinicId and customerId are required");
        }
        return em.createQuery(
                        "SELECT a FROM Appointment a WHERE a.clinicId = :clinicId AND a.customerId = :customerId AND a.deleted = false ORDER BY a.appointmentDate, a.appointmentTime",
                        Appointment.class)
                .setParameter("clinicId", clinicId)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public List<Appointment> listAppointmentsByDoctor(Long clinicId, Long doctorId) {
        if (clinicId == null || doctorId == null) {
            throw new BadRequestException("clinicId and doctorId are required");
        }
        return em.createQuery(
                        "SELECT a FROM Appointment a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId AND a.deleted = false ORDER BY a.appointmentDate, a.appointmentTime",
                        Appointment.class)
                .setParameter("clinicId", clinicId)
                .setParameter("doctorId", doctorId)
                .getResultList();
    }

    public Appointment findById(Long clinicId, Long appointmentId) {
        List<Appointment> appointments = em.createQuery(
                        "SELECT a FROM Appointment a WHERE a.id = :id AND a.clinicId = :clinicId AND a.deleted = false",
                        Appointment.class)
                .setParameter("id", appointmentId)
                .setParameter("clinicId", clinicId)
                .setMaxResults(1)
                .getResultList();
        if (appointments.isEmpty()) {
            throw new NotFoundException("appointment not found");
        }
        return appointments.get(0);
    }

    @Transactional
    public Appointment cancelAppointment(Long clinicId, Long appointmentId) {
        Appointment appointment = findById(clinicId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BadRequestException("only BOOKED appointments can be cancelled");
        }
        Instant appointmentInstant = appointment.getAppointmentDate().atTime(appointment.getAppointmentTime()).atZone(ZoneOffset.UTC).toInstant();
        if (ChronoUnit.MINUTES.between(Instant.now(), appointmentInstant) < CANCELLATION_CUTOFF_MINUTES) {
            throw new BadRequestException("cancellation cutoff window has passed");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setUpdatedAt(Instant.now());
        sendEvent("APPOINTMENT_CANCELLED", appointment);
        return appointment;
    }

    @Transactional
    public Appointment reschedule(Long clinicId, Long appointmentId, LocalTime newTime,
                                   LocalTime scheduleWindowStart, LocalTime scheduleWindowEnd) {
        if (newTime == null) {
            throw new BadRequestException("newTime is required");
        }
        Appointment appointment = findById(clinicId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BadRequestException("only BOOKED appointments can be rescheduled");
        }
        if (appointment.getRescheduleCount() >= MAX_RESCHEDULE_COUNT) {
            throw new BadRequestException("reschedule limit reached for this appointment");
        }

        if (scheduleWindowStart != null && scheduleWindowEnd != null) {
            if (newTime.isBefore(scheduleWindowStart) || !newTime.isBefore(scheduleWindowEnd)) {
                throw new BadRequestException("newTime must be within schedule window (" + scheduleWindowStart + " - " + scheduleWindowEnd + ")");
            }
        }

        Long duplicateCount = em.createQuery(
                        "SELECT COUNT(a) FROM Appointment a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId AND a.scheduleId = :scheduleId AND a.appointmentTime = :time AND a.status = :status AND a.deleted = false AND a.id != :id",
                        Long.class)
                .setParameter("clinicId", clinicId)
                .setParameter("doctorId", appointment.getDoctorId())
                .setParameter("scheduleId", appointment.getScheduleId())
                .setParameter("time", newTime)
                .setParameter("status", AppointmentStatus.BOOKED)
                .setParameter("id", appointmentId)
                .getSingleResult();
        if (duplicateCount > 0) {
            throw new BadRequestException("doctor is already booked for this time slot");
        }

        appointment.setAppointmentTime(newTime);
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
        appointment.setUpdatedAt(Instant.now());
        sendEvent("APPOINTMENT_RESCHEDULED", appointment);
        return appointment;
    }

    @Transactional
    public Appointment updateStatus(Long clinicId, Long appointmentId, AppointmentStatus status) {
        if (status == null) {
            throw new BadRequestException("status is required");
        }
        Appointment appointment = findById(clinicId, appointmentId);
        appointment.setStatus(status);
        appointment.setUpdatedAt(Instant.now());
        return appointment;
    }

    @Transactional
    public void softDelete(Long clinicId, Long appointmentId) {
        Appointment appointment = findById(clinicId, appointmentId);
        appointment.setDeleted(true);
        appointment.setUpdatedAt(Instant.now());
    }

    private void sendEvent(String eventType, Appointment appointment) {
        try {
            AppointmentEvent event = AppointmentEvent.of(eventType, appointment,
                    CorrelationIdFilter.current());
            String json = jsonb.toJson(event);
            jmsContext.createProducer().send(appointmentEventsQueue,
                    jmsContext.createTextMessage(json));
            LOG.info("[AppointmentManagementService] Sent event: " + eventType
                    + " for appointment " + appointment.getId());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[AppointmentManagementService] Failed to send event: " + eventType, e);
        }
    }
}
