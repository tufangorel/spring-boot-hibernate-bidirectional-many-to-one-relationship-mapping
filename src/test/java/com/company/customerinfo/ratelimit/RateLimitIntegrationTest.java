package com.company.customerinfo.ratelimit;

import com.company.customerinfo.CustomerInfoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CustomerInfoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.servlet.context-path=/customer-info",
                "spring.datasource.url=jdbc:h2:mem:rate_limit_it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.cache.type=none",
                "app.rate-limit.profiles.write.capacity=2",
                "app.rate-limit.profiles.write.refill-tokens=2",
                "app.rate-limit.profiles.write.refill-period-seconds=3600"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RateLimitIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setUp() {
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
    }

    @Test
    void returnsTooManyRequestsWhenSameUserExceedsWriteLimit() {
        assertThat(postCustomer("rate-user", "Name One").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postCustomer("rate-user", "Name Two").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> third = postCustomer("rate-user", "Name Three");

        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(third.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(third.getBody()).contains("Rate limit exceeded for bucket: customer.write");
    }

    @Test
    void tracksBucketsIndependentlyPerUserHeader() {
        assertThat(postCustomer("first-user", "First One").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postCustomer("first-user", "First Two").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postCustomer("first-user", "First Three").getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(postCustomer("second-user", "Second One").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void fallsBackToClientIpWhenUserHeaderIsMissing() {
        assertThat(postCustomerWithoutUserHeader("Ip One").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postCustomerWithoutUserHeader("Ip Two").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> third = postCustomerWithoutUserHeader("Ip Three");

        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(third.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    private ResponseEntity<String> postCustomer(String userId, String name) {
        HttpHeaders headers = jsonHeaders();
        headers.set(UserKeyResolver.USER_ID_HEADER, userId);
        return postCustomer(headers, name);
    }

    private ResponseEntity<String> postCustomerWithoutUserHeader(String name) {
        return postCustomer(jsonHeaders(), name);
    }

    private ResponseEntity<String> postCustomer(HttpHeaders headers, String name) {
        HttpEntity<String> request = new HttpEntity<>(
                "{\"name\":\"" + name + "\",\"age\":30}",
                headers);
        return restTemplate.exchange(
                "http://localhost:" + port + "/customer-info/customer/save",
                HttpMethod.POST,
                request,
                String.class);
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
