package com.example.clinic.user;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless
public class UserManagementService {
    @PersistenceContext(unitName = "userMgmtPU")
    private EntityManager em;

    @Transactional
    public UserAccount createUser(Long clinicId, String username, String rawPassword, Set<String> requestedRoles) {
        if (clinicId == null || clinicId < 1) {
            throw new BadRequestException("clinicId is required");
        }
        if (username == null || username.isBlank()) {
            throw new BadRequestException("username is required");
        }
        if (!PasswordPolicy.isValid(rawPassword)) {
            throw new BadRequestException("password must be at least 8 chars and alphanumeric");
        }

        String normalizedUsername = username.trim().toLowerCase();
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM UserAccount u WHERE u.clinicId = :clinicId AND u.username = :username",
                        Long.class)
                .setParameter("clinicId", clinicId)
                .setParameter("username", normalizedUsername)
                .getSingleResult();
        if (count > 0) {
            throw new BadRequestException("username already exists for clinic");
        }

        Set<RoleName> roles = normalizeRoles(requestedRoles);
        UserAccount user = new UserAccount();
        user.setClinicId(clinicId);
        user.setUsername(normalizedUsername);
        user.setPasswordHash(PasswordHasher.hash(rawPassword));
        user.setRoles(roles);
        user.setActive(false);
        user.setDeleted(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        em.persist(user);
        return user;
    }

    @Transactional
    public UserAccount activateUser(Long clinicId, Long userId) {
        UserAccount user = findClinicUser(clinicId, userId);
        user.setActive(true);
        user.setUpdatedAt(Instant.now());
        return user;
    }

    @Transactional
    public UserAccount deactivateUser(Long clinicId, Long userId) {
        UserAccount user = findClinicUser(clinicId, userId);
        user.setActive(false);
        user.setUpdatedAt(Instant.now());
        return user;
    }

    public AuthenticatedUser authenticate(Long clinicId, String username, String rawPassword) {
        if (clinicId == null || username == null || rawPassword == null) {
            throw new BadRequestException("clinicId, username and password are required");
        }
        String normalizedUsername = username.trim().toLowerCase();

        List<UserAccount> users = em.createQuery(
                        "SELECT u FROM UserAccount u WHERE u.clinicId = :clinicId AND u.username = :username AND u.deleted = false",
                        UserAccount.class)
                .setParameter("clinicId", clinicId)
                .setParameter("username", normalizedUsername)
                .setMaxResults(1)
                .getResultList();

        if (users.isEmpty()) {
            throw new NotAuthorizedException("Invalid credentials");
        }

        UserAccount user = users.get(0);
        if (!user.isActive()) {
            throw new NotAuthorizedException("User is not active");
        }
        if (!PasswordHasher.verify(rawPassword, user.getPasswordHash())) {
            throw new NotAuthorizedException("Invalid credentials");
        }

        Set<String> roles = user.getRoles().stream().map(RoleName::name).collect(Collectors.toSet());
        return new AuthenticatedUser(user.getId(), user.getClinicId(), user.getUsername(), roles);
    }

    public List<UserAccount> listUsers(Long clinicId) {
        if (clinicId == null) {
            throw new BadRequestException("clinicId is required");
        }
        return em.createQuery(
                        "SELECT u FROM UserAccount u WHERE u.clinicId = :clinicId AND u.deleted = false ORDER BY u.id",
                        UserAccount.class)
                .setParameter("clinicId", clinicId)
                .getResultList();
    }

    private UserAccount findClinicUser(Long clinicId, Long userId) {
        List<UserAccount> users = em.createQuery(
                        "SELECT u FROM UserAccount u WHERE u.id = :id AND u.clinicId = :clinicId AND u.deleted = false",
                        UserAccount.class)
                .setParameter("id", userId)
                .setParameter("clinicId", clinicId)
                .setMaxResults(1)
                .getResultList();
        if (users.isEmpty()) {
            throw new NotFoundException("user not found");
        }
        return users.get(0);
    }

    private Set<RoleName> normalizeRoles(Set<String> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            throw new BadRequestException("at least one role is required");
        }
        try {
            return requestedRoles.stream()
                    .map(role -> role == null ? "" : role.trim().toUpperCase())
                    .map(RoleName::valueOf)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("roles must be ADMIN, USER, CUSTOMER, or DOCTOR");
        }
    }
}
