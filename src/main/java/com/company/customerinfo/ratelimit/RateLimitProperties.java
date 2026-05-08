package com.company.customerinfo.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private Cache cache = new Cache();

    /**
     * Default profile used when no annotation override and no named profile match.
     */
    private Profile defaults = new Profile(60, 60, 60);

    /**
     * Named profiles. The aspect derives the profile name from the suffix of
     * {@code RateLimited.key()} after the last {@code '.'}. For example
     * {@code "customer.write"} resolves to profile {@code write}.
     */
    private Map<String, Profile> profiles = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    /**
     * Bound to {@code app.rate-limit.default} via Spring Boot's relaxed binding.
     * The field is named {@code defaults} because {@code default} is a reserved word.
     */
    public Profile getDefault() {
        return defaults;
    }

    public void setDefault(Profile defaults) {
        this.defaults = defaults;
    }

    public Map<String, Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<String, Profile> profiles) {
        this.profiles = profiles;
    }

    public static class Cache {
        private long maxSize = 100_000L;

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }
    }

    public static class Profile {
        private long capacity;
        private long refillTokens;
        private long refillPeriodSeconds;

        public Profile() {
        }

        public Profile(long capacity, long refillTokens, long refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodSeconds = refillPeriodSeconds;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillPeriodSeconds() {
            return refillPeriodSeconds;
        }

        public void setRefillPeriodSeconds(long refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }
}
