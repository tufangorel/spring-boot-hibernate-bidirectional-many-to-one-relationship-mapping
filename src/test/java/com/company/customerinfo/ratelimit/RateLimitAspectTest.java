package com.company.customerinfo.ratelimit;

import com.company.customerinfo.exception.RateLimitExceededException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    private RateLimitProperties properties;
    private BucketRegistry registry;
    private RateLimitAspect aspect;
    private RateLimited rateLimited;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        properties = new RateLimitProperties();
        properties.setDefault(new RateLimitProperties.Profile(1, 1, 3600));
        registry = new BucketRegistry(100, Duration.ofHours(1));
        aspect = new RateLimitAspect(properties, registry);

        Method method = TestTarget.class.getDeclaredMethod("limited");
        rateLimited = method.getAnnotation(RateLimited.class);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void allowsRequestWhenBucketHasTokens() throws Throwable {
        UserContext.set("user:allowed");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.enforce(joinPoint, rateLimited);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    void rejectsRequestBeforeBusinessLogicWhenBucketIsEmpty() throws Throwable {
        UserContext.set("user:limited");
        when(joinPoint.proceed()).thenReturn("first");

        assertThat(aspect.enforce(joinPoint, rateLimited)).isEqualTo("first");
        reset(joinPoint);

        assertThatThrownBy(() -> aspect.enforce(joinPoint, rateLimited))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getBucketId()).isEqualTo("test.write"));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void usesSeparateBucketsForDifferentUsers() throws Throwable {
        UserContext.set("user:first");
        when(joinPoint.proceed()).thenReturn("first");
        assertThat(aspect.enforce(joinPoint, rateLimited)).isEqualTo("first");

        UserContext.set("user:second");
        reset(joinPoint);
        when(joinPoint.proceed()).thenReturn("second");

        assertThat(aspect.enforce(joinPoint, rateLimited)).isEqualTo("second");
        verify(joinPoint).proceed();
    }

    @Test
    void shortCircuitsBucketCheckWhenDisabled() throws Throwable {
        properties.setEnabled(false);
        UserContext.set("user:disabled");
        when(joinPoint.proceed()).thenReturn("first", "second");

        assertThat(aspect.enforce(joinPoint, rateLimited)).isEqualTo("first");
        assertThat(aspect.enforce(joinPoint, rateLimited)).isEqualTo("second");

        verify(joinPoint, Mockito.times(2)).proceed();
    }

    @Test
    void usesAnonymousKeyWhenUserContextIsEmpty() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.enforce(joinPoint, rateLimited)).isEqualTo("ok");

        assertThatThrownBy(() -> aspect.enforce(joinPoint, rateLimited))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getUserKey()).isEqualTo("anonymous"));
    }

    @Test
    void honoursAnnotationOverrideValues() throws Throwable {
        UserContext.set("user:overrides");
        Method method = TestTarget.class.getDeclaredMethod("explicitlyConfigured");
        RateLimited overrides = method.getAnnotation(RateLimited.class);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.enforce(joinPoint, overrides)).isEqualTo("ok");
        assertThat(aspect.enforce(joinPoint, overrides)).isEqualTo("ok");

        assertThatThrownBy(() -> aspect.enforce(joinPoint, overrides))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void usesNamedProfileMatchingKeySuffix() throws Throwable {
        properties.getProfiles().put("write", new RateLimitProperties.Profile(2, 2, 3600));
        UserContext.set("user:named-profile");
        Method method = TestTarget.class.getDeclaredMethod("namedProfileSuffix");
        RateLimited named = method.getAnnotation(RateLimited.class);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.enforce(joinPoint, named)).isEqualTo("ok");
        assertThat(aspect.enforce(joinPoint, named)).isEqualTo("ok");

        assertThatThrownBy(() -> aspect.enforce(joinPoint, named))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getBucketId()).isEqualTo("customer.write"));
    }

    @Test
    void derivesBucketIdFromMethodSignatureWhenKeyIsBlank() throws Throwable {
        UserContext.set("user:no-key");
        Method method = TestTarget.class.getDeclaredMethod("blankKey");
        RateLimited blank = method.getAnnotation(RateLimited.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getDeclaringType()).thenAnswer(invocation -> TestTarget.class);
        when(signature.getName()).thenReturn("blankKey");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.enforce(joinPoint, blank)).isEqualTo("ok");

        assertThatThrownBy(() -> aspect.enforce(joinPoint, blank))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getBucketId()).isEqualTo("TestTarget#blankKey"));
    }

    static class TestTarget {

        @RateLimited(key = "test.write")
        void limited() {
        }

        @RateLimited(key = "explicit", capacity = 2, refillTokens = 2, refillPeriodSeconds = 60)
        void explicitlyConfigured() {
        }

        @RateLimited(key = "customer.write")
        void namedProfileSuffix() {
        }

        @RateLimited
        void blankKey() {
        }
    }
}
