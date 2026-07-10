package com.ms.middleware.autonomy.run;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAutonomyLedgerTest {

    private static final String TENANT = "order-system";

    private final List<Object> publishedEvents = new ArrayList<>();
    private InMemoryAutonomyLedger ledger;

    @BeforeEach
    void setUp() {
        ApplicationEventPublisher publisher = publishedEvents::add;
        AutonomyTenantProvider tenantProvider = () -> TENANT;
        ledger = new InMemoryAutonomyLedger(publisher, tenantProvider, 10);
    }

    @Test
    void startRunStoresTenantScopedRun() {
        AutonomyRun run = newRun("abc12345");
        ledger.startRun(run);

        assertTrue(ledger.get("abc12345").isPresent());
        assertEquals(TENANT, ledger.get("abc12345").orElseThrow().getTenant());
        assertEquals(1, ledger.listRecent(5).size());
        assertFalse(publishedEvents.isEmpty());
    }

    @Test
    void listActiveExcludesStableRuns() {
        AutonomyRun active = newRun("run-active");
        ledger.startRun(active);

        AutonomyRun stable = newRun("run-stable");
        stable.setStatus(AutonomyRunStatus.STABLE);
        ledger.startRun(stable);

        List<AutonomyRun> activeRuns = ledger.listActive();
        assertEquals(1, activeRuns.size());
        assertEquals("run-active", activeRuns.get(0).getRunId());
    }

    @Test
    void appendTimelineAddsEventAndPublishes() {
        AutonomyRun run = newRun("tl-run01");
        ledger.startRun(run);
        int before = publishedEvents.size();

        ledger.appendTimeline(run, "PLAN", "生成处置计划");

        assertEquals(before + 1, publishedEvents.size());
        assertEquals(2, run.getTimeline().size());
        assertEquals("PLAN", run.getTimeline().get(1).getPhase());
    }

    private static AutonomyRun newRun(String runId) {
        AutonomyRun run = new AutonomyRun();
        run.setRunId(runId);
        run.setTenant(TENANT);
        return run;
    }
}
