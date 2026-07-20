package com.example.clinic.security;

import java.util.Set;

public record JwtPrincipal(String username, Long clinicId, Set<String> roles) {
}
