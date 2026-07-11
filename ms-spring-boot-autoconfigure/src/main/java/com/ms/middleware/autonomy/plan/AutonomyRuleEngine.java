package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 if-else 规则的默认决策引擎（实现 {@link AutonomyDecisionEngine}）。
 *
 * <p>按故障严重程度依次匹配，<strong>先匹配者优先</strong>：</p>
 * <ol>
 *   <li>Redis 不可用 → {@code REDIS_UNAVAILABLE}</li>
 *   <li>RabbitMQ 不可用 → {@code RABBITMQ_UNAVAILABLE}</li>
 *   <li>MQ 消费失败达阈值 → {@code MQ_DEGRADED}（使用 {@link AutonomyContext#isMqDegraded()}，非简单的 count&gt;0）</li>
 *   <li>缓存命中率低 → {@code CACHE_DEGRADED}</li>
 * </ol>
 *
 * <p>每个分支产出 {@link PlannedAction}（可自动执行）与 {@link AutonomyRecommendation}（需人工采纳的配置建议）。
 * 后续可替换为 YAML/EasyRules 实现，编排层无需改动。</p>
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
            // 最高优先级：L2 不可用，组合 L1 降级 + 热点预热 + Redis 自愈
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
            // Rabbit 宕机优先于 MQ 失败计数（组件级故障更严重）
            plan.setIncidentType("RABBITMQ_UNAVAILABLE");
            plan.setSummary("RabbitMQ 不可用，触发自愈并关注消息堆积");
            actions.add(action(AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY,
                    "主动触发 RabbitMQ 连接自愈"));
            recommendations.add(new AutonomyRecommendation(
                    "检查 MQ 幂等窗口",
                    "故障期间可能重复投递，可适当延长幂等键过期时间（需人工确认）",
                    "ms.middleware.mq.idempotent.expiration-hours"));
        } else if (context.isMqDegraded()) {
            // 组件健康但消费失败累计超阈值：与 ContextBuilder issues 使用同一阈值
            plan.setIncidentType("MQ_DEGRADED");
            plan.setSummary(String.format(
                    "MQ 消费失败累计 %d（阈值 %d），建议排查消费端与幂等",
                    context.getMqFailedCount(), context.getMqFailedWarnThreshold()));
            recommendations.add(new AutonomyRecommendation(
                    "查看失败消息 Trace",
                    "在控制台聊天中提供 messageId 可进一步定位",
                    null));
            // 限流等自动动作将在后续排序选优 + 执行器中补齐
        } else if (context.isCacheDegraded()) {
            plan.setIncidentType("CACHE_DEGRADED");
            plan.setSummary(String.format(
                    "缓存命中率 %.1f%% 低于阈值 %.1f%%，建议预热热点",
                    context.getCacheHitRate() * 100, context.getCacheHitRateWarnThreshold() * 100));
            if (!context.getHotKeys().isEmpty()) {
                actions.add(action(AutonomyActionType.WARMUP_HOT_KEYS, "命中率低且存在热点，执行预热"));
            }
        } else {
            // 有 issues 但未命中已知类型时的兜底
            plan.setIncidentType("UNKNOWN");
            plan.setSummary("存在预警但未匹配已知 incident 类型");
        }

        plan.setActions(actions);
        plan.setRecommendations(recommendations);
        return plan;
    }

    /** 构造计划动作，风险等级取自动作类型定义 */
    private PlannedAction action(AutonomyActionType type, String reason) {
        PlannedAction action = new PlannedAction();
        action.setActionType(type);
        action.setRisk(type.getRisk());
        action.setReason(reason);
        return action;
    }
}
