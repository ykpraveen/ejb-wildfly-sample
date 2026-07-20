package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotNull;

public class ActivateUserRequest {
    @NotNull
    public Long clinicId;
}
