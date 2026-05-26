package com.nullify.supportportal.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nullify.supportportal.PortalIntegrationTestBase;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WebhookDispatchIntegrationTest extends PortalIntegrationTestBase {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;

    private HttpServer captureServer;
    private final ConcurrentLinkedQueue<String> captured = new ConcurrentLinkedQueue<>();
    private int capturePort;

    @BeforeEach
    void startCaptureServer() throws Exception {
        captureServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        capturePort = captureServer.getAddress().getPort();
        captureServer.createContext("/hook", exchange -> {
            try (ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                exchange.getRequestBody().transferTo(buf);
                captured.add(buf.toString());
            }
            byte[] response = "ok".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        captureServer.start();
    }

    @AfterEach
    void stopCaptureServer() {
        if (captureServer != null) {
            captureServer.stop(0);
        }
        captured.clear();
    }

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
    void status_change_fires_outbound_webhook() throws Exception {
        String adminToken = adminLogin();
        String customerToken = register("hookcust");

        ResponseEntity<String> created = http.exchange("/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", "hook me"), customerToken), String.class);
        long ticketId = mapper.readTree(created.getBody()).get("id").asLong();

        String hookUrl = "http://127.0.0.1:" + capturePort + "/hook";
        ResponseEntity<String> hookResp = http.exchange("/api/v1/admin/webhooks", HttpMethod.POST,
                json(Map.of(
                        "name", "capture",
                        "targetUrl", hookUrl,
                        "eventType", "ticket.status_changed",
                        "enabled", true), adminToken),
                String.class);
        assertThat(hookResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> statusResp = http.exchange(
                "/api/v1/tickets/" + ticketId + "/status", HttpMethod.POST,
                json(Map.of("status", "RESOLVED"), adminToken), String.class);
        assertThat(statusResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(10, TimeUnit.SECONDS).until(() -> !captured.isEmpty());
        String body = captured.peek();
        assertThat(body).contains("ticket.status_changed");
        assertThat(body).contains("\"ticket_id\":" + ticketId);
        assertThat(body).contains("\"new_status\":\"RESOLVED\"");
    }
}
