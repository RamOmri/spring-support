package com.nullify.supportportal.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nullify.supportportal.PortalIntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.net.HttpRetryException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiIntegrationTest extends PortalIntegrationTestBase {

    @Autowired
    TestRestTemplate http;

    @Autowired
    ObjectMapper mapper;

    private HttpEntity<Map<String, String>> json(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(body, headers);
    }

    @Test
    void admin_login_returns_jwt_and_admin_role() throws Exception {
        ResponseEntity<String> response = http.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                json(Map.of("email", "admin@portal.local", "password", "admin")),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(response.getBody());
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("role").asText()).isEqualTo("ADMIN");
        assertThat(body.get("email").asText()).isEqualTo("admin@portal.local");
    }

    @Test
    void login_with_wrong_password_returns_401() {
        // The JDK's HttpURLConnection (used under the hood by TestRestTemplate)
        // refuses to read the response when a POST receives a 401 with the
        // body already streamed — it raises HttpRetryException wrapped as
        // ResourceAccessException. Either outcome (the explicit 401 status or
        // the JDK-level rejection on a 401 it could not retry) proves the
        // server rejected the bad credentials.
        try {
            ResponseEntity<String> response = http.exchange(
                    "/api/v1/auth/login", HttpMethod.POST,
                    json(Map.of("email", "admin@portal.local", "password", "wrong")),
                    String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        } catch (ResourceAccessException ex) {
            assertThat(ex.getCause()).isInstanceOf(HttpRetryException.class);
        }
    }

    @Test
    void register_creates_customer_and_returns_jwt() throws Exception {
        ResponseEntity<String> response = http.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                json(Map.of(
                    "username", "newcustomer",
                    "email", "newcustomer@example.com",
                    "password", "secretpassword")),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(response.getBody());
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("role").asText()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_with_duplicate_email_returns_400() {
        http.exchange("/api/v1/auth/register", HttpMethod.POST,
                json(Map.of("username", "dupuser", "email", "dup@example.com", "password", "pwpwpw")),
                String.class);
        ResponseEntity<String> response = http.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                json(Map.of("username", "dupuser2", "email", "dup@example.com", "password", "pwpwpw")),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_with_invalid_email_returns_400() {
        ResponseEntity<String> response = http.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                json(Map.of("username", "xxx", "email", "not-an-email", "password", "abcdef")),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
