package com.company.customerinfo.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates {@link UserContext} from the request before any controller or
 * service runs and clears it afterward. Registered with the highest
 * precedence in {@code RateLimitConfig} so it executes before any other
 * application filter. Order is {@code HIGHEST_PRECEDENCE + 10} so Micrometer's
 * HTTP observation / tracing filter runs first and establishes trace context.
 */
public class UserKeyFilter extends OncePerRequestFilter {

    private final UserKeyResolver resolver;

    public UserKeyFilter(UserKeyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            resolver.resolve(request).ifPresent(UserContext::set);
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
