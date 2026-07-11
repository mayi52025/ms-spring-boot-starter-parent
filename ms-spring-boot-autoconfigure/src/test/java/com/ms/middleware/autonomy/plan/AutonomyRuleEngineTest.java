package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link AutonomyRuleEngine} 的 incident 分类与优先级：
 * Redis &gt; Rabbit &gt; MQ（阈值）&gt; Cache。
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

    /** Redis 不可用应生成降级 + 自愈动作 */
    @Test
    void redisDownPlansRecoveryActions() {
        AutonomyContext ctx = incidentContext();
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

    /** MQ 达到阈值应生成 MQ_DEGRADED 计划 */
    @Test
    void mqAtThresholdPlansMqDegraded() {
        AutonomyContext ctx = incidentContext();
        ctx.setMqFailedCount(10);
        ctx.setMqFailedWarnThreshold(10);
        ctx.getIssues().add("MQ 消费失败累计偏高: 10");

        AutonomyPlan plan = engine.plan(ctx);

        assertEquals("MQ_DEGRADED", plan.getIncidentType());
        assertTrue(plan.getSummary().contains("阈值 10"));
        assertFalse(plan.getRecommendations().isEmpty());
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
