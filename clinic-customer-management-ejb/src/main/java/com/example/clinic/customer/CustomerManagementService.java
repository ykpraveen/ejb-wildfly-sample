package com.example.clinic.customer;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@Stateless
public class CustomerManagementService {
    @PersistenceContext(unitName = "customerMgmtPU")
    private EntityManager em;

    @Transactional
    public Customer createCustomer(Long clinicId, String fullName, String username, String email, String phone) {
        if (clinicId == null || clinicId < 1) {
            throw new BadRequestException("clinicId is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("fullName is required");
        }
        if (username == null || username.isBlank()) {
            throw new BadRequestException("username is required");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("email is required");
        }

        String normalizedUsername = username.trim().toLowerCase();
        Long count = em.createQuery(
                        "SELECT COUNT(c) FROM Customer c WHERE c.clinicId = :clinicId AND c.username = :username",
                        Long.class)
                .setParameter("clinicId", clinicId)
                .setParameter("username", normalizedUsername)
                .getSingleResult();
        if (count > 0) {
            throw new BadRequestException("username already exists for clinic");
        }

        Long emailCount = em.createQuery(
                        "SELECT COUNT(c) FROM Customer c WHERE c.clinicId = :clinicId AND c.email = :email",
                        Long.class)
                .setParameter("clinicId", clinicId)
                .setParameter("email", email.trim().toLowerCase())
                .getSingleResult();
        if (emailCount > 0) {
            throw new BadRequestException("email already exists for clinic");
        }

        Customer customer = new Customer();
        customer.setClinicId(clinicId);
        customer.setFullName(fullName.trim());
        customer.setUsername(normalizedUsername);
        customer.setEmail(email.trim().toLowerCase());
        customer.setPhone(phone == null ? null : phone.trim());
        customer.setDeleted(false);
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        em.persist(customer);
        return customer;
    }

    public List<Customer> listCustomers(Long clinicId) {
        if (clinicId == null) {
            throw new BadRequestException("clinicId is required");
        }
        return em.createQuery(
                        "SELECT c FROM Customer c WHERE c.clinicId = :clinicId AND c.deleted = false ORDER BY c.id",
                        Customer.class)
                .setParameter("clinicId", clinicId)
                .getResultList();
    }

    public Customer findById(Long clinicId, Long customerId) {
        List<Customer> customers = em.createQuery(
                        "SELECT c FROM Customer c WHERE c.id = :id AND c.clinicId = :clinicId AND c.deleted = false",
                        Customer.class)
                .setParameter("id", customerId)
                .setParameter("clinicId", clinicId)
                .setMaxResults(1)
                .getResultList();
        if (customers.isEmpty()) {
            throw new NotFoundException("customer not found");
        }
        return customers.get(0);
    }

    public Customer findByUsername(Long clinicId, String username) {
        if (clinicId == null || username == null) {
            throw new BadRequestException("clinicId and username are required");
        }
        List<Customer> customers = em.createQuery(
                        "SELECT c FROM Customer c WHERE c.clinicId = :clinicId AND c.username = :username AND c.deleted = false",
                        Customer.class)
                .setParameter("clinicId", clinicId)
                .setParameter("username", username.trim().toLowerCase())
                .setMaxResults(1)
                .getResultList();
        if (customers.isEmpty()) {
            throw new NotFoundException("customer not found for username");
        }
        return customers.get(0);
    }

    @Transactional
    public Customer updateCustomer(Long clinicId, Long customerId, String fullName, String email, String phone) {
        Customer customer = findById(clinicId, customerId);
        if (fullName != null && !fullName.isBlank()) {
            customer.setFullName(fullName.trim());
        }
        if (email != null && !email.isBlank()) {
            String normalizedEmail = email.trim().toLowerCase();
            if (!normalizedEmail.equals(customer.getEmail())) {
                Long emailCount = em.createQuery(
                                "SELECT COUNT(c) FROM Customer c WHERE c.clinicId = :clinicId AND c.email = :email AND c.id != :id",
                                Long.class)
                        .setParameter("clinicId", clinicId)
                        .setParameter("email", normalizedEmail)
                        .setParameter("id", customerId)
                        .getSingleResult();
                if (emailCount > 0) {
                    throw new BadRequestException("email already exists for clinic");
                }
                customer.setEmail(normalizedEmail);
            }
        }
        if (phone != null) {
            customer.setPhone(phone.trim());
        }
        customer.setUpdatedAt(Instant.now());
        return customer;
    }

    @Transactional
    public void softDelete(Long clinicId, Long customerId) {
        Customer customer = findById(clinicId, customerId);
        customer.setDeleted(true);
        customer.setUpdatedAt(Instant.now());
    }
}
