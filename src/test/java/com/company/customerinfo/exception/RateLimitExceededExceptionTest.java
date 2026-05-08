package com.company.customerinfo.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceededExceptionTest {

    @Test
    void exposesAllConstructorArgumentsViaAccessors() {
        RateLimitExceededException ex = new RateLimitExceededException("user:42", "customer.write", 12_345L);

        assertThat(ex.getUserKey()).isEqualTo("user:42");
        assertThat(ex.getBucketId()).isEqualTo("customer.write");
        assertThat(ex.getRetryAfterNanos()).isEqualTo(12_345L);
        assertThat(ex.getMessage()).contains("customer.write").contains("user:42");
    }
}
