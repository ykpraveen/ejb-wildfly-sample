package com.example.clinic.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

/**
 * Every request carries a clinicId as a query param or request-body field, but a JWT is only ever
 * issued for one clinic. Without this check any authenticated caller could pass a different
 * clinic's id and read/write that clinic's data.
 *
 * Reads the clinicId back from an HttpServletRequest attribute that JwtAuthenticationFilter sets
 * (JwtAuthenticationFilter.CLINIC_ID_ATTRIBUTE). HttpServletRequest is safe to @Context-inject into
 * resource methods on a servlet container (unlike ContainerRequestContext, which per the JAX-RS spec
 * is only guaranteed injectable in filters/interceptors/exception mappers, not resource classes).
 */
public final class TenantGuard {
    private TenantGuard() {
    }

    public static void requireClinic(HttpServletRequest httpRequest, Long requestedClinicId) {
        if (requestedClinicId == null) {
            throw new BadRequestException("clinicId is required");
        }

        Object authenticatedClinicId = httpRequest.getAttribute(JwtAuthenticationFilter.CLINIC_ID_ATTRIBUTE);
        if (!requestedClinicId.equals(authenticatedClinicId)) {
            throw new ForbiddenException("clinicId does not match the authenticated clinic");
        }
    }
}
