package com.ms.middleware.autonomy.tenant;

/**
 * 多应用/多租户标识（Phase 4 扩展鉴权与隔离）。
 */
public interface AutonomyTenantProvider {

    String getTenant();
}
