package com.ms.middleware.autonomy;

import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.context.AutonomyContextBuilder;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.plan.AutonomyRuleEngine;
import com.ms.middleware.autonomy.policy.AutonomyPolicy;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
import com.ms.middleware.autonomy.run.InMemoryAutonomyLedger;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.MsMiddlewareProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MQ_DEGRADED 金路径：DETECT → PLAN（含 runbook 版本）→ AUTO → STABLE 并清空滑动窗口。
 */
@ExtendWith(MockitoExtension.class)
class AutonomyGoldenPathTest {

    private static final String TENANT = "order-system";

    @Mock
    private AutonomyContextBuilder contextBuilder;
    @Mock
    private AutonomyActuator actuator;

    private InMemoryAutonomyLedger ledger;
    private AutonomyOrchestrator orchestrator;
    private MsMetrics msMetrics;
    private MsMiddlewareProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MsMiddlewareProperties();
        properties.getAutonomy().setMqFailedWarnThreshold(3);
        properties.getAutonomy().setAutoExecuteMinConfidenceLow(0.55);
        properties.getAutonomy().setAutoExecuteMinConfidence(0.7);

        List<Object> events = new ArrayList<>();
        ApplicationEventPublisher publisher = events::add;
        AutonomyTenantProvider tenantProvider = () -> TENANT;
        ledger = new InMemoryAutonomyLedger(publisher, tenantProvider, 20);

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        msMetrics = new MsMetrics(meterRegistry);
        AutonomyMetrics autonomyMetrics = new AutonomyMetrics(meterRegistry);
        AutonomyPolicy policy = new AutonomyPolicy(properties);
        AutonomyRuleEngine decisionEngine = new AutonomyRuleEngine();

        doAnswer(inv -> {
            PlannedActionHelper.markSuccess(inv.getArgument(0));
            return null;
        }).when(actuator).execute(any());

        orchestrator = new AutonomyOrchestrator(
                contextBuilder,
                decisionEngine,
                policy,
                actuator,
                ledger,
                tenantProvider,
                autonomyMetrics,
                msMetrics);
    }

    @Test
    void mqDegradedGoldenPathAutoThenStable() {
        AutonomyContext degraded = mqDegradedContext(5);
        AutonomyContext recovered = mqDegradedContext(0);

        when(contextBuilder.build())
                .thenReturn(degraded, degraded, recovered, recovered);
        when(contextBuilder.isIncidentResolved(org.mockito.ArgumentMatchers.eq("MQ_DEGRADED"),
                org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            AutonomyContext ctx = inv.getArgument(1);
            return ctx.getMqFailedCount() < properties.getAutonomy().getMqFailedWarnThreshold();
        });

        orchestrator.tick();

        List<AutonomyRun> active = ledger.listActive();
        assertEquals(1, active.size());
        AutonomyRun run = active.get(0);
        assertEquals(AutonomyRunStatus.EXECUTING, run.getStatus());
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.PLAN.code().equals(e.getPhase())
                        && e.getMessage().contains("runbook=default@1.0")));
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.AUTO.code().equals(e.getPhase())));

        orchestrator.tick();

        assertTrue(ledger.listActive().isEmpty() || ledger.listActive().stream()
                .allMatch(r -> r.getStatus() == AutonomyRunStatus.STABLE));
        assertEquals(AutonomyRunStatus.STABLE, run.getStatus());
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.STABLE.code().equals(e.getPhase())
                        && e.getMessage().contains("MQ窗口失败 5→0")));
        assertNotNull(run.getRecoveryEvidence());
        assertEquals("MQ窗口失败 5→0（阈值<3）", run.getRecoveryEvidence().getSummary());
        verify(actuator).clearMqThrottle();
        assertEquals(0, msMetrics.getMessageFailedCount());
    }

    private AutonomyContext mqDegradedContext(long failedCount) {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setRedisHealthy(true);
        ctx.setRabbitMqHealthy(true);
        ctx.setMqFailedCount(failedCount);
        ctx.setMqFailedWarnThreshold(3);
        ctx.setCacheHitRate(0.95);
        ctx.setCacheHitRateWarnThreshold(0.5);
        if (failedCount >= 3) {
            ctx.getIssues().add("MQ 消费失败（窗口内）偏高: " + failedCount);
        }
        return ctx;
    }

    /** 测试辅助：模拟 Actuator 成功执行 */
    private static final class PlannedActionHelper {
        private PlannedActionHelper() {
        }

        static void markSuccess(com.ms.middleware.autonomy.plan.PlannedAction action) {
            action.setExecutionStatus("SUCCESS");
            action.setExecutionDetail("test ok");
        }
    }
}
