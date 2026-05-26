package com.nullify.supportportal.search;

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

class SearchApiIntegrationTest extends PortalIntegrationTestBase {

    @Autowired
    TestRestTemplate http;

    @Autowired
    ObjectMapper mapper;

    private HttpEntity<Map<String, ?>> json(Map<String, ?> body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null) h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    private HttpEntity<Void> auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private String register(String name) throws Exception {
        ResponseEntity<String> r = http.exchange("/api/v1/auth/register", HttpMethod.POST,
                json(Map.of("username", name, "email", name + "@ex.com", "password", "password"), null),
                String.class);
        return mapper.readTree(r.getBody()).get("accessToken").asText();
    }

    private String adminLogin() throws Exception {
        ResponseEntity<String> r = http.exchange("/api/v1/auth/login", HttpMethod.POST,
                json(Map.of("email", "admin@portal.local", "password", "admin"), null),
                String.class);
        return mapper.readTree(r.getBody()).get("accessToken").asText();
    }

    private void createTicket(String token, String title, String description) {
        http.exchange("/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", title, "description", description), token),
                String.class);
    }

    @Test
    void search_returns_tickets_matching_title() throws Exception {
        String token = register("searchuser");
        createTicket(token, "kafka outage", "broker x");
        createTicket(token, "billing dispute", "card declined");
        ResponseEntity<String> r = http.exchange(
                "/api/v1/tickets/search?q=kafka", HttpMethod.GET,
                auth(token), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(r.getBody());
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);
        boolean found = false;
        for (JsonNode n : arr) {
            if (n.get("title").asText().toLowerCase().contains("kafka")) found = true;
        }
        assertThat(found).isTrue();
    }

    @Test
    void search_isolation_customer_does_not_see_others_tickets() throws Exception {
        String aliceToken = register("alicesearch");
        createTicket(aliceToken, "alice secret stuff", "");
        String bobToken = register("bobsearch");
        ResponseEntity<String> r = http.exchange(
                "/api/v1/tickets/search?q=alice secret", HttpMethod.GET,
                auth(bobToken), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(r.getBody());
        for (JsonNode n : arr) {
            assertThat(n.get("customerUsername").asText()).isEqualTo("bobsearch");
        }
    }

    @Test
    void admin_search_returns_all_matching() throws Exception {
        String customerToken = register("custforadmin");
        createTicket(customerToken, "admin will see me", "details");
        String adminToken = adminLogin();
        ResponseEntity<String> r = http.exchange(
                "/api/v1/tickets/search?q=admin will see", HttpMethod.GET,
                auth(adminToken), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(r.getBody());
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void save_list_delete_saved_search_roundtrip() throws Exception {
        String token = register("savedsearchuser");

        ResponseEntity<String> saved = http.exchange(
                "/api/v1/searches", HttpMethod.POST,
                json(Map.of("name", "high pri opens", "query", "status:open priority:high"), token),
                String.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        long id = mapper.readTree(saved.getBody()).get("id").asLong();

        ResponseEntity<String> list = http.exchange(
                "/api/v1/searches", HttpMethod.GET,
                auth(token), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(list.getBody());
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);

        ResponseEntity<String> deleted = http.exchange(
                "/api/v1/searches/" + id, HttpMethod.DELETE,
                auth(token), String.class);
        assertThat(deleted.getStatusCode().value()).isEqualTo(204);
    }
}
