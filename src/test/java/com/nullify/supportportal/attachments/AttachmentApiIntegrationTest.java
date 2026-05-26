package com.nullify.supportportal.attachments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nullify.supportportal.PortalIntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentApiIntegrationTest extends PortalIntegrationTestBase {

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

    private String registerCustomer(String user) throws Exception {
        ResponseEntity<String> r = http.exchange("/api/v1/auth/register", HttpMethod.POST,
                json(Map.of("username", user, "email", user + "@ex.com", "password", "password"), null),
                String.class);
        return mapper.readTree(r.getBody()).get("accessToken").asText();
    }

    private long createTicket(String token, String title) throws Exception {
        ResponseEntity<String> r = http.exchange("/api/v1/tickets", HttpMethod.POST,
                json(Map.of("title", title), token), String.class);
        return mapper.readTree(r.getBody()).get("id").asLong();
    }

    @Test
    void upload_and_list_and_download_roundtrip() throws Exception {
        String token = registerCustomer("upcust");
        long ticketId = createTicket(token, "broken thing");

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        uploadHeaders.setBearerAuth(token);

        ByteArrayResource fileResource = new ByteArrayResource("hello, world\n".getBytes()) {
            @Override
            public String getFilename() { return "note.txt"; }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileResource);

        ResponseEntity<String> uploaded = http.exchange(
                "/api/v1/tickets/" + ticketId + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(form, uploadHeaders),
                String.class);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(uploaded.getBody());
        long attachmentId = body.get("id").asLong();
        assertThat(body.get("filename").asText()).isEqualTo("note.txt");
        assertThat(body.get("sizeBytes").asLong()).isEqualTo(13L);

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);

        ResponseEntity<String> list = http.exchange(
                "/api/v1/tickets/" + ticketId + "/attachments",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(list.getBody());
        assertThat(arr.size()).isEqualTo(1);

        ResponseEntity<byte[]> downloaded = http.exchange(
                "/api/v1/attachments/" + attachmentId + "/download",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                byte[].class);
        assertThat(downloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(downloaded.getBody())).isEqualTo("hello, world\n");
    }

    @Test
    void other_customer_cannot_download_attachment() throws Exception {
        String aliceToken = registerCustomer("alicedown");
        long ticketId = createTicket(aliceToken, "secret stuff");

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        uploadHeaders.setBearerAuth(aliceToken);
        ByteArrayResource fileResource = new ByteArrayResource("secret".getBytes()) {
            @Override
            public String getFilename() { return "secret.txt"; }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileResource);
        ResponseEntity<String> uploaded = http.exchange(
                "/api/v1/tickets/" + ticketId + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(form, uploadHeaders),
                String.class);
        long attachmentId = mapper.readTree(uploaded.getBody()).get("id").asLong();

        String bobToken = registerCustomer("bobdown");
        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.setBearerAuth(bobToken);
        ResponseEntity<String> attempt = http.exchange(
                "/api/v1/attachments/" + attachmentId + "/download",
                HttpMethod.GET,
                new HttpEntity<>(bobHeaders),
                String.class);
        assertThat(attempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
