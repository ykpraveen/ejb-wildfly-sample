package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotNull;

public class StartBookingRequest {
    @NotNull
    public Long clinicId;
    @NotNull
    public Long customerId;
}
