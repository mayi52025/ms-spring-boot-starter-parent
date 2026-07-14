package com.ms.middleware.autonomy.insight;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.InMemoryAutonomyLedger;
import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Insight 层 tenant 防御性过滤：控制台 issues/history 仅当前应用。
 */
class DefaultMiddlewareInsightServiceTenantTest {

    private static final String ORDER_TENANT = "order-system";

    private final AtomicReference<String> tenantRef = new AtomicReference<>(ORDER_TENANT);
    private InMemoryAutonomyLedger ledger;
    private DefaultMiddlewareInsightService insightService;

    @BeforeEach
    void setUp() {
        List<Object> events = new ArrayList<>();
        ApplicationEventPublisher publisher = events::add;
        ledger = new InMemoryAutonomyLedger(publisher, tenantRef::get, 20);
        MsMetrics metrics = new MsMetrics(new SimpleMeterRegistry());
        AutonomyMetrics autonomyMetrics = new AutonomyMetrics(new SimpleMeterRegistry());
        insightService = new DefaultMiddlewareInsightService(
                ledger, metrics, autonomyMetrics, tenantRef::get);
    }

    @Test
    void listActiveRunsOnlyCurrentTenant() {
        tenantRef.set("order-system");
        ledger.startRun(newRun("order-run", AutonomyRunStatus.EXECUTING));

        tenantRef.set("payment-service");
        ledger.startRun(newRun("pay-run", AutonomyRunStatus.EXECUTING));

        tenantRef.set("order-system");
        assertEquals(1, insightService.listActiveRuns().size());
        assertEquals("order-run", insightService.listActiveRuns().get(0).getRunId());
    }

    @Test
    void getRunHiddenForOtherTenant() {
        tenantRef.set("payment-service");
        ledger.startRun(newRun("pay-only", AutonomyRunStatus.EXECUTING));

        tenantRef.set("order-system");
        assertTrue(insightService.getRun("pay-only").isEmpty());
    }

    private static AutonomyRun newRun(String runId, AutonomyRunStatus status) {
        AutonomyRun run = new AutonomyRun();
        run.setRunId(runId);
        run.setStatus(status);
        return run;
    }
}
