package com.company.customerinfo.ratelimit;

import java.util.Optional;

/**
 * Per-thread holder for the resolved user key (from {@code X-User-Id} header
 * or client IP). Populated by {@code UserKeyFilter} at the very start of the
 * request, consumed by {@code RateLimitAspect}, and cleared in a {@code finally}
 * block by the filter to prevent leaks across pooled threads.
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_KEY = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String userKey) {
        USER_KEY.set(userKey);
    }

    public static Optional<String> get() {
        return Optional.ofNullable(USER_KEY.get());
    }

    public static void clear() {
        USER_KEY.remove();
    }
}
