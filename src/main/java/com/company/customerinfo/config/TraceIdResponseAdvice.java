package com.company.customerinfo.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Adds {@value TracingHttp#X_TRACE_ID} during response body serialization, when the
 * Micrometer {@link Tracer} still has the current span (after controller execution).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class TraceIdResponseAdvice implements ResponseBodyAdvice<Object> {

    private final Tracer tracer;

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (response.getHeaders().getFirst(TracingHttp.X_TRACE_ID) != null) {
            return body;
        }
        String traceId = currentTraceId();
        if (traceId != null) {
            response.getHeaders().add(TracingHttp.X_TRACE_ID, traceId);
        }
        return body;
    }

    @Nullable
    private String currentTraceId() {
        Span span = tracer.currentSpan();
        if (span == null || span.context() == null) {
            return null;
        }
        String id = span.context().traceId();
        return id != null && !id.isEmpty() ? id : null;
    }
}
