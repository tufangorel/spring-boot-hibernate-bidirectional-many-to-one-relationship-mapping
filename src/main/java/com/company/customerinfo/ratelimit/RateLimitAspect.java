package com.company.customerinfo.ratelimit;

import com.company.customerinfo.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * AOP gate that consumes a token from a per-user Bucket4j bucket before
 * delegating to the target service method. Configured at
 * {@link Ordered#HIGHEST_PRECEDENCE} so it sits outermost in the proxy chain
 * and runs before any other advice (transactions, caching, circuit breakers,
 * retries, validation).
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitAspect {

    private static final String ANONYMOUS_USER_KEY = "anonymous";

    private final RateLimitProperties properties;
    private final BucketRegistry registry;

    public RateLimitAspect(RateLimitProperties properties, BucketRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @Around("@annotation(com.company.customerinfo.ratelimit.RateLimited)")
    public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
        return enforce(joinPoint, resolveRateLimitedAnnotation(joinPoint));
    }

    Object enforce(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        String bucketId = resolveBucketId(joinPoint, rateLimited);
        String userKey = UserContext.get().orElse(ANONYMOUS_USER_KEY);
        ResolvedLimit limit = resolveLimit(rateLimited);

        Bucket bucket = registry.resolve(userKey, bucketId,
                limit.capacity(), limit.refillTokens(), Duration.ofSeconds(limit.refillPeriodSeconds()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for user='{}' bucket='{}' retryAfterNanos={}",
                    userKey, bucketId, probe.getNanosToWaitForRefill());
            throw new RateLimitExceededException(userKey, bucketId, probe.getNanosToWaitForRefill());
        }
        log.debug("Rate limit ok for user='{}' bucket='{}' remainingTokens={}",
                userKey, bucketId, probe.getRemainingTokens());
        return joinPoint.proceed();
    }

    private RateLimited resolveRateLimitedAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimited annotation = method.getAnnotation(RateLimited.class);
        if (annotation != null) {
            return annotation;
        }
        Object target = joinPoint.getTarget();
        if (target == null) {
            throw new IllegalStateException("Unable to resolve @RateLimited annotation without a target object");
        }
        try {
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            RateLimited targetAnnotation = targetMethod.getAnnotation(RateLimited.class);
            if (targetAnnotation != null) {
                return targetAnnotation;
            }
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Unable to resolve target method for rate limiting", ex);
        }
        throw new IllegalStateException("Unable to resolve @RateLimited annotation for " + method);
    }

    private String resolveBucketId(ProceedingJoinPoint joinPoint, RateLimited rateLimited) {
        if (StringUtils.hasText(rateLimited.key())) {
            return rateLimited.key();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
    }

    private ResolvedLimit resolveLimit(RateLimited rateLimited) {
        RateLimitProperties.Profile profile = resolveProfile(rateLimited.key());
        long capacity = rateLimited.capacity() > 0 ? rateLimited.capacity() : profile.getCapacity();
        long refillTokens = rateLimited.refillTokens() > 0 ? rateLimited.refillTokens() : profile.getRefillTokens();
        long refillPeriodSeconds = rateLimited.refillPeriodSeconds() > 0
                ? rateLimited.refillPeriodSeconds()
                : profile.getRefillPeriodSeconds();
        return new ResolvedLimit(capacity, refillTokens, refillPeriodSeconds);
    }

    private RateLimitProperties.Profile resolveProfile(String key) {
        if (StringUtils.hasText(key)) {
            int dot = key.lastIndexOf('.');
            String suffix = dot >= 0 ? key.substring(dot + 1) : key;
            RateLimitProperties.Profile named = properties.getProfiles().get(suffix);
            if (named != null) {
                return named;
            }
        }
        return properties.getDefault();
    }

    private record ResolvedLimit(long capacity, long refillTokens, long refillPeriodSeconds) {
    }
}
