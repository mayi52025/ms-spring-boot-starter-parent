package com.ms.middleware.autonomy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.context.AutonomyContextBuilder;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.plan.AutonomyRuleEngine;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.policy.AutonomyPolicy;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
import com.ms.middleware.autonomy.run.InMemoryAutonomyLedger;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MQ 限流安全兜底：超时 SAFETY_UNWIND、无改善 ESCALATE、STABLE 仍清限流。
 */
@ExtendWith(MockitoExtension.class)
class AutonomyThrottleSafetyTest {

    private static final String TENANT = "order-system";

    @Mock
    private AutonomyContextBuilder contextBuilder;
    @Mock
    private AutonomyActuator actuator;

    private InMemoryAutonomyLedger ledger;
    private AutonomyOrchestrator orchestrator;
    private MsMiddlewareProperties properties;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        properties = new MsMiddlewareProperties();
        properties.getAutonomy().setMqFailedWarnThreshold(3);
        properties.getAutonomy().setAutoExecuteMinConfidenceLow(0.55);
        properties.getAutonomy().getMq().setThrottleMaxDurationSeconds(300);
        properties.getAutonomy().getMq().setThrottleNoImproveTicks(3);

        ApplicationEventPublisher publisher = new ArrayList<>()::add;
        AutonomyTenantProvider tenantProvider = () -> TENANT;
        ledger = new InMemoryAutonomyLedger(publisher, tenantProvider, 20);

        meterRegistry = new SimpleMeterRegistry();
        MsMetrics msMetrics = new MsMetrics(meterRegistry);
        AutonomyMetrics autonomyMetrics = new AutonomyMetrics(meterRegistry);

        doAnswer(inv -> {
            PlannedAction action = inv.getArgument(0);
            action.setExecutionStatus("SUCCESS");
            action.setExecutionDetail("test ok");
            return null;
        }).when(actuator).execute(any());

        orchestrator = new AutonomyOrchestrator(
                contextBuilder,
                new AutonomyRuleEngine(),
                new AutonomyPolicy(properties),
                actuator,
                ledger,
                tenantProvider,
                autonomyMetrics,
                msMetrics,
                properties);
    }

    @Test
    void throttleTimeoutTriggersSafetyUnwind() {
        AutonomyContext degraded = mqDegraded(5);
        when(contextBuilder.build()).thenReturn(degraded);
        when(contextBuilder.isIncidentResolved(any(), any())).thenReturn(false);

        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        orchestrator.useClock(Clock.fixed(t0, ZoneOffset.UTC));
        orchestrator.tick();

        AutonomyRun run = ledger.listActive().get(0);
        assertEquals(AutonomyRunStatus.EXECUTING, run.getStatus());
        assertTrue(run.getMqThrottleEnabledAt() != null);

        // 推进超过 throttle-max-duration-seconds
        orchestrator.useClock(Clock.fixed(t0.plusSeconds(301), ZoneOffset.UTC));
        orchestrator.tick();

        assertEquals(AutonomyRunStatus.EXECUTING, run.getStatus());
        assertTrue(run.isMqThrottleSafetyConsumed());
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.SAFETY_UNWIND.code().equals(e.getPhase())
                        && e.getMessage().contains("限流超时保护")));
        verify(actuator, atLeastOnce()).clearMqThrottle();
        assertEquals(1.0, meterRegistry.get("ms.autonomy.throttle.safety_unwind.total").counter().count());
    }

    @Test
    void throttleNoImproveEscalates() {
        AutonomyContext degraded = mqDegraded(5);
        when(contextBuilder.build()).thenReturn(degraded);
        when(contextBuilder.isIncidentResolved(any(), any())).thenReturn(false);

        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        orchestrator.useClock(Clock.fixed(t0, ZoneOffset.UTC));
        orchestrator.tick();

        AutonomyRun run = ledger.listActive().get(0);
        assertEquals(AutonomyRunStatus.EXECUTING, run.getStatus());

        // 连续 3 次 tick 失败数未下降
        for (int i = 0; i < 3; i++) {
            orchestrator.useClock(Clock.fixed(t0.plusSeconds(10L * (i + 1)), ZoneOffset.UTC));
            orchestrator.tick();
        }

        assertEquals(AutonomyRunStatus.ESCALATED, run.getStatus());
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.ESCALATE.code().equals(e.getPhase())));
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.ADVISE.code().equals(e.getPhase())
                        && e.getMessage().contains("勿继续自动加压")));
        verify(actuator, atLeastOnce()).clearMqThrottle();
        assertEquals(1.0, meterRegistry.get("ms.autonomy.run.escalated.total")
                .tag("reason", "throttle_no_improve").counter().count());
    }

    @Test
    void stableStillClearsThrottleAfterAuto() {
        AutonomyContext degraded = mqDegraded(5);
        AutonomyContext recovered = mqDegraded(0);
        when(contextBuilder.build()).thenReturn(degraded, degraded, recovered, recovered);
        when(contextBuilder.isIncidentResolved(org.mockito.ArgumentMatchers.eq("MQ_DEGRADED"), any()))
                .thenAnswer(inv -> {
                    AutonomyContext ctx = inv.getArgument(1);
                    return ctx.getMqFailedCount() < 3;
                });

        orchestrator.tick();
        AutonomyRun run = ledger.listActive().get(0);
        assertEquals(AutonomyRunStatus.EXECUTING, run.getStatus());

        orchestrator.tick();
        assertEquals(AutonomyRunStatus.STABLE, run.getStatus());
        assertFalse(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.ESCALATE.code().equals(e.getPhase())));
        verify(actuator, atLeastOnce()).clearMqThrottle();
    }

    private static AutonomyContext mqDegraded(long failedCount) {
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
}
