package com.m42.bellatrix.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TokenBlacklistService blacklist;

    @Test
    void registerLoginMeRefreshLogoutAndBlacklistFlow() throws Exception {
        String registerBody = """
                {
                  "email": "demo@example.com",
                  "password": "ChangeMe123!",
                  "displayName": "Demo User"
                }
                """;

        JsonNode register = json(mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String accessToken = register.path("data").path("accessToken").asText();
        String refreshToken = register.path("data").path("refreshToken").asText();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        JsonNode login = json(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo@example.com",
                                  "password": "ChangeMe123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String loginAccessToken = login.path("data").path("accessToken").asText();
        String loginRefreshToken = login.path("data").path("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + loginAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loginRefreshToken + "\"}"))
                .andExpect(status().isOk());

        Mockito.when(blacklist.isBlacklisted(anyString())).thenReturn(true);
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + loginAccessToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().is4xxClientError());
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}
