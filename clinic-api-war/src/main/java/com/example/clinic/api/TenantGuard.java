package com.example.clinic.api;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Every request carries a clinicId as a query param or request-body field, but a JWT is only ever
 * issued for one clinic. Without this check any authenticated caller could pass a different
 * clinic's id and read/write that clinic's data.
 */
public final class TenantGuard {
    private TenantGuard() {
    }

    public static void requireClinic(SecurityContext securityContext, Long requestedClinicId) {
        if (requestedClinicId == null) {
            throw new BadRequestException("clinicId is required");
        }
        if (!(securityContext instanceof TokenSecurityContext tokenContext)
                || !requestedClinicId.equals(tokenContext.getClinicId())) {
            throw new ForbiddenException("clinicId does not match the authenticated clinic");
        }
    }
}
