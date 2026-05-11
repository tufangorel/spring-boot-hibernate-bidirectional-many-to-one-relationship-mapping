package com.company.customerinfo.config;

/**
 * Shared HTTP tracing header name for filters, interceptors, and exception responses.
 */
public final class TracingHttp {

    public static final String X_TRACE_ID = "X-Trace-Id";

    private TracingHttp() {
    }
}
