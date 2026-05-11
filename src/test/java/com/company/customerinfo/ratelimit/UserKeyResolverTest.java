package com.company.customerinfo.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserKeyResolverTest {

    @Mock
    private HttpServletRequest request;

    private UserKeyResolver.DefaultUserKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new UserKeyResolver.DefaultUserKeyResolver();
    }

    @Test
    void returnsEmptyWhenRequestIsNull() {
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void prefersUserIdHeaderAndTrimsWhitespace() {
        when(request.getHeader(UserKeyResolver.USER_ID_HEADER)).thenReturn("  alice  ");

        Optional<String> key = resolver.resolve(request);

        assertThat(key).contains("user:alice");
    }

    @Test
    void fallsBackToFirstForwardedForHopWhenUserIdMissing() {
        when(request.getHeader(UserKeyResolver.USER_ID_HEADER)).thenReturn(null);
        when(request.getHeader(UserKeyResolver.FORWARDED_FOR_HEADER))
                .thenReturn("203.0.113.10, 198.51.100.1, 10.0.0.1");

        Optional<String> key = resolver.resolve(request);

        assertThat(key).contains("ip:203.0.113.10");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardedForIsBlank() {
        when(request.getHeader(UserKeyResolver.USER_ID_HEADER)).thenReturn("");
        when(request.getHeader(UserKeyResolver.FORWARDED_FOR_HEADER)).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("10.0.0.42");

        Optional<String> key = resolver.resolve(request);

        assertThat(key).contains("ip:10.0.0.42");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardedForIsMissing() {
        when(request.getHeader(UserKeyResolver.USER_ID_HEADER)).thenReturn(null);
        when(request.getHeader(UserKeyResolver.FORWARDED_FOR_HEADER)).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.5");

        Optional<String> key = resolver.resolve(request);

        assertThat(key).contains("ip:192.168.1.5");
    }

    @Test
    void returnsEmptyWhenForwardedForFirstHopIsBlank() {
        when(request.getHeader(UserKeyResolver.USER_ID_HEADER)).thenReturn(null);
        when(request.getHeader(UserKeyResolver.FORWARDED_FOR_HEADER)).thenReturn(" , 10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("172.16.0.10");

        Optional<String> key = resolver.resolve(request);

        assertThat(key).contains("ip:172.16.0.10");
    }

    @Test
    void returnsEmptyWhenAllSourcesAreMissing() {
        when(request.getHeader(UserKeyResolver.USER_ID_HEADER)).thenReturn(null);
        when(request.getHeader(UserKeyResolver.FORWARDED_FOR_HEADER)).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        assertThat(resolver.resolve(request)).isEmpty();
    }
}
