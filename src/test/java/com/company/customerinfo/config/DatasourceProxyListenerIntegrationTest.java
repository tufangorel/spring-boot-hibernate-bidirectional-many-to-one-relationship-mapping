package com.company.customerinfo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class DatasourceProxyListenerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldTriggerFastAndSlowQueryPaths() {
        // Fast query path: should stay below threshold.
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        // Slow query path: force elapsed time above threshold.
        jdbcTemplate.execute("CREATE ALIAS IF NOT EXISTS SLEEP FOR \"java.lang.Thread.sleep(long)\"");
        jdbcTemplate.queryForObject("SELECT SLEEP(75)", Long.class);
    }
}
