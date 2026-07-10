package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomyRuleEngineTest {

    private final AutonomyRuleEngine engine = new AutonomyRuleEngine();

    @Test
    void noIncidentReturnsNone() {
        AutonomyContext ctx = new AutonomyContext();
        AutonomyPlan plan = engine.plan(ctx);
        assertEquals("NONE", plan.getIncidentType());
        assertTrue(plan.getActions().isEmpty());
    }

    @Test
    void redisDownPlansRecoveryActions() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setRedisHealthy(false);
        ctx.getIssues().add("Redis 不可用");

        AutonomyPlan plan = engine.plan(ctx);
        assertEquals("REDIS_UNAVAILABLE", plan.getIncidentType());
        List<PlannedAction> actions = plan.getActions();
        assertFalse(actions.isEmpty());
        assertTrue(actions.stream().anyMatch(a -> a.getActionType() == AutonomyActionType.ENSURE_L1_DEGRADE));
        assertTrue(actions.stream().anyMatch(a -> a.getActionType() == AutonomyActionType.TRIGGER_REDIS_RECOVERY));
        assertFalse(plan.getRecommendations().isEmpty());
    }
}
