package com.ms.middleware.autonomy.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringEnvironmentTenantProviderTest {

    @Test
    void usesApplicationNameByDefault() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.application.name", "order-system");
        assertEquals("order-system", new SpringEnvironmentTenantProvider(env).getTenant());
    }

    @Test
    void tenantIdPropertyOverridesApplicationName() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.application.name", "order-system");
        env.setProperty("ms.middleware.autonomy.tenant-id", "custom-tenant");
        assertEquals("custom-tenant", new SpringEnvironmentTenantProvider(env).getTenant());
    }
}
