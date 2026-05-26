package com.nullify.supportportal.admin;

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

class AdminApiIntegrationTest extends PortalIntegrationTestBase {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;

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

    @Test
    void list_users_requires_admin() throws Exception {
        String customer = register("listcust");
        ResponseEntity<String> denied = http.exchange("/api/v1/admin/users", HttpMethod.GET,
                auth(customer), String.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String admin = adminLogin();
        ResponseEntity<String> ok = http.exchange("/api/v1/admin/users", HttpMethod.GET,
                auth(admin), String.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(ok.getBody()).isArray()).isTrue();
    }

    @Test
    void admin_promotes_customer_to_agent() throws Exception {
        register("promoteme");
        String admin = adminLogin();
        ResponseEntity<String> usersResp = http.exchange("/api/v1/admin/users", HttpMethod.GET,
                auth(admin), String.class);
        long userId = -1;
        for (JsonNode u : mapper.readTree(usersResp.getBody())) {
            if ("promoteme".equals(u.get("username").asText())) {
                userId = u.get("id").asLong();
                break;
            }
        }
        assertThat(userId).isGreaterThan(0);

        ResponseEntity<String> updated = http.exchange(
                "/api/v1/admin/users/" + userId + "/role", HttpMethod.POST,
                json(Map.of("role", "AGENT"), admin), String.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(updated.getBody()).get("role").asText()).isEqualTo("AGENT");
    }

    @Test
    void webhook_crud() throws Exception {
        String admin = adminLogin();
        ResponseEntity<String> created = http.exchange("/api/v1/admin/webhooks", HttpMethod.POST,
                json(Map.of("name", "test-hook", "targetUrl", "https://example.com/hook", "eventType", "ticket.status_changed", "enabled", true), admin),
                String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long id = mapper.readTree(created.getBody()).get("id").asLong();

        ResponseEntity<String> list = http.exchange("/api/v1/admin/webhooks", HttpMethod.GET,
                auth(admin), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(list.getBody());
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);

        ResponseEntity<String> deleted = http.exchange("/api/v1/admin/webhooks/" + id, HttpMethod.DELETE,
                auth(admin), String.class);
        assertThat(deleted.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void webhook_rejects_invalid_url() throws Exception {
        String admin = adminLogin();
        ResponseEntity<String> r = http.exchange("/api/v1/admin/webhooks", HttpMethod.POST,
                json(Map.of("name", "bad", "targetUrl", "ftp://nope/", "eventType", "ticket.status_changed"), admin),
                String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void email_inbound_creates_ticket_for_known_user() throws Exception {
        register("emailuser");
        String admin = adminLogin();
        String raw = """
                From: emailuser@ex.com
                Subject: my widget is broken
                Content-Type: text/plain

                Please help. The widget makes a clicking sound.
                """;
        ResponseEntity<String> r = http.exchange("/api/v1/email/inbound", HttpMethod.POST,
                json(Map.of("rawMessage", raw), admin), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(r.getBody());
        assertThat(body.get("title").asText()).isEqualTo("my widget is broken");
        assertThat(body.get("customerUsername").asText()).isEqualTo("emailuser");
    }

    @Test
    void email_inbound_rejects_unknown_sender() throws Exception {
        String admin = adminLogin();
        String raw = """
                From: stranger@ex.com
                Subject: hello

                hi
                """;
        ResponseEntity<String> r = http.exchange("/api/v1/email/inbound", HttpMethod.POST,
                json(Map.of("rawMessage", raw), admin), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void bulk_xml_import_creates_tickets() throws Exception {
        register("bulkuser");
        String admin = adminLogin();
        String xml = """
                <tickets>
                  <ticket>
                    <title>bulk one</title>
                    <description>first</description>
                    <customerEmail>bulkuser@ex.com</customerEmail>
                  </ticket>
                  <ticket>
                    <title>bulk two</title>
                    <description>second</description>
                    <customerEmail>bulkuser@ex.com</customerEmail>
                  </ticket>
                </tickets>
                """;
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_XML);
        h.setBearerAuth(admin);
        ResponseEntity<String> r = http.exchange("/api/v1/admin/import/xml", HttpMethod.POST,
                new HttpEntity<>(xml, h), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode ids = mapper.readTree(r.getBody()).get("createdIds");
        assertThat(ids.isArray()).isTrue();
        assertThat(ids.size()).isEqualTo(2);
    }

    @Test
    void bulk_csv_import_creates_tickets() throws Exception {
        register("csvuser");
        String admin = adminLogin();
        String csv = """
                title,description,customerEmail
                first csv,desc one,csvuser@ex.com
                second csv,desc two,csvuser@ex.com
                """;
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("text/csv"));
        h.setBearerAuth(admin);
        ResponseEntity<String> r = http.exchange("/api/v1/admin/import/csv", HttpMethod.POST,
                new HttpEntity<>(csv, h), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode ids = mapper.readTree(r.getBody()).get("createdIds");
        assertThat(ids.size()).isEqualTo(2);
    }

    @Test
    void bulk_xml_import_rejects_doctype() throws Exception {
        String admin = adminLogin();
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <tickets>
                  <ticket><title>&xxe;</title><customerEmail>admin@portal.local</customerEmail></ticket>
                </tickets>
                """;
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_XML);
        h.setBearerAuth(admin);
        ResponseEntity<String> r = http.exchange("/api/v1/admin/import/xml", HttpMethod.POST,
                new HttpEntity<>(xml, h), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
