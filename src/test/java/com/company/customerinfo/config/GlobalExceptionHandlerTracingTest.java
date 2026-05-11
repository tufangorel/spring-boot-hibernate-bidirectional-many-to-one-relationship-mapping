package com.company.customerinfo.config;

import com.company.customerinfo.exception.RateLimitExceededException;
import com.company.customerinfo.exception.ResourceNotFoundException;
import com.company.customerinfo.exception.ServiceUnavailableException;
import com.company.customerinfo.model.Customer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTracingTest {

    private static final String TRACE_HEX = "feedfacefeedfacefeedfacefeedface";

    @Mock
    private Tracer tracer;
    @Mock
    private Span span;
    @Mock
    private TraceContext traceContext;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(tracer);
    }

    private void givenActiveTrace() {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(TRACE_HEX);
    }

    private void givenNoTrace() {
        when(tracer.currentSpan()).thenReturn(null);
    }

    @Test
    void validationErrorAddsTraceIdAndHeaderWhenSpanPresent() throws NoSuchMethodException {
        givenActiveTrace();
        Customer target = new Customer();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(target, "customer");
        errors.addError(new FieldError("customer", "age", null, false,
                new String[]{"Min.customer.age"}, null, "Age must be at least 18"));
        MethodParameter parameter = new MethodParameter(
                ValidationStub.class.getMethod("save", Customer.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, errors);

        ResponseEntity<Map<String, Object>> entity =
                handler.handleValidationException(ex, new ServletWebRequest(new org.springframework.mock.web.MockHttpServletRequest()));

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody()).containsEntry("traceId", TRACE_HEX);
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isEqualTo(TRACE_HEX);
    }

    @Test
    void validationErrorOmitsTraceWhenNoSpan() throws NoSuchMethodException {
        givenNoTrace();
        Customer target = new Customer();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(target, "customer");
        errors.addError(new FieldError("customer", "name", "blank"));
        MethodParameter parameter = new MethodParameter(
                ValidationStub.class.getMethod("save", Customer.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, errors);

        ResponseEntity<Map<String, Object>> entity =
                handler.handleValidationException(ex, new ServletWebRequest(new org.springframework.mock.web.MockHttpServletRequest()));

        assertThat(entity.getBody()).doesNotContainKey("traceId");
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isNull();
    }

    @Test
    void illegalArgumentAddsTraceWhenSpanPresent() {
        givenActiveTrace();
        ResponseEntity<Map<String, Object>> entity =
                handler.handleIllegalArgument(new IllegalArgumentException("bad"), null);
        assertThat(entity.getBody()).containsEntry("traceId", TRACE_HEX);
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isEqualTo(TRACE_HEX);
    }

    @Test
    void illegalArgumentOmitsTraceWhenNoSpan() {
        givenNoTrace();
        ResponseEntity<Map<String, Object>> entity =
                handler.handleIllegalArgument(new IllegalArgumentException("bad"), null);
        assertThat(entity.getBody()).doesNotContainKey("traceId");
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isNull();
    }

    @Test
    void resourceNotFoundAddsTraceWhenSpanPresent() {
        givenActiveTrace();
        ResponseEntity<Map<String, Object>> entity =
                handler.handleResourceNotFound(new ResourceNotFoundException("missing"), null);
        assertThat(entity.getBody()).containsEntry("traceId", TRACE_HEX);
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isEqualTo(TRACE_HEX);
    }

    @Test
    void serviceUnavailableAddsTraceWhenSpanPresent() {
        givenActiveTrace();
        ResponseEntity<Map<String, Object>> entity =
                handler.handleServiceUnavailable(new ServiceUnavailableException("down"), null);
        assertThat(entity.getBody()).containsEntry("traceId", TRACE_HEX);
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isEqualTo(TRACE_HEX);
    }

    @Test
    void rateLimitExceededAddsTraceRetryAfterAndHeader() {
        givenActiveTrace();
        long retryNanos = TimeUnit.SECONDS.toNanos(42);
        ResponseEntity<Map<String, Object>> entity = handler.handleRateLimitExceeded(
                new RateLimitExceededException("user:1", "customer.write", retryNanos), null);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(entity.getBody()).containsEntry("traceId", TRACE_HEX);
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isEqualTo(TRACE_HEX);
        assertThat(entity.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("42");
        assertThat(entity.getBody()).containsEntry("retryAfterSeconds", 42L);
    }

    @Test
    void unexpectedExceptionAddsTraceWhenSpanPresent() {
        givenActiveTrace();
        ResponseEntity<Map<String, Object>> entity =
                handler.handleGlobalException(new RuntimeException("boom"), null);
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(entity.getBody()).containsEntry("traceId", TRACE_HEX);
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isEqualTo(TRACE_HEX);
    }

    @Test
    void unexpectedExceptionOmitsTraceWhenSpanContextMissing() {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(null);
        ResponseEntity<Map<String, Object>> entity =
                handler.handleGlobalException(new RuntimeException("boom"), null);
        assertThat(entity.getBody()).doesNotContainKey("traceId");
        assertThat(entity.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isNull();
    }

    private static final class ValidationStub {
        @SuppressWarnings("unused")
        public void save(@Valid Customer customer) {
        }
    }
}
