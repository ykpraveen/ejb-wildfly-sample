package com.example.clinic.doctor;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
public class DoctorSeedData {
    @jakarta.inject.Inject
    private DoctorManagementService doctorManagementService;

    @PostConstruct
    public void seed() {
        try {
            doctorManagementService.createDoctor(
                    1L,
                    "Dr. Alice Johnson",
                    "alicejohnson",
                    "Cardiology",
                    true,
                    30
            );
            doctorManagementService.createDoctor(
                    1L,
                    "Dr. Bob Williams",
                    "bobwilliams",
                    "General Practice",
                    true,
                    20
            );
        } catch (Exception ignored) {
        }
    }
}
