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

    /** Step 6：决策链路指标 */
    @Test
    void recordDecisionMetrics() {
        metrics.recordActionAuto("order-system", "MQ_DEGRADED", "THROTTLE_CONSUMER", "auto");
        metrics.recordRecommendation("order-system", "MQ_DEGRADED");
        metrics.recordRecommendationAccepted("order-system", "MQ_DEGRADED");
        metrics.recordRecommendationRejected("order-system", "REDIS_UNAVAILABLE");
        metrics.recordPlanConfidence("order-system", "MQ_DEGRADED", 0.82);

        assertEquals(1.0, registry.get("ms.autonomy.action.auto.total")
                .tag("trigger", "auto").counter().count());
        assertEquals(1.0, registry.get("ms.autonomy.recommendation.total").counter().count());
        assertEquals(1.0, registry.get("ms.autonomy.recommendation.accepted.total").counter().count());
        assertEquals(1.0, registry.get("ms.autonomy.recommendation.rejected.total").counter().count());
        assertEquals(1, registry.get("ms.autonomy.plan.confidence").summary().count());
    }
}
