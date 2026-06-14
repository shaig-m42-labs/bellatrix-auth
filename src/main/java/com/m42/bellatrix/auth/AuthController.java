package com.m42.bellatrix.auth;

import com.m42.bellatrix.auth.AuthDtos.*;
import com.m42.bellatrix.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.register(request), correlation(servletRequest));
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request), correlation(servletRequest));
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.refresh(request), correlation(servletRequest));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @Valid @RequestBody LogoutRequest request,
                             HttpServletRequest servletRequest) {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        authService.logout(token, request);
        return ApiResponse.ok(null, correlation(servletRequest));
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.me(principal), correlation(servletRequest));
    }

    private String correlation(HttpServletRequest request) {
        return (String) request.getAttribute("correlationId");
    }
}
