package com.company.customerinfo.config;

import com.company.customerinfo.CustomerInfoApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CustomerInfoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:tracing_it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TracingIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setUp() {
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
    }

    @Test
    void successfulRequestIncludesXTraceIdHeader() {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/customer/list"),
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isNotBlank();
    }

    @Test
    void validationErrorIncludesTraceIdInBodyAndHeader() throws Exception {
        HttpHeaders headers = jsonHeaders();
        HttpEntity<String> request = new HttpEntity<>("{\"name\":\"ab\",\"age\":10}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/customer/save"),
                HttpMethod.POST,
                request,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String xTraceId = response.getHeaders().getFirst(TracingHttp.X_TRACE_ID);
        assertThat(xTraceId).isNotBlank();
        assertThat(response.getBody()).contains("\"traceId\"");
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(root.path("traceId").asText()).isEqualToIgnoringCase(xTraceId);
    }

    @Test
    void w3cTraceparentIsHonoredForXTraceId() {
        String traceId32 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String traceparent = "00-" + traceId32 + "-bbbbbbbbbbbbbbbb-01";
        HttpHeaders headers = jsonHeaders();
        headers.add("traceparent", traceparent);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/customer/list"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(TracingHttp.X_TRACE_ID))
                .isEqualToIgnoringCase(traceId32);
    }

    @Test
    void createdResponseIncludesXTraceIdMatchingInboundTraceparent() {
        String traceId32 = "cccccccccccccccccccccccccccccccc";
        String traceparent = "00-" + traceId32 + "-dddddddddddddddd-01";
        HttpHeaders headers = jsonHeaders();
        headers.add("traceparent", traceparent);

        String body = "{\"name\":\"Tracing Created\",\"age\":30}";
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/customer/save"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst(TracingHttp.X_TRACE_ID))
                .isEqualToIgnoringCase(traceId32);
    }

    @Test
    void illegalArgumentHandlerAddsTraceIdMatchingHeader() throws Exception {
        HttpHeaders headers = jsonHeaders();
        headers.add("traceparent", "00-eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee-ffffffffffffffff-01");

        // OrderItemService.findById rejects id <= 0 without @CircuitBreaker (unlike customer delete).
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/orderitem/update/0"),
                HttpMethod.PUT,
                new HttpEntity<>("{\"quantity\":5}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String xTraceId = response.getHeaders().getFirst(TracingHttp.X_TRACE_ID);
        assertThat(xTraceId).isNotBlank();
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(root.path("traceId").asText()).isEqualToIgnoringCase(xTraceId);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + "/customer-info" + path;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static class NoOpResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return false;
        }
    }
}
