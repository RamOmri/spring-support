package com.nullify.supportportal.auth;

import com.nullify.supportportal.PortalIntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthWebIntegrationTest extends PortalIntegrationTestBase {

    @Autowired
    TestRestTemplate http;

    @Test
    void login_page_is_anonymous_accessible() {
        ResponseEntity<String> response = http.getForEntity("/login", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Sign in");
    }

    @Test
    void register_page_is_anonymous_accessible() {
        ResponseEntity<String> response = http.getForEntity("/register", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Create an account");
    }

    @Test
    void actuator_health_is_anonymous_accessible() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
