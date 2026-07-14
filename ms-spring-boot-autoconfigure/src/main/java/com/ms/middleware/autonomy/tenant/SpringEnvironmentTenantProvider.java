package com.ms.middleware.autonomy.tenant;

import org.springframework.core.env.Environment;

/**
 * 从 Spring 环境解析 tenant 的默认实现。
 *
 * <p>优先级：{@code ms.middleware.autonomy.tenant-id}（非空）→ {@code spring.application.name} → {@code default}。
 * 多实例同应用名共享 tenant；不同应用名天然隔离账本 key {@code ms:autonomy:run:{tenant}:*}。</p>
 */
public class SpringEnvironmentTenantProvider implements AutonomyTenantProvider {

    private static final String DEFAULT_TENANT = "default";
    private static final String TENANT_ID_PROPERTY = "ms.middleware.autonomy.tenant-id";

    private final String tenant;

    public SpringEnvironmentTenantProvider(Environment environment) {
        String configured = environment.getProperty(TENANT_ID_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            this.tenant = configured.trim();
            return;
        }
        String appName = environment.getProperty("spring.application.name");
        this.tenant = (appName == null || appName.isBlank()) ? DEFAULT_TENANT : appName.trim();
    }

    @Override
    public String getTenant() {
        return tenant;
    }
}
