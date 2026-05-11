package com.company.customerinfo.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;

/**
 * Caffeine-backed registry of in-memory Bucket4j buckets keyed by
 * {@code userKey + "|" + bucketId}. Limit parameters are applied only when a
 * brand new bucket is built; existing buckets are reused to preserve their
 * token state across calls.
 */
public class BucketRegistry {

    private final Cache<String, Bucket> cache;

    public BucketRegistry(long maxSize, Duration entryTtl) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(entryTtl)
                .build();
    }

    public Bucket resolve(String userKey, String bucketId, long capacity, long refillTokens, Duration refillPeriod) {
        String compositeKey = userKey + "|" + bucketId;
        return cache.get(compositeKey, key -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(refillTokens, refillPeriod)
                        .build())
                .build());
    }
}
