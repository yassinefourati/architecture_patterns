package com.application.tenancy.tenant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Builds one DataSource per tenant (different H2 in-memory DBs) and wraps them
 * in a TenantRoutingDataSource that picks the right one per request.
 *
 * Spring sees only the routing DataSource as the application's bean.
 */
@Configuration
public class TenantDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSource acme = TenantRoutingDataSource.buildTenantDataSource(
            "jdbc:h2:mem:tenant_acme;DB_CLOSE_DELAY=-1");
        DataSource globex = TenantRoutingDataSource.buildTenantDataSource(
            "jdbc:h2:mem:tenant_globex;DB_CLOSE_DELAY=-1");

        return TenantRoutingDataSource.forTenants("acme", Map.of(
            "acme", acme,
            "globex", globex
        ));
    }

    /**
     * Bootstraps both tenant schemas with the same DDL and a row of seed data
     * that makes it visible which DB you're hitting.
     */
    @Bean
    CommandLineRunner initTenantSchemas(DataSource routing) {
        return args -> {
            for (String tenant : new String[]{"acme", "globex"}) {
                try {
                    TenantContext.set(tenant);
                    JdbcTemplate jdbc = new JdbcTemplate(routing);
                    jdbc.execute("CREATE TABLE IF NOT EXISTS customers (id IDENTITY PRIMARY KEY, name VARCHAR(255), email VARCHAR(255))");
                    jdbc.update("INSERT INTO customers (name, email) VALUES (?, ?)",
                        "Seed-" + tenant, tenant + "-seed@example.com");
                } finally {
                    TenantContext.clear();
                }
            }
        };
    }
}
