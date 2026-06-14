package com.m42.bellatrix.health;

import com.m42.bellatrix.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    ApiResponse<Map<String, String>> health(HttpServletRequest request) {
        return ApiResponse.ok(Map.of("service", "bellatrix-auth", "status", "UP"), (String) request.getAttribute("correlationId"));
    }
}
