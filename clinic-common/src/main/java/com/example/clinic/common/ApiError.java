package com.example.clinic.common;

public record ApiError(String code, String message, String correlationId) {
}
