package com.m42.bellatrix.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.m42.bellatrix.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTtlSeconds;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.jwt.secret:${JWT_SECRET:dev-secret-change-me-dev-secret-change-me}}") String secret,
                      @Value("${app.jwt.access-ttl-seconds:${JWT_ACCESS_TTL_SECONDS:900}}") long accessTtlSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTtlSeconds = accessTtlSeconds;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(accessTtlSeconds).getEpochSecond());
        payload.put("jti", UUID.randomUUID().toString());
        return sign(payload);
    }

    public UserPrincipal verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token");
            }
            String expected = hmac(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expected, parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            Map<String, Object> payload = objectMapper.readValue(base64Decode(parts[1]), new TypeReference<>() {});
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("Token expired");
            }
            return new UserPrincipal(UUID.fromString((String) payload.get("sub")), (String) payload.get("email"), (String) payload.get("role"));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    public long accessTtlSeconds() {
        return accessTtlSeconds;
    }

    public String jti(String token) {
        try {
            String[] parts = token.split("\\.");
            Map<String, Object> payload = objectMapper.readValue(base64Decode(parts[1]), new TypeReference<>() {});
            return (String) payload.get("jti");
        } catch (Exception ex) {
            return token;
        }
    }

    private String sign(Map<String, Object> payload) {
        try {
            String header = base64Encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String body = base64Encode(objectMapper.writeValueAsBytes(payload));
            return header + "." + body + "." + hmac(header + "." + body);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create token", ex);
        }
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return base64Encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64Decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
