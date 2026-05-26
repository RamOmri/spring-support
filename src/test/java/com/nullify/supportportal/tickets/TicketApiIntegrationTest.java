package com.nullify.supportportal.tickets;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TicketApiIntegrationTest extends PortalIntegrationTestBase {

    @Autowired
    TestRestTemplate http;

    @Autowired
    ObjectMapper mapper;

    private HttpEntity<Map<String, ?>> json(Map<String, ?> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authOnly(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private String login(String email, String password) throws Exception {
        ResponseEntity<String> response = http.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                json(Map.of("email", email, "password", password), null),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(response.getBody()).get("accessToken").asText();
    }

    private String registerCustomer(String username, String email, String password) throws Exception {
        ResponseEntity<String> response = http.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                json(Map.of("username", username, "email", email, "password", password), null),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(response.getBody()).get("accessToken").asText();
    }

    @Test
    void customer_creates_ticket_then_reads_own() throws Exception {
        String token = registerCustomer("alpha", "alpha@ex.com", "password");
        ResponseEntity<String> created = http.exchange(
                "/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "Login is broken", "description", "details", "priority", "HIGH"), token),
                String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(created.getBody());
        long ticketId = body.get("id").asLong();
        assertThat(body.get("status").asText()).isEqualTo("OPEN");
        assertThat(body.get("priority").asText()).isEqualTo("HIGH");
        assertThat(body.get("customerUsername").asText()).isEqualTo("alpha");

        ResponseEntity<String> got = http.exchange(
                "/api/v1/tickets/" + ticketId, HttpMethod.GET,
                authOnly(token), String.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(got.getBody()).get("title").asText()).isEqualTo("Login is broken");
    }

    @Test
    void customer_cannot_see_other_customer_ticket() throws Exception {
        String aliceToken = registerCustomer("alice", "alice@ex.com", "password");
        ResponseEntity<String> created = http.exchange(
                "/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "alice ticket", "description", "x"), aliceToken),
                String.class);
        long ticketId = mapper.readTree(created.getBody()).get("id").asLong();

        String bobToken = registerCustomer("bob", "bob@ex.com", "password");
        ResponseEntity<String> got = http.exchange(
                "/api/v1/tickets/" + ticketId, HttpMethod.GET,
                authOnly(bobToken), String.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_sees_all_tickets() throws Exception {
        String customerToken = registerCustomer("delta", "delta@ex.com", "password");
        http.exchange("/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "from customer"), customerToken), String.class);

        String adminToken = login("admin@portal.local", "admin");
        ResponseEntity<String> list = http.exchange(
                "/api/v1/tickets", HttpMethod.GET,
                authOnly(adminToken), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(list.getBody());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void customer_cannot_change_status() throws Exception {
        String token = registerCustomer("echo", "echo@ex.com", "password");
        ResponseEntity<String> created = http.exchange(
                "/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "echo ticket"), token), String.class);
        long id = mapper.readTree(created.getBody()).get("id").asLong();

        ResponseEntity<String> response = http.exchange(
                "/api/v1/tickets/" + id + "/status", HttpMethod.POST,
                json(Map.of("status", "RESOLVED"), token), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_can_change_status() throws Exception {
        String customerToken = registerCustomer("foxtrot", "foxtrot@ex.com", "password");
        ResponseEntity<String> created = http.exchange(
                "/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "foxtrot ticket"), customerToken), String.class);
        long id = mapper.readTree(created.getBody()).get("id").asLong();

        String adminToken = login("admin@portal.local", "admin");
        ResponseEntity<String> response = http.exchange(
                "/api/v1/tickets/" + id + "/status", HttpMethod.POST,
                json(Map.of("status", "RESOLVED"), adminToken), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(response.getBody()).get("status").asText()).isEqualTo("RESOLVED");
    }

    @Test
    void customer_can_comment_on_own_ticket() throws Exception {
        String token = registerCustomer("golf", "golf@ex.com", "password");
        ResponseEntity<String> created = http.exchange(
                "/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "golf ticket"), token), String.class);
        long id = mapper.readTree(created.getBody()).get("id").asLong();

        ResponseEntity<String> comment = http.exchange(
                "/api/v1/tickets/" + id + "/comments", HttpMethod.POST,
                json(Map.of("body", "any updates?"), token), String.class);
        assertThat(comment.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> list = http.exchange(
                "/api/v1/tickets/" + id + "/comments", HttpMethod.GET,
                authOnly(token), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(list.getBody());
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("body").asText()).isEqualTo("any updates?");
    }

    @Test
    void unauthenticated_request_is_rejected() {
        ResponseEntity<String> response = http.exchange(
                "/api/v1/tickets", HttpMethod.GET,
                authOnly(null), String.class);
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
