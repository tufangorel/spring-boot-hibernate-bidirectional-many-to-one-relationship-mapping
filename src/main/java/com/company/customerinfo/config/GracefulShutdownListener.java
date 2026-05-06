package com.company.customerinfo.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GracefulShutdownListener {

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        log.info("Graceful shutdown started. Waiting for in-flight requests to complete.");
    }

    @PreDestroy
    public void onPreDestroy() {
        log.info("Graceful shutdown complete. Application resources released.");
    }
}
