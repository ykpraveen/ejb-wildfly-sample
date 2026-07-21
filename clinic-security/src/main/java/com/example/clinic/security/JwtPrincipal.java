package com.example.clinic.security;

import java.security.Principal;
import java.util.Set;

public record JwtPrincipal(String username, Long clinicId, Set<String> roles) implements Principal {
	@Override
	public String getName() {
		return username;
	}
}
