package com.company.customerinfo.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public UserKeyResolver userKeyResolver() {
        return new UserKeyResolver.DefaultUserKeyResolver();
    }

    @Bean
    public BucketRegistry bucketRegistry(RateLimitProperties properties) {
        long maxRefillSeconds = Math.max(properties.getDefault().getRefillPeriodSeconds(),
                properties.getProfiles().values().stream()
                        .mapToLong(RateLimitProperties.Profile::getRefillPeriodSeconds)
                        .max().orElse(properties.getDefault().getRefillPeriodSeconds()));
        Duration ttl = Duration.ofSeconds(Math.max(60, maxRefillSeconds * 2));
        return new BucketRegistry(properties.getCache().getMaxSize(), ttl);
    }

    @Bean
    public FilterRegistrationBean<UserKeyFilter> userKeyFilterRegistration(@Autowired UserKeyResolver resolver) {
        FilterRegistrationBean<UserKeyFilter> registration = new FilterRegistrationBean<>(new UserKeyFilter(resolver));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // after Micrometer HTTP observation (span open)
        registration.addUrlPatterns("/*");
        return registration;
    }
}
