package com.example.clinic.schedule;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Stateless
public class ScheduleManagementService {
    @PersistenceContext(unitName = "scheduleMgmtPU")
    private EntityManager em;

    @Transactional
    public DoctorSchedule addSchedule(Long clinicId, Long doctorId, LocalDate availableDate,
                                       LocalTime startTime, LocalTime endTime, int capacity) {
        if (clinicId == null || clinicId < 1) {
            throw new BadRequestException("clinicId is required");
        }
        if (doctorId == null || doctorId < 1) {
            throw new BadRequestException("doctorId is required");
        }
        if (availableDate == null) {
            throw new BadRequestException("availableDate is required");
        }
        if (startTime == null || endTime == null) {
            throw new BadRequestException("startTime and endTime are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be after startTime");
        }
        if (capacity < 1) {
            throw new BadRequestException("capacity must be at least 1");
        }

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setClinicId(clinicId);
        schedule.setDoctorId(doctorId);
        schedule.setAvailableDate(availableDate);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setCapacity(capacity);
        schedule.setDeleted(false);
        schedule.setCreatedAt(Instant.now());
        schedule.setUpdatedAt(Instant.now());
        em.persist(schedule);
        return schedule;
    }

    public List<DoctorSchedule> listSchedules(Long clinicId, Long doctorId) {
        if (clinicId == null) {
            throw new BadRequestException("clinicId is required");
        }
        if (doctorId == null) {
            throw new BadRequestException("doctorId is required");
        }
        return em.createQuery(
                        "SELECT s FROM DoctorSchedule s WHERE s.clinicId = :clinicId AND s.doctorId = :doctorId AND s.deleted = false ORDER BY s.availableDate, s.startTime",
                        DoctorSchedule.class)
                .setParameter("clinicId", clinicId)
                .setParameter("doctorId", doctorId)
                .getResultList();
    }

    public List<DoctorSchedule> listSchedulesByDate(Long clinicId, Long doctorId, LocalDate date) {
        if (clinicId == null || doctorId == null || date == null) {
            throw new BadRequestException("clinicId, doctorId, and date are required");
        }
        return em.createQuery(
                        "SELECT s FROM DoctorSchedule s WHERE s.clinicId = :clinicId AND s.doctorId = :doctorId AND s.availableDate = :date AND s.deleted = false ORDER BY s.startTime",
                        DoctorSchedule.class)
                .setParameter("clinicId", clinicId)
                .setParameter("doctorId", doctorId)
                .setParameter("date", date)
                .getResultList();
    }

    public DoctorSchedule findById(Long clinicId, Long scheduleId) {
        List<DoctorSchedule> schedules = em.createQuery(
                        "SELECT s FROM DoctorSchedule s WHERE s.id = :id AND s.clinicId = :clinicId AND s.deleted = false",
                        DoctorSchedule.class)
                .setParameter("id", scheduleId)
                .setParameter("clinicId", clinicId)
                .setMaxResults(1)
                .getResultList();
        if (schedules.isEmpty()) {
            throw new NotFoundException("schedule not found");
        }
        return schedules.get(0);
    }

    @Transactional
    public DoctorSchedule updateSchedule(Long clinicId, Long scheduleId, LocalTime startTime,
                                          LocalTime endTime, Integer capacity) {
        DoctorSchedule schedule = findById(clinicId, scheduleId);
        if (startTime != null) {
            schedule.setStartTime(startTime);
        }
        if (endTime != null) {
            schedule.setEndTime(endTime);
        }
        if (capacity != null && capacity >= 1) {
            schedule.setCapacity(capacity);
        }
        if (schedule.getEndTime().isBefore(schedule.getStartTime()) || !schedule.getEndTime().isAfter(schedule.getStartTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }
        schedule.setUpdatedAt(Instant.now());
        return schedule;
    }

    @Transactional
    public void softDelete(Long clinicId, Long scheduleId) {
        DoctorSchedule schedule = findById(clinicId, scheduleId);
        schedule.setDeleted(true);
        schedule.setUpdatedAt(Instant.now());
    }
}
