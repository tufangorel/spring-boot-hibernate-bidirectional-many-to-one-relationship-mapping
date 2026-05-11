package com.company.customerinfo.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as rate-limited per user.
 *
 * <p>Applied at the service layer, this is intercepted by {@code RateLimitAspect}
 * with {@code Ordered.HIGHEST_PRECEDENCE} so the token check runs before any
 * other advice (transactions, caching, circuit breakers, retries, validation).
 *
 * <p>Resolution rules for the bucket parameters when an attribute is left at
 * its negative sentinel ({@code -1}): the aspect first looks at the named
 * profile derived from {@link #key()} (e.g. {@code "customer.write"} maps to
 * profile {@code write}), then falls back to {@code app.rate-limit.default}
 * from {@code application.yml}. Any explicit value on the annotation wins.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Logical bucket id. Combined with the resolved user key to identify the
     * Bucket4j bucket. When empty, the aspect derives a default of
     * {@code <SimpleClassName>#<methodName>}.
     */
    String key() default "";

    /**
     * Maximum tokens the bucket holds. Negative means "use config default".
     */
    long capacity() default -1L;

    /**
     * Tokens added per refill period. Negative means "use config default".
     */
    long refillTokens() default -1L;

    /**
     * Refill period in seconds. Negative means "use config default".
     */
    long refillPeriodSeconds() default -1L;
}
