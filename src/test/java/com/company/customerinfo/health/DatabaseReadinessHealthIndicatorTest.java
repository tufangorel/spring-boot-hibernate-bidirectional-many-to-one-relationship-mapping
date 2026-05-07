package com.company.customerinfo.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseReadinessHealthIndicatorTest {

    @Test
    void healthReturnsUpWhenQuerySucceedsAndThresholdsAreNotExceeded() throws Exception {
        DataSource dataSource = mockQuerySuccessDataSource();
        when(dataSource.unwrap(HikariDataSource.class)).thenThrow(new SQLException("No hikari"));

        DatabaseReadinessHealthIndicator indicator =
                new DatabaseReadinessHealthIndicator(dataSource, 200, 0.9, 0);

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
        assertThat(result.getDetails()).containsKey("queryLatencyMs");
        assertThat(result.getDetails()).doesNotContainKey("reasons");
    }

    @Test
    void healthReturnsOutOfServiceWhenPoolPressureExceedsThresholds() throws Exception {
        DataSource dataSource = mockQuerySuccessDataSource();
        HikariDataSource hikariDataSource = mock(HikariDataSource.class);
        HikariPoolMXBean poolMXBean = mock(HikariPoolMXBean.class);

        when(dataSource.unwrap(HikariDataSource.class)).thenReturn(hikariDataSource);
        when(hikariDataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(hikariDataSource.getMaximumPoolSize()).thenReturn(10);
        when(poolMXBean.getActiveConnections()).thenReturn(9);
        when(poolMXBean.getIdleConnections()).thenReturn(1);
        when(poolMXBean.getTotalConnections()).thenReturn(10);
        when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(2);

        DatabaseReadinessHealthIndicator indicator =
                new DatabaseReadinessHealthIndicator(dataSource, 5_000, 0.5, 0);

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(result.getDetails()).containsKey("pool");
        assertThat(result.getDetails()).containsEntry("activeRatio", 0.9d);
        assertThat(result.getDetails().get("reasons"))
                .asInstanceOf(LIST)
                .contains("activeRatioExceeded", "threadsAwaitingExceeded");
    }

    @Test
    void healthReturnsDownWhenDatabaseConnectionFails() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection unavailable"));

        DatabaseReadinessHealthIndicator indicator =
                new DatabaseReadinessHealthIndicator(dataSource, 200, 0.9, 0);

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        assertThat(result.getDetails()).containsEntry("query", "SELECT 1");
        assertThat(result.getDetails()).containsKey("queryLatencyMs");
    }

    @Test
    void healthIncludesPoolDetailsWhenHikariDataSourceIsAvailable() throws Exception {
        DataSource dataSource = mockQuerySuccessDataSource();
        HikariDataSource hikariDataSource = mock(HikariDataSource.class);
        HikariPoolMXBean poolMXBean = mock(HikariPoolMXBean.class);

        when(dataSource.unwrap(HikariDataSource.class)).thenReturn(hikariDataSource);
        when(hikariDataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(hikariDataSource.getMaximumPoolSize()).thenReturn(20);
        when(poolMXBean.getActiveConnections()).thenReturn(4);
        when(poolMXBean.getIdleConnections()).thenReturn(6);
        when(poolMXBean.getTotalConnections()).thenReturn(10);
        when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(0);

        DatabaseReadinessHealthIndicator indicator =
                new DatabaseReadinessHealthIndicator(dataSource, 5_000, 0.9, 3);

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
        assertThat(result.getDetails()).containsKey("pool");
        assertThat(result.getDetails().get("pool"))
                .asInstanceOf(MAP)
                .containsEntry("active", 4)
                .containsEntry("idle", 6)
                .containsEntry("total", 10)
                .containsEntry("max", 20)
                .containsEntry("threadsAwaitingConnection", 0);
    }

    private DataSource mockQuerySuccessDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT 1")).thenReturn(statement);
        when(statement.execute()).thenReturn(true);
        return dataSource;
    }
}
