package com.company.customerinfo.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Resolves a stable per-request key used as the user identity for rate limiting.
 *
 * <p>The default implementation prefers an explicit {@code X-User-Id} header, then
 * the first hop of {@code X-Forwarded-For}, and finally the raw remote address.
 * The interface exists so callers can later swap in an auth-aware resolver
 * without touching the aspect or filter.
 */
public interface UserKeyResolver {

    String USER_ID_HEADER = "X-User-Id";
    String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    Optional<String> resolve(HttpServletRequest request);

    /**
     * Default header-then-IP-fallback implementation.
     */
    class DefaultUserKeyResolver implements UserKeyResolver {

        @Override
        public Optional<String> resolve(HttpServletRequest request) {
            if (request == null) {
                return Optional.empty();
            }
            String userId = request.getHeader(USER_ID_HEADER);
            if (StringUtils.hasText(userId)) {
                return Optional.of("user:" + userId.trim());
            }
            String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
            if (StringUtils.hasText(forwarded)) {
                String firstHop = forwarded.split(",", 2)[0].trim();
                if (StringUtils.hasText(firstHop)) {
                    return Optional.of("ip:" + firstHop);
                }
            }
            String remoteAddr = request.getRemoteAddr();
            if (StringUtils.hasText(remoteAddr)) {
                return Optional.of("ip:" + remoteAddr);
            }
            return Optional.empty();
        }
    }
}
