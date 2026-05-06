package com.company.customerinfo.config;

import com.company.customerinfo.CustomerInfoApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GracefulShutdownIntegrationTest {

    @Test
    void shouldShutdownGracefullyAndInvokeLifecycleHooks(CapturedOutput output) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CustomerInfoApplication.class)
                .properties(
                        "spring.profiles.active=dev",
                        "server.port=0",
                        "management.server.port=0"
                )
                .run()) {
            assertThat(context.isActive()).isTrue();
            assertThat(context.getEnvironment().getProperty("server.shutdown")).isEqualTo("graceful");
            assertThat(context.getEnvironment().getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
                    .isEqualTo("30s");
        }

        assertThat(output.getOut()).contains("Graceful shutdown started. Waiting for in-flight requests to complete.");
        assertThat(output.getOut()).contains("Graceful shutdown complete. Application resources released.");
    }
}
