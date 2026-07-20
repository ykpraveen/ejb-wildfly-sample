package com.example.clinic.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateCustomerRequest {
    @NotNull
    public Long clinicId;
    @NotBlank
    @Size(min = 2, max = 120)
    public String fullName;
    @NotBlank
    @Size(min = 3, max = 50)
    public String username;
    @Email
    @Size(max = 120)
    public String email;
    @Size(max = 30)
    public String phone;
}
