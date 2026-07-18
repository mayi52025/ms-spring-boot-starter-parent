package com.ms.middleware.console.agent.rag;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.recovery.RecoveryEvidence;
import com.ms.middleware.autonomy.run.AutonomyRun;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StableRunTextBuilderTest {

    @Test
    void buildsStructuredSummaryWithPrimaryActionAndRecovery() {
        AutonomyRun run = new AutonomyRun();
        run.setRunId("run-mq-1");
        run.setTenant("order-system");
        run.setStatus(AutonomyRunStatus.STABLE);
        run.setStabilizedAt(Instant.parse("2026-07-18T02:00:00Z"));

        AutonomyPlan plan = new AutonomyPlan();
        plan.setIncidentType("MQ_DEGRADED");
        plan.setSummary("MQ 失败偏高，优先限流");
        PlannedAction action = new PlannedAction();
        action.setActionType(AutonomyActionType.THROTTLE_CONSUMER);
        action.setRank(1);
        action.setReason("止血限流");
        action.setExecutionStatus("SUCCESS");
        plan.getActions().add(action);
        run.setPlan(plan);

        RecoveryEvidence evidence = new RecoveryEvidence();
        evidence.setSummary("近期失败窗口已回落");
        run.setRecoveryEvidence(evidence);
        run.setStartedAt(Instant.parse("2026-07-18T01:59:00Z"));
        run.setStabilizedAt(Instant.parse("2026-07-18T02:00:30Z"));

        String text = StableRunTextBuilder.build(run);

        assertTrue(text.contains("runId=run-mq-1"));
        assertTrue(text.contains("incident=MQ_DEGRADED"));
        assertTrue(text.contains("THROTTLE_CONSUMER"));
        assertTrue(text.contains("recovery=近期失败窗口已回落"));
        assertTrue(text.contains("mttrSeconds="));
        assertFalse(text.contains("HEARTBEAT"));
    }

    @Test
    void nullRunReturnsEmpty() {
        assertTrue(StableRunTextBuilder.build(null).isEmpty());
    }
}
