package com.application.tenancy.tenant;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * AbstractRoutingDataSource is Spring's official "dispatch DataSource by key"
 * primitive. We use the current tenant id from TenantContext as the lookup key.
 *
 * Each tenant has its own physical DataSource (its own connection pool to its
 * own schema). Hibernate gets a different real connection depending on which
 * tenant is in context.
 *
 * Trade-off: connection pool count = tenant count Ã— pool size. Fine for a few
 * dozen tenants; for hundreds, use a discriminator column instead (single
 * schema with tenant_id on every row), or pool-per-app with a SET search_path.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    public static DataSource forTenants(String defaultTenant, Map<String, DataSource> tenants) {
        TenantRoutingDataSource ds = new TenantRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>(tenants);
        ds.setTargetDataSources(targets);
        ds.setDefaultTargetDataSource(tenants.get(defaultTenant));
        ds.afterPropertiesSet();
        return ds;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.get();  // null falls back to the default
    }

    public static DataSource buildTenantDataSource(String jdbcUrl) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setMaximumPoolSize(5);
        ds.setPoolName("hikari-" + jdbcUrl.substring(jdbcUrl.lastIndexOf(':') + 1));
        return ds;
    }
}
