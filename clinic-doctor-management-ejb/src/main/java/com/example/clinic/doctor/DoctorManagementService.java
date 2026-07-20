package com.example.clinic.doctor;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@Stateless
public class DoctorManagementService {
    @PersistenceContext(unitName = "doctorMgmtPU")
    private EntityManager em;

    @Transactional
    public Doctor createDoctor(Long clinicId, String fullName, String username, String specialty, boolean active, int slotMinutes) {
        if (clinicId == null || clinicId < 1) {
            throw new BadRequestException("clinicId is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("fullName is required");
        }
        if (username == null || username.isBlank()) {
            throw new BadRequestException("username is required");
        }
        if (specialty == null || specialty.isBlank()) {
            throw new BadRequestException("specialty is required");
        }
        if (slotMinutes < 5 || slotMinutes > 240) {
            throw new BadRequestException("slotMinutes must be between 5 and 240");
        }

        String normalizedUsername = username.trim().toLowerCase();
        Long count = em.createQuery(
                        "SELECT COUNT(d) FROM Doctor d WHERE d.clinicId = :clinicId AND d.username = :username",
                        Long.class)
                .setParameter("clinicId", clinicId)
                .setParameter("username", normalizedUsername)
                .getSingleResult();
        if (count > 0) {
            throw new BadRequestException("username already exists for clinic");
        }

        Doctor doctor = new Doctor();
        doctor.setClinicId(clinicId);
        doctor.setFullName(fullName.trim());
        doctor.setUsername(normalizedUsername);
        doctor.setSpecialty(specialty.trim());
        doctor.setActive(active);
        doctor.setDeleted(false);
        doctor.setSlotMinutes(slotMinutes);
        doctor.setCreatedAt(Instant.now());
        doctor.setUpdatedAt(Instant.now());
        em.persist(doctor);
        return doctor;
    }

    public List<Doctor> listDoctors(Long clinicId) {
        if (clinicId == null) {
            throw new BadRequestException("clinicId is required");
        }
        return em.createQuery(
                        "SELECT d FROM Doctor d WHERE d.clinicId = :clinicId AND d.deleted = false ORDER BY d.id",
                        Doctor.class)
                .setParameter("clinicId", clinicId)
                .getResultList();
    }

    public Doctor findById(Long clinicId, Long doctorId) {
        List<Doctor> doctors = em.createQuery(
                        "SELECT d FROM Doctor d WHERE d.id = :id AND d.clinicId = :clinicId AND d.deleted = false",
                        Doctor.class)
                .setParameter("id", doctorId)
                .setParameter("clinicId", clinicId)
                .setMaxResults(1)
                .getResultList();
        if (doctors.isEmpty()) {
            throw new NotFoundException("doctor not found");
        }
        return doctors.get(0);
    }

    public Doctor findByUsername(Long clinicId, String username) {
        if (clinicId == null || username == null) {
            throw new BadRequestException("clinicId and username are required");
        }
        List<Doctor> doctors = em.createQuery(
                        "SELECT d FROM Doctor d WHERE d.clinicId = :clinicId AND d.username = :username AND d.deleted = false",
                        Doctor.class)
                .setParameter("clinicId", clinicId)
                .setParameter("username", username.trim().toLowerCase())
                .setMaxResults(1)
                .getResultList();
        if (doctors.isEmpty()) {
            throw new NotFoundException("doctor not found for username");
        }
        return doctors.get(0);
    }

    @Transactional
    public Doctor updateDoctor(Long clinicId, Long doctorId, String fullName, String specialty, boolean active, Integer slotMinutes) {
        Doctor doctor = findById(clinicId, doctorId);
        if (fullName != null && !fullName.isBlank()) {
            doctor.setFullName(fullName.trim());
        }
        if (specialty != null && !specialty.isBlank()) {
            doctor.setSpecialty(specialty.trim());
        }
        doctor.setActive(active);
        if (slotMinutes != null && slotMinutes >= 5 && slotMinutes <= 240) {
            doctor.setSlotMinutes(slotMinutes);
        }
        doctor.setUpdatedAt(Instant.now());
        return doctor;
    }

    @Transactional
    public void softDelete(Long clinicId, Long doctorId) {
        Doctor doctor = findById(clinicId, doctorId);
        doctor.setDeleted(true);
        doctor.setUpdatedAt(Instant.now());
    }
}
