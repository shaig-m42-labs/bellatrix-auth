package com.m42.bellatrix.auth;

import com.m42.bellatrix.auth.AuthDtos.*;
import com.m42.bellatrix.user.Role;
import com.m42.bellatrix.user.User;
import com.m42.bellatrix.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final long refreshTtlSeconds;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklistService blacklist,
                       @Value("${app.jwt.refresh-ttl-seconds:${JWT_REFRESH_TTL_SECONDS:604800}}") long refreshTtlSeconds) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = users.save(new User(request.email().toLowerCase(), passwordEncoder.encode(request.password()), request.displayName(), Role.MEMBER));
        return tokensFor(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return tokensFor(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokens.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        token.revoke();
        return tokensFor(token.getUser());
    }

    @Transactional
    public void logout(String accessToken, LogoutRequest request) {
        refreshTokens.findByToken(request.refreshToken()).ifPresent(RefreshToken::revoke);
        if (accessToken != null && !accessToken.isBlank()) {
            blacklist.blacklist(jwtService.jti(accessToken), Duration.ofSeconds(jwtService.accessTtlSeconds()));
        }
    }

    public UserResponse me(UserPrincipal principal) {
        User user = users.findById(principal.id()).orElseThrow(() -> new BadCredentialsException("User not found"));
        return toUser(user);
    }

    private AuthResponse tokensFor(User user) {
        String access = jwtService.createAccessToken(user);
        String refresh = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        refreshTokens.save(new RefreshToken(refresh, user, Instant.now().plusSeconds(refreshTtlSeconds)));
        return new AuthResponse(access, refresh, jwtService.accessTtlSeconds(), toUser(user));
    }

    private UserResponse toUser(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name());
    }
}
