package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link AutonomyRuleEngine} 的 incident 分类、规则选优与 PLAN 选优说明。
 */
class AutonomyRuleEngineTest {

    private final AutonomyRuleEngine engine = new AutonomyRuleEngine();

    /** 无 issues 时返回 NONE */
    @Test
    void noIncidentReturnsNone() {
        AutonomyContext ctx = new AutonomyContext();
        AutonomyPlan plan = engine.plan(ctx);
        assertEquals("NONE", plan.getIncidentType());
        assertTrue(plan.getActions().isEmpty());
    }

    /** Redis 不可用：自愈排第一，且 PLAN 含选优说明 */
    @Test
    void redisDownPlansRankedRecoveryFirst() {
        AutonomyContext ctx = incidentContext();
        ctx.setRedisHealthy(false);
        ctx.getIssues().add("Redis 不可用");

        AutonomyPlan plan = engine.plan(ctx);
        assertEquals("REDIS_UNAVAILABLE", plan.getIncidentType());
        List<PlannedAction> actions = plan.getActions();
        assertFalse(actions.isEmpty());
        assertEquals(AutonomyActionType.TRIGGER_REDIS_RECOVERY, actions.get(0).getActionType());
        assertEquals(1, actions.get(0).getRank());
        assertTrue(actions.stream().anyMatch(a -> a.getActionType() == AutonomyActionType.ENSURE_L1_DEGRADE));
        assertNotNull(plan.getRankingSummary());
        assertTrue(plan.getRankingSummary().contains("自动执行候选"));
        assertFalse(plan.getRecommendations().isEmpty());
    }

    /** MQ 未达阈值时不应走 MQ_DEGRADED，应落到缓存类 incident */
    @Test
    void mqBelowThresholdDoesNotPlanMqDegraded() {
        AutonomyContext ctx = incidentContext();
        ctx.setMqFailedCount(5);
        ctx.setMqFailedWarnThreshold(10);
        ctx.getIssues().add("缓存命中率偏低: 40.0%");
        ctx.setCacheHitRate(0.4);
        ctx.setCacheHitRateWarnThreshold(0.5);

        AutonomyPlan plan = engine.plan(ctx);

        assertEquals("CACHE_DEGRADED", plan.getIncidentType());
    }

    /** MQ 达到阈值：限流排第一；踩线时证据不足，仅 ADVISE */
    @Test
    void mqAtThresholdSelectsThrottleButWeakEvidence() {
        AutonomyContext ctx = incidentContext();
        ctx.setMqFailedCount(10);
        ctx.setMqFailedWarnThreshold(10);
        ctx.getIssues().add("MQ 消费失败累计偏高: 10");

        AutonomyPlan plan = engine.plan(ctx);

        assertEquals("MQ_DEGRADED", plan.getIncidentType());
        PlannedAction top = plan.getActions().get(0);
        assertEquals(AutonomyActionType.THROTTLE_CONSUMER, top.getActionType());
        assertEquals(1, top.getRank());
        assertTrue(top.getConfidence() < 0.7, "踩线时应 ADVISE，证据=" + top.getConfidence());
        assertTrue(plan.getRankingSummary().contains("根因优先"));
    }

    /** MQ 明显超阈值：限流为 AUTO 候选 */
    @Test
    void mqAboveThresholdSelectsThrottleAsAutoCandidate() {
        AutonomyContext ctx = incidentContext();
        ctx.setMqFailedCount(15);
        ctx.setMqFailedWarnThreshold(10);
        ctx.getIssues().add("MQ 消费失败累计偏高: 15");

        AutonomyPlan plan = engine.plan(ctx);

        PlannedAction top = plan.getActions().get(0);
        assertEquals(AutonomyActionType.THROTTLE_CONSUMER, top.getActionType());
        assertTrue(top.getConfidence() >= 0.7);
    }

    /** Rabbit 宕机优先于 MQ 失败计数 */
    @Test
    void rabbitDownTakesPriorityOverMqDegraded() {
        AutonomyContext ctx = incidentContext();
        ctx.setRabbitMqHealthy(false);
        ctx.setMqFailedCount(20);
        ctx.setMqFailedWarnThreshold(10);
        ctx.getIssues().add("RabbitMQ 不可用");

        AutonomyPlan plan = engine.plan(ctx);

        assertEquals("RABBITMQ_UNAVAILABLE", plan.getIncidentType());
        assertEquals(AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY, plan.getActions().get(0).getActionType());
    }

    /** 构造「有 incident 且 Redis/Rabbit 健康」的通用上下文 */
    private AutonomyContext incidentContext() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setRedisHealthy(true);
        ctx.setRabbitMqHealthy(true);
        ctx.setMqFailedWarnThreshold(10);
        ctx.setCacheHitRateWarnThreshold(0.5);
        return ctx;
    }
}
