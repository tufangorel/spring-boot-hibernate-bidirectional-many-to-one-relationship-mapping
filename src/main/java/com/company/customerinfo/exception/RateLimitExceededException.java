package com.company.customerinfo.exception;

/**
 * Thrown by the rate-limit aspect when a user has exhausted their token bucket.
 * Carries enough information for {@code GlobalExceptionHandler} to build a
 * {@code 429 Too Many Requests} response with a {@code Retry-After} header.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String userKey;
    private final String bucketId;
    private final long retryAfterNanos;

    public RateLimitExceededException(String userKey, String bucketId, long retryAfterNanos) {
        super("Rate limit exceeded for bucket '" + bucketId + "' (user=" + userKey + ")");
        this.userKey = userKey;
        this.bucketId = bucketId;
        this.retryAfterNanos = retryAfterNanos;
    }

    public String getUserKey() {
        return userKey;
    }

    public String getBucketId() {
        return bucketId;
    }

    public long getRetryAfterNanos() {
        return retryAfterNanos;
    }
}
