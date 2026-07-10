package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于规则的处置计划（Phase 1 默认决策引擎，Phase 3 由 EasyRules 逐步替换）。
 */
public class AutonomyRuleEngine implements AutonomyDecisionEngine {

    @Override
    public AutonomyPlan plan(AutonomyContext context) {
        AutonomyPlan plan = new AutonomyPlan();
        plan.setContext(context);

        if (!context.hasIncident()) {
            plan.setIncidentType("NONE");
            plan.setSummary("中间件状态正常");
            return plan;
        }

        List<PlannedAction> actions = new ArrayList<>();
        List<AutonomyRecommendation> recommendations = new ArrayList<>();

        if (!context.isRedisHealthy()) {
            plan.setIncidentType("REDIS_UNAVAILABLE");
            plan.setSummary("Redis 不可用，启用本地缓存与自愈组合处置");

            actions.add(action(AutonomyActionType.ENSURE_L1_DEGRADE,
                    "Redis 不可用时 MultiLevelCache 在请求路径自动降级至 L1"));
            if (!context.getHotKeys().isEmpty()) {
                actions.add(action(AutonomyActionType.WARMUP_HOT_KEYS,
                        "热点 Key 较多，建议在 Redis 恢复前后预热本地缓存"));
            }
            actions.add(action(AutonomyActionType.TRIGGER_REDIS_RECOVERY,
                    "主动触发 Redis 连接自愈"));

            recommendations.add(new AutonomyRecommendation(
                    "缩短本地缓存 TTL 差",
                    "可将 distributed.ttl 与 local.ttl 比例调至 1:8，减少 Redis 恢复后陈旧数据窗口",
                    "ms.middleware.cache.distributed.ttl / local.ttl"));
            recommendations.add(new AutonomyRecommendation(
                    "开启热点自动预热",
                    "确保 ai.hotKey.autoWarmup=true，降低击穿风险",
                    "ms.middleware.ai.hotKey.auto-warmup=true"));
        } else if (!context.isRabbitMqHealthy()) {
            plan.setIncidentType("RABBITMQ_UNAVAILABLE");
            plan.setSummary("RabbitMQ 不可用，触发自愈并关注消息堆积");
            actions.add(action(AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY,
                    "主动触发 RabbitMQ 连接自愈"));
            recommendations.add(new AutonomyRecommendation(
                    "检查 MQ 幂等窗口",
                    "故障期间可能重复投递，可适当延长幂等键过期时间（需人工确认）",
                    "ms.middleware.mq.idempotent.expiration-hours"));
        } else if (context.getMqFailedCount() > 0) {
            plan.setIncidentType("MQ_DEGRADED");
            plan.setSummary("MQ 失败计数偏高，建议排查消费端与幂等");
            recommendations.add(new AutonomyRecommendation(
                    "查看失败消息 Trace",
                    "在控制台聊天中提供 messageId 可进一步定位（Phase 5）",
                    null));
        } else {
            plan.setIncidentType("CACHE_DEGRADED");
            plan.setSummary("缓存命中率偏低，建议预热热点");
            if (!context.getHotKeys().isEmpty()) {
                actions.add(action(AutonomyActionType.WARMUP_HOT_KEYS, "命中率低且存在热点，执行预热"));
            }
        }

        plan.setActions(actions);
        plan.setRecommendations(recommendations);
        return plan;
    }

    private PlannedAction action(AutonomyActionType type, String reason) {
        PlannedAction action = new PlannedAction();
        action.setActionType(type);
        action.setRisk(type.getRisk());
        action.setReason(reason);
        return action;
    }
}
