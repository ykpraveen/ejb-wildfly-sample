package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateDoctorRequest {
    @NotNull
    public Long clinicId;
    @NotBlank
    @Size(min = 2, max = 120)
    public String fullName;
    @NotBlank
    @Size(min = 3, max = 50)
    public String username;
    @NotBlank
    @Size(max = 100)
    public String specialty;
    public boolean active = true;
    @Positive
    public int slotMinutes = 30;
}
