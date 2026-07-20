package com.example.clinic.user;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.Set;

@Singleton
@Startup
public class UserSeedData {
    @jakarta.inject.Inject
    private UserManagementService userManagementService;

    @PostConstruct
    public void seed() {
        try {
            UserAccount admin = userManagementService.createUser(
                    1L,
                    "admin",
                    "Admin123",
                    Set.of("ADMIN", "USER")
            );
            userManagementService.activateUser(1L, admin.getId());
        } catch (Exception ignored) {
            // Seed should be idempotent and skip if user already exists.
        }
    }
}
