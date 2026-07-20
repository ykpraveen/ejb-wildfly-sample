package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotNull;

public class LoginRequest {
    @NotNull
    public Long clinicId;
    @NotNull
    public String username;
    @NotNull
    public String password;
}
