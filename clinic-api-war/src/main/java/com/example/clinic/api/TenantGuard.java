package com.example.clinic.api;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Every request carries a clinicId as a query param or request-body field, but a JWT is only ever
 * issued for one clinic. Without this check any authenticated caller could pass a different
 * clinic's id and read/write that clinic's data.
 *
 * Deliberately reads the clinicId back from the ContainerRequestContext property that
 * JwtAuthenticationFilter sets (JwtAuthenticationFilter.CLINIC_ID_PROPERTY), rather than casting
 * the injected SecurityContext/Principal — the property is a plain, unambiguous key/value set and
 * read on the same request-scoped object, with no dependency on how a given JAX-RS runtime chooses
 * to wrap/proxy SecurityContext for @Context injection.
 */
public final class TenantGuard {
    private TenantGuard() {
    }

    public static void requireClinic(ContainerRequestContext requestContext, Long requestedClinicId) {
        if (requestedClinicId == null) {
            throw new BadRequestException("clinicId is required");
        }

        Object authenticatedClinicId = requestContext.getProperty(JwtAuthenticationFilter.CLINIC_ID_PROPERTY);
        if (!requestedClinicId.equals(authenticatedClinicId)) {
            throw new ForbiddenException("clinicId does not match the authenticated clinic");
        }
    }
}
