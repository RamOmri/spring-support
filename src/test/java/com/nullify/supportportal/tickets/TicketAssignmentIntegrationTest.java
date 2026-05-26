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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TicketAssignmentIntegrationTest extends PortalIntegrationTestBase {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;

    private HttpEntity<Map<String, ?>> json(Map<String, ?> body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null) h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    private String adminLogin() throws Exception {
        ResponseEntity<String> r = http.exchange("/api/v1/auth/login", HttpMethod.POST,
                json(Map.of("email", "admin@portal.local", "password", "admin"), null),
                String.class);
        return mapper.readTree(r.getBody()).get("accessToken").asText();
    }

    private String register(String name) throws Exception {
        ResponseEntity<String> r = http.exchange("/api/v1/auth/register", HttpMethod.POST,
                json(Map.of("username", name, "email", name + "@ex.com", "password", "password"), null),
                String.class);
        return mapper.readTree(r.getBody()).get("accessToken").asText();
    }

    @Test
    void admin_can_assign_agent_to_ticket() throws Exception {
        String adminToken = adminLogin();
        String customerToken = register("assigncust");

        ResponseEntity<String> created = http.exchange("/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "needs an agent"), customerToken), String.class);
        long ticketId = mapper.readTree(created.getBody()).get("id").asLong();

        long adminUserId = -1;
        ResponseEntity<String> users = http.exchange("/api/v1/admin/users", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), String.class);
        for (JsonNode u : mapper.readTree(users.getBody())) {
            if ("admin".equals(u.get("username").asText())) {
                adminUserId = u.get("id").asLong();
                break;
            }
        }
        assertThat(adminUserId).isGreaterThan(0);

        ResponseEntity<String> assigned = http.exchange(
                "/api/v1/tickets/" + ticketId + "/assign", HttpMethod.POST,
                json(Map.of("agentId", adminUserId), adminToken), String.class);
        assertThat(assigned.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(assigned.getBody());
        assertThat(body.get("assignedAgentId").asLong()).isEqualTo(adminUserId);
        assertThat(body.get("assignedAgentUsername").asText()).isEqualTo("admin");
    }

    @Test
    void customer_cannot_assign_agent() throws Exception {
        String customerToken = register("cantassign");
        ResponseEntity<String> created = http.exchange("/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "no assign for me"), customerToken), String.class);
        long ticketId = mapper.readTree(created.getBody()).get("id").asLong();

        ResponseEntity<String> r = http.exchange(
                "/api/v1/tickets/" + ticketId + "/assign", HttpMethod.POST,
                json(Map.of("agentId", 1), customerToken), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void assign_rejects_non_agent_user() throws Exception {
        String adminToken = adminLogin();
        String customerToken = register("nonagent");

        ResponseEntity<String> created = http.exchange("/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "x"), customerToken), String.class);
        long ticketId = mapper.readTree(created.getBody()).get("id").asLong();

        long customerId = -1;
        ResponseEntity<String> users = http.exchange("/api/v1/admin/users", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), String.class);
        for (JsonNode u : mapper.readTree(users.getBody())) {
            if ("nonagent".equals(u.get("username").asText())) {
                customerId = u.get("id").asLong();
                break;
            }
        }
        assertThat(customerId).isGreaterThan(0);

        ResponseEntity<String> r = http.exchange(
                "/api/v1/tickets/" + ticketId + "/assign", HttpMethod.POST,
                json(Map.of("agentId", customerId), adminToken), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }
}
