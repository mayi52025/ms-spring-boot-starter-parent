package com.ms.middleware.autonomy;

import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.context.AutonomyContextBuilder;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.orchestrator.AutonomyTickLock;
import com.ms.middleware.autonomy.plan.AutonomyRuleEngine;
import com.ms.middleware.autonomy.policy.AutonomyPolicy;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分布式 tick 锁：未获锁时跳过编排，避免多实例重复 AUTO。
 */
@ExtendWith(MockitoExtension.class)
class AutonomyOrchestratorDistributedLockTest {

    private static final String TENANT = "order-system";

    @Mock
    private AutonomyContextBuilder contextBuilder;
    @Mock
    private AutonomyActuator actuator;

    private InMemoryAutonomyLedger ledger;
    private AutonomyOrchestrator orchestrator;
    private AutonomyContext healthyContext;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        List<Object> events = new ArrayList<>();
        ApplicationEventPublisher publisher = events::add;
        AutonomyTenantProvider tenantProvider = () -> TENANT;
        ledger = new InMemoryAutonomyLedger(publisher, tenantProvider, 20);

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics msMetrics = new MsMetrics(meterRegistry);
        AutonomyMetrics autonomyMetrics = new AutonomyMetrics(meterRegistry);
        AutonomyPolicy policy = new AutonomyPolicy(properties);
        AutonomyRuleEngine decisionEngine = new AutonomyRuleEngine();

        healthyContext = new AutonomyContext();

        AutonomyTickLock blockingLock = (tenant, action) -> { /* skip tick */ };
        orchestrator = new AutonomyOrchestrator(
                contextBuilder,
                decisionEngine,
                policy,
                actuator,
                ledger,
                tenantProvider,
                autonomyMetrics,
                msMetrics,
                blockingLock);
    }

    @Test
    void tickSkippedWhenDistributedLockNotAcquired() {
        orchestrator.tick();

        assertEquals(0, ledger.listRecent(10).size());
        verify(contextBuilder, never()).build();
    }

    @Test
    void tickRunsWhenLockAllows() {
        AtomicInteger runs = new AtomicInteger();
        AutonomyTickLock passThroughLock = (tenant, action) -> {
            runs.incrementAndGet();
            action.run();
        };
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        orchestrator = new AutonomyOrchestrator(
                contextBuilder,
                new AutonomyRuleEngine(),
                new AutonomyPolicy(properties),
                actuator,
                ledger,
                () -> TENANT,
                new AutonomyMetrics(meterRegistry),
                new MsMetrics(meterRegistry),
                passThroughLock);

        when(contextBuilder.build()).thenReturn(healthyContext);

        orchestrator.tick();

        assertEquals(1, runs.get());
    }
}
