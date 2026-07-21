package com.example.clinic.api;

import com.example.clinic.security.JwtPrincipal;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

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

        Long authenticatedClinicId = null;
        if (securityContext != null) {
            Principal principal = securityContext.getUserPrincipal();
            if (principal instanceof JwtPrincipal jwtPrincipal) {
                authenticatedClinicId = jwtPrincipal.clinicId();
            } else if (securityContext instanceof TokenSecurityContext tokenContext) {
                authenticatedClinicId = tokenContext.getClinicId();
            }
        }

        if (!requestedClinicId.equals(authenticatedClinicId)) {
            throw new ForbiddenException("clinicId does not match the authenticated clinic");
        }
    }
}
