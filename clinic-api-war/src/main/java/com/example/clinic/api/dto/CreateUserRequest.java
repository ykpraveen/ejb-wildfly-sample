package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class CreateUserRequest {
    @NotNull
    public Long clinicId;
    @NotBlank
    @Size(min = 3, max = 50)
    public String username;
    @NotBlank
    @Size(min = 8, max = 100)
    public String password;
    @NotNull
    @Size(min = 1)
    public Set<String> roles;
}
