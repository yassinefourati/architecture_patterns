package com.application.tenancy.tenant;

/**
 * ThreadLocal holder for the current tenant. Set on request entry by a filter,
 * cleared at exit, read by the DataSource routing and by Hibernate.
 *
 * Be careful with async: when offloading work to another thread (CompletableFuture,
 * @Async, etc.), explicitly capture and propagate the tenant â€” ThreadLocals do not
 * cross thread boundaries.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static String require() {
        String t = CURRENT.get();
        if (t == null) throw new IllegalStateException("No tenant set on current thread");
        return t;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
