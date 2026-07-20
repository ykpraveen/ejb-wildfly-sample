package com.example.clinic.api.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class UpdateDoctorRequest {
    @Size(min = 2, max = 120)
    public String fullName;
    @Size(max = 100)
    public String specialty;
    public boolean active;
    @Positive
    public Integer slotMinutes;
}
