package com.company.customerinfo.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("readinessDb")
public class DatabaseReadinessHealthIndicator implements HealthIndicator {

    private static final String VALIDATION_QUERY = "SELECT 1";

    private final DataSource dataSource;
    private final long maxLatencyMs;
    private final double maxActiveRatio;
    private final int maxWaitingThreads;

    public DatabaseReadinessHealthIndicator(
            DataSource dataSource,
            @Value("${app.health.db.max-latency-ms:200}") long maxLatencyMs,
            @Value("${app.health.db.max-active-ratio:0.90}") double maxActiveRatio,
            @Value("${app.health.db.max-waiting-threads:0}") int maxWaitingThreads
    ) {
        this.dataSource = dataSource;
        this.maxLatencyMs = maxLatencyMs;
        this.maxActiveRatio = maxActiveRatio;
        this.maxWaitingThreads = maxWaitingThreads;
    }

    @Override
    public Health health() {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(VALIDATION_QUERY)) {
            statement.execute();
            long latencyMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

            List<String> reasons = new ArrayList<>();
            if (latencyMs > maxLatencyMs) {
                reasons.add("latencyExceeded");
            }

            PoolMetrics poolMetrics = readPoolMetrics();
            if (poolMetrics.activeRatioExceeded(maxActiveRatio)) {
                reasons.add("activeRatioExceeded");
            }
            if (poolMetrics.threadsAwaitingExceeded(maxWaitingThreads)) {
                reasons.add("threadsAwaitingExceeded");
            }

            Health.Builder builder = reasons.isEmpty() ? Health.up() : Health.status(Status.OUT_OF_SERVICE);
            builder.withDetail("query", VALIDATION_QUERY)
                    .withDetail("queryLatencyMs", latencyMs)
                    .withDetail("thresholds", thresholdDetails());

            poolMetrics.writeDetails(builder);
            if (!reasons.isEmpty()) {
                builder.withDetail("reasons", reasons);
            }
            return builder.build();
        } catch (SQLException ex) {
            long latencyMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            return Health.down(ex)
                    .withDetail("query", VALIDATION_QUERY)
                    .withDetail("queryLatencyMs", latencyMs)
                    .withDetail("thresholds", thresholdDetails())
                    .build();
        }
    }

    private PoolMetrics readPoolMetrics() {
        try {
            HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool == null) {
                return PoolMetrics.empty();
            }

            int active = pool.getActiveConnections();
            int idle = pool.getIdleConnections();
            int total = pool.getTotalConnections();
            int waiting = pool.getThreadsAwaitingConnection();
            int max = hikari.getMaximumPoolSize();

            double activeRatio = max > 0 ? (double) active / max : -1.0;
            return new PoolMetrics(active, idle, total, max, waiting, activeRatio);
        } catch (SQLException ignored) {
            // DataSource is not Hikari or cannot be unwrapped; skip pool details.
            return PoolMetrics.empty();
        }
    }

    private Map<String, Object> thresholdDetails() {
        return Map.of(
                "maxLatencyMs", maxLatencyMs,
                "maxActiveRatio", maxActiveRatio,
                "maxWaitingThreads", maxWaitingThreads
        );
    }

    private record PoolMetrics(
            Integer active,
            Integer idle,
            Integer total,
            Integer max,
            Integer threadsAwaitingConnection,
            Double activeRatio
    ) {
        private static PoolMetrics empty() {
            return new PoolMetrics(null, null, null, null, null, null);
        }

        private boolean hasPoolData() {
            return active != null;
        }

        private boolean activeRatioExceeded(double threshold) {
            return activeRatio != null && activeRatio >= 0 && activeRatio > threshold;
        }

        private boolean threadsAwaitingExceeded(int threshold) {
            return threadsAwaitingConnection != null && threadsAwaitingConnection > threshold;
        }

        private void writeDetails(Health.Builder builder) {
            if (!hasPoolData()) {
                return;
            }
            builder.withDetail("pool", Map.of(
                    "active", active,
                    "idle", idle,
                    "total", total,
                    "max", max,
                    "threadsAwaitingConnection", threadsAwaitingConnection
            ));
            if (activeRatio != null && activeRatio >= 0) {
                builder.withDetail("activeRatio", activeRatio);
            }
        }
    }
}
