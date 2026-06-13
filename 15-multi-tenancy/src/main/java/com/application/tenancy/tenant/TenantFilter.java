package com.application.tenancy.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Reads X-Tenant-Id header (or query param fallback) on every request,
 * validates against the known tenants, and sets TenantContext.
 *
 * Real systems would resolve from JWT claims, subdomain, or a path prefix â€”
 * the mechanism is identical, the source is what changes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {

    public static final String HEADER = "X-Tenant-Id";
    private static final Set<String> KNOWN_TENANTS = Set.of("acme", "globex");
    private static final Set<String> PUBLIC_PATHS = Set.of("/error");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        HttpServletResponse httpResp = (HttpServletResponse) res;

        // Don't enforce tenant on error handler
        if (PUBLIC_PATHS.contains(http.getRequestURI())) {
            chain.doFilter(req, res);
            return;
        }

        String tenantId = http.getHeader(HEADER);
        if (tenantId == null) tenantId = http.getParameter("tenant");

        if (tenantId == null || !KNOWN_TENANTS.contains(tenantId)) {
            httpResp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(
                "{\"error\":\"missing or unknown tenant; set " + HEADER +
                " to one of: " + KNOWN_TENANTS + "\"}");
            return;
        }

        try {
            TenantContext.set(tenantId);
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();  // critical: don't leak the tenant into the next request on this thread
        }
    }
}
