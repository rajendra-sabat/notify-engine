package com.notifyengine.config;

public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(String schema) {
        currentTenant.set(schema);
    }

    public static String getTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
