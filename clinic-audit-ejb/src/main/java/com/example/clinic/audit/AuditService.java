package com.example.clinic.audit;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.BadRequestException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class AuditService {

    private static final int DEFAULT_LIMIT = 200;

    @PersistenceContext(unitName = "auditMgmtPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AuditLogEntry record(Long clinicId, String actor, String action, String entityType,
                                 Long entityId, String correlationId, String details) {
        if (clinicId == null || clinicId < 1) {
            throw new BadRequestException("clinicId is required");
        }
        if (actor == null || actor.isBlank()) {
            throw new BadRequestException("actor is required");
        }
        if (action == null || action.isBlank()) {
            throw new BadRequestException("action is required");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new BadRequestException("entityType is required");
        }
        if (entityId == null) {
            throw new BadRequestException("entityId is required");
        }

        AuditLogEntry entry = new AuditLogEntry();
        entry.setClinicId(clinicId);
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setCorrelationId(correlationId);
        entry.setCreatedAt(Instant.now());
        em.persist(entry);
        return entry;
    }

    public List<AuditLogEntry> list(Long clinicId, String entityType, Long entityId, String actor, Integer limit) {
        if (clinicId == null) {
            throw new BadRequestException("clinicId is required");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT a FROM AuditLogEntry a WHERE a.clinicId = :clinicId");
        if (entityType != null && !entityType.isBlank()) {
            jpql.append(" AND a.entityType = :entityType");
        }
        if (entityId != null) {
            jpql.append(" AND a.entityId = :entityId");
        }
        if (actor != null && !actor.isBlank()) {
            jpql.append(" AND a.actor = :actor");
        }
        jpql.append(" ORDER BY a.createdAt DESC");

        TypedQuery<AuditLogEntry> query = em.createQuery(jpql.toString(), AuditLogEntry.class)
                .setParameter("clinicId", clinicId);
        if (entityType != null && !entityType.isBlank()) {
            query.setParameter("entityType", entityType);
        }
        if (entityId != null) {
            query.setParameter("entityId", entityId);
        }
        if (actor != null && !actor.isBlank()) {
            query.setParameter("actor", actor);
        }

        int effectiveLimit = (limit == null || limit < 1 || limit > DEFAULT_LIMIT) ? DEFAULT_LIMIT : limit;
        return query.setMaxResults(effectiveLimit).getResultList();
    }
}
