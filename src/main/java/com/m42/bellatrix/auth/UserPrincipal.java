package com.m42.bellatrix.auth;

import java.util.UUID;

public record UserPrincipal(UUID id, String email, String role) {
}
