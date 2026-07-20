package com.example.clinic.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateCustomerRequest {
    @Size(min = 2, max = 120)
    public String fullName;
    @Email
    @Size(max = 120)
    public String email;
    @Size(max = 30)
    public String phone;
}
