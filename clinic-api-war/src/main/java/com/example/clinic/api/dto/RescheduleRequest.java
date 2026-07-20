package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotBlank;

public class RescheduleRequest {
    @NotBlank
    public String newTime;
}
