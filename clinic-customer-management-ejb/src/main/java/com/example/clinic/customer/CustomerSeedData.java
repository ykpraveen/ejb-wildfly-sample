package com.example.clinic.customer;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
public class CustomerSeedData {
    @jakarta.inject.Inject
    private CustomerManagementService customerManagementService;

    @PostConstruct
    public void seed() {
        try {
            customerManagementService.createCustomer(
                    1L,
                    "John Doe",
                    "johndoe",
                    "johndoe@clinic.com",
                    "+1-555-0101"
            );
            customerManagementService.createCustomer(
                    1L,
                    "Jane Smith",
                    "janesmith",
                    "janesmith@clinic.com",
                    "+1-555-0102"
            );
        } catch (Exception ignored) {
        }
    }
}
