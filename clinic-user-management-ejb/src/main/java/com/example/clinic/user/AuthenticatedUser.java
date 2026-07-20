package com.example.clinic.user;

import java.util.Set;

public record AuthenticatedUser(Long userId, Long clinicId, String username, Set<String> roles) {
}
