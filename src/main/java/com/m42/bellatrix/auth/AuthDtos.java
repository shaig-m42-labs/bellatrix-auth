package com.m42.bellatrix.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(@Email @NotBlank String email, @Size(min = 8) String password, @NotBlank String displayName) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
    }

    public record UserResponse(UUID id, String email, String displayName, String role) {
    }
}
