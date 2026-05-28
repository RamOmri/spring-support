package com.nullify.supportportal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PortalSmokeTest {

    private static final String BASE = System.getProperty("portal.baseUrl", "http://localhost:8000");
    private static final String ADMIN_EMAIL = "admin@portal.local";
    private static final String ADMIN_PASSWORD = "admin";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpResponse<String> get(String path, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        return CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String body, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        return CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String adminToken() throws Exception {
        HttpResponse<String> resp = postJson(
                "/api/v1/auth/login",
                MAPPER.writeValueAsString(java.util.Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD)),
                null);
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode node = MAPPER.readTree(resp.body());
        return node.get("accessToken").asText();
    }

    @Test
    void health_endpoint_returns_200() throws Exception {
        HttpResponse<String> resp = get("/actuator/health", null);
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("UP");
    }

    @Test
    void admin_login_returns_jwt() throws Exception {
        String token = adminToken();
        assertThat(token).isNotBlank();
    }

    @Test
    void login_with_wrong_password_returns_401() throws Exception {
        HttpResponse<String> resp = postJson(
                "/api/v1/auth/login",
                MAPPER.writeValueAsString(java.util.Map.of("email", ADMIN_EMAIL, "password", "wrong")),
                null);
        assertThat(resp.statusCode()).isIn(401, 403);
    }

    @Test
    void list_tickets_without_auth_returns_401() throws Exception {
        HttpResponse<String> resp = get("/api/v1/tickets", null);
        assertThat(resp.statusCode()).isIn(401, 403);
    }

    @Test
    void list_tickets_with_admin_token_returns_200() throws Exception {
        HttpResponse<String> resp = get("/api/v1/tickets", adminToken());
        assertThat(resp.statusCode()).isEqualTo(200);
    }

    @Test
    void create_ticket_authenticated_returns_2xx() throws Exception {
        String token = adminToken();
        HttpResponse<String> resp = postJson(
                "/api/v1/tickets",
                MAPPER.writeValueAsString(java.util.Map.of(
                        "title", "Smoke ticket",
                        "description", "Created by PortalSmokeTest",
                        "priority", "NORMAL")),
                token);
        assertThat(resp.statusCode()).isBetween(200, 299);
    }

    @Test
    void create_ticket_unauthenticated_returns_401() throws Exception {
        HttpResponse<String> resp = postJson(
                "/api/v1/tickets",
                MAPPER.writeValueAsString(java.util.Map.of(
                        "title", "x",
                        "description", "y",
                        "priority", "NORMAL")),
                null);
        assertThat(resp.statusCode()).isIn(401, 403);
    }

    @Test
    void admin_users_requires_admin() throws Exception {
        HttpResponse<String> unauth = get("/api/v1/admin/users", null);
        assertThat(unauth.statusCode()).isIn(401, 403);
        HttpResponse<String> auth = get("/api/v1/admin/users", adminToken());
        assertThat(auth.statusCode()).isEqualTo(200);
    }

    @Test
    void web_login_page_anonymous_accessible() throws Exception {
        HttpResponse<String> resp = get("/login", null);
        assertThat(resp.statusCode()).isEqualTo(200);
    }

    @Test
    void web_register_page_anonymous_accessible() throws Exception {
        HttpResponse<String> resp = get("/register", null);
        assertThat(resp.statusCode()).isEqualTo(200);
    }
}
