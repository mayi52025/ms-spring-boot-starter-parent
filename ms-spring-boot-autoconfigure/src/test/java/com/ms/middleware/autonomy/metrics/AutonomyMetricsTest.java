package com.ms.middleware.autonomy.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutonomyMetricsTest {

    private SimpleMeterRegistry registry;
    private AutonomyMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AutonomyMetrics(registry);
    }

    @Test
    void recordRunStabilizedIncrementsCounters() {
        metrics.recordRunStabilized("order-system", "REDIS_UNAVAILABLE", 42);

        assertEquals(42, metrics.getLastMttrSeconds());
        assertEquals(1, metrics.getCompletedRunCount());
        assertEquals(1.0, registry.get("ms.autonomy.run.total")
                .tag("tenant", "order-system")
                .tag("incident_type", "REDIS_UNAVAILABLE")
                .counter().count());
        assertEquals(1, registry.get("ms.autonomy.mttr")
                .tag("tenant", "order-system")
                .tag("incident_type", "REDIS_UNAVAILABLE")
                .timer().count());
    }
}
