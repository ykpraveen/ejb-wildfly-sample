package com.example.clinic.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @Test
    void verifyRejectsMalformedToken() {
        assertThrows(Exception.class, () -> jwtService.verify("not.a.valid.jwt.at.all"));
    }

    @Test
    void verifyRejectsEmptyToken() {
        assertThrows(Exception.class, () -> jwtService.verify(""));
    }

    @Test
    void verifyRejectsTamperedSignature() {
        String[] fakeParts = {"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
                "eyJzdWIiOiJhZG1pbiIsImNsaW5pY0lkIjoxLCJyb2xlcyI6WyJBRE1JTiJdLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6OTk5OTk5OTk5OX0",
                "TAMPERED_SIGNATURE"};
        String token = String.join(".", fakeParts);
        assertThrows(Exception.class, () -> jwtService.verify(token));
    }
}
