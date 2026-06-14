package com.m42.bellatrix.common;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, ApiError error, String correlationId, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data, String correlationId) {
        return new ApiResponse<>(true, data, null, correlationId, Instant.now());
    }

    public static <T> ApiResponse<T> failed(String code, String message, String correlationId) {
        return new ApiResponse<>(false, null, new ApiError(code, message), correlationId, Instant.now());
    }

    public record ApiError(String code, String message) {
    }
}
