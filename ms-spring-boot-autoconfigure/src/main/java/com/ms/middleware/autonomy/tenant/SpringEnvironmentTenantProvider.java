package com.ms.middleware.autonomy.tenant;

import org.springframework.core.env.Environment;

/**
 * 默认以 spring.application.name 作为 tenant。
 */
public class SpringEnvironmentTenantProvider implements AutonomyTenantProvider {

    private static final String DEFAULT_TENANT = "default";

    private final String tenant;

    public SpringEnvironmentTenantProvider(Environment environment) {
        String name = environment.getProperty("spring.application.name");
        this.tenant = (name == null || name.isBlank()) ? DEFAULT_TENANT : name.trim();
    }

    @Override
    public String getTenant() {
        return tenant;
    }
}
