package com.ms.middleware.autonomy.run;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    /** 同一 JVM 内存账本：切换 tenant 后互不可见 */
    @Test
    void crossTenantIsolation() {
        AtomicReference<String> tenantRef = new AtomicReference<>(TENANT);
        ApplicationEventPublisher publisher = publishedEvents::add;
        InMemoryAutonomyLedger scopedLedger =
                new InMemoryAutonomyLedger(publisher, tenantRef::get, 10);

        tenantRef.set("order-system");
        scopedLedger.startRun(newRun("run-a"));

        tenantRef.set("payment-service");
        scopedLedger.startRun(newRun("run-b"));
        assertEquals(1, scopedLedger.listRecent(10).size());
        assertTrue(scopedLedger.get("run-b").isPresent());
        assertTrue(scopedLedger.get("run-a").isEmpty());

        tenantRef.set("order-system");
        assertTrue(scopedLedger.get("run-a").isPresent());
        assertTrue(scopedLedger.get("run-b").isEmpty());
    }

    /** 写入时强制覆盖 run 上的 tenant 字段，防止 key 污染 */
    @Test
    void startRunBindsCurrentTenantEvenIfRunCarriesForeignTenant() {
        AutonomyRun run = newRun("run-foreign");
        run.setTenant("other-app");
        ledger.startRun(run);
        assertEquals(TENANT, run.getTenant());
        assertTrue(ledger.get("run-foreign").isPresent());
    }

    private static AutonomyRun newRun(String runId) {
        AutonomyRun run = new AutonomyRun();
        run.setRunId(runId);
        run.setTenant(TENANT);
        return run;
    }
}
