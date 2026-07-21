package com.example.clinic.api;

import com.example.clinic.security.JwtPrincipal;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

public class TokenSecurityContext implements SecurityContext {
    private final JwtPrincipal principal;
    private final boolean secure;

    public TokenSecurityContext(JwtPrincipal principal, boolean secure) {
        this.principal = principal;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    public Long getClinicId() {
        return principal.clinicId();
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.roles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }
}
