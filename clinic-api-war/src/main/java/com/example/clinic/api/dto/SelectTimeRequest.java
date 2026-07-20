package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotBlank;

public class SelectTimeRequest {
    @NotBlank
    public String appointmentTime;
}
