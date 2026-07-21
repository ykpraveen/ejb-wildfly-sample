package com.example.clinic.api;

import com.example.clinic.audit.AuditLogEntry;
import com.example.clinic.audit.AuditService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/audit")
@Produces(MediaType.APPLICATION_JSON)
public class AuditResource {
    @Inject
    private AuditService auditService;

    @GET
    @RolesAllowed("ADMIN")
    public List<Map<String, Object>> listAuditEntries(
            @QueryParam("clinicId") Long clinicId,
            @QueryParam("entityType") String entityType,
            @QueryParam("entityId") Long entityId,
            @QueryParam("actor") String actor,
            @QueryParam("limit") Integer limit,
            @Context ContainerRequestContext requestContext
    ) {
        TenantGuard.requireClinic(requestContext, clinicId);
        return auditService.list(clinicId, entityType, entityId, actor, limit).stream()
                .map(this::toPayload).toList();
    }

    private Map<String, Object> toPayload(AuditLogEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entry.getId());
        map.put("clinicId", entry.getClinicId());
        map.put("actor", entry.getActor());
        map.put("action", entry.getAction());
        map.put("entityType", entry.getEntityType());
        map.put("entityId", entry.getEntityId());
        map.put("details", entry.getDetails() != null ? entry.getDetails() : "");
        map.put("correlationId", entry.getCorrelationId());
        map.put("createdAt", entry.getCreatedAt().toString());
        return map;
    }
}
