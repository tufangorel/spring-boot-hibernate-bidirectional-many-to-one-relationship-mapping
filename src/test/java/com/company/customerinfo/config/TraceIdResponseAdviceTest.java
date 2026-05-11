package com.company.customerinfo.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceIdResponseAdviceTest {

    private static final String TRACE_HEX = "a1b2c3d4e5f678901234567890abcd12";

    @Mock
    private Tracer tracer;
    @Mock
    private Span span;
    @Mock
    private TraceContext traceContext;

    private TraceIdResponseAdvice advice;
    private MethodParameter returnTypeParameter;
    private ServletServerHttpResponse response;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        advice = new TraceIdResponseAdvice(tracer);
        Method handlerMethod = TraceIdResponseAdviceTest.class.getDeclaredMethod("handlerStub", String.class);
        returnTypeParameter = new MethodParameter(handlerMethod, -1);
        response = new ServletServerHttpResponse(new MockHttpServletResponse());
    }

    @SuppressWarnings("unused")
    private static String handlerStub(String ignored) {
        return "";
    }

    @Test
    void supportsReturnsTrue() {
        assertThat(advice.supports(returnTypeParameter, StringHttpMessageConverter.class)).isTrue();
    }

    @Test
    void beforeBodyWriteAddsXTraceIdWhenSpanHasTraceContext() {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(TRACE_HEX);

        Object body = advice.beforeBodyWrite(
                "{\"ok\":true}",
                returnTypeParameter,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                null,
                response);

        assertThat(body).isEqualTo("{\"ok\":true}");
        assertThat(response.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isEqualTo(TRACE_HEX);
    }

    @Test
    void beforeBodyWriteLeavesPreSetHeaderUnchanged() {
        response.getHeaders().set(TracingHttp.X_TRACE_ID, "pre-set");

        advice.beforeBodyWrite(
                "x",
                returnTypeParameter,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                null,
                response);

        assertThat(response.getHeaders().get(TracingHttp.X_TRACE_ID)).containsExactly("pre-set");
    }

    @Test
    void beforeBodyWriteSkipsWhenNoCurrentSpan() {
        when(tracer.currentSpan()).thenReturn(null);

        advice.beforeBodyWrite(
                "x",
                returnTypeParameter,
                MediaType.TEXT_PLAIN,
                StringHttpMessageConverter.class,
                null,
                response);

        assertThat(response.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isNull();
    }

    @Test
    void beforeBodyWriteSkipsWhenTraceContextIsNull() {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(null);

        advice.beforeBodyWrite(
                "x",
                returnTypeParameter,
                MediaType.TEXT_PLAIN,
                StringHttpMessageConverter.class,
                null,
                response);

        assertThat(response.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isNull();
    }

    @Test
    void beforeBodyWriteSkipsWhenTraceIdBlank() {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("");

        advice.beforeBodyWrite(
                "x",
                returnTypeParameter,
                MediaType.TEXT_PLAIN,
                StringHttpMessageConverter.class,
                null,
                response);

        assertThat(response.getHeaders().getFirst(TracingHttp.X_TRACE_ID)).isNull();
    }
}
