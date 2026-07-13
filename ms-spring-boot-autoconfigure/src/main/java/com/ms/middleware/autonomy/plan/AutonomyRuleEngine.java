package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 if-else 规则的默认决策引擎（实现 {@link AutonomyDecisionEngine}）。
 *
 * <p>流程：识别 incident → {@link IncidentActionCatalog} 产出 Runbook 候选池
 * → {@link ActionSelector} 规则选优 → 写入 {@link AutonomyPlan}。</p>
 *
 * <p>incident 匹配优先级（先匹配者优先）：</p>
 * <ol>
 *   <li>Redis 不可用 → {@code REDIS_UNAVAILABLE}</li>
 *   <li>RabbitMQ 不可用 → {@code RABBITMQ_UNAVAILABLE}</li>
 *   <li>MQ 消费失败达阈值 → {@code MQ_DEGRADED}</li>
 *   <li>缓存命中率低 → {@code CACHE_DEGRADED}</li>
 * </ol>
 */
public class AutonomyRuleEngine implements AutonomyDecisionEngine {

    private final ActionSelector actionSelector;

    /** 无参构造供测试；生产环境由自动配置注入 ActionSelector */
    public AutonomyRuleEngine() {
        this(new ActionSelector());
    }

    public AutonomyRuleEngine(ActionSelector actionSelector) {
        this.actionSelector = actionSelector;
    }

    @Override
    public AutonomyPlan plan(AutonomyContext context) {
        AutonomyPlan plan = new AutonomyPlan();
        plan.setContext(context);

        if (!context.hasIncident()) {
            plan.setIncidentType("NONE");
            plan.setSummary("中间件状态正常");
            return plan;
        }

        String incidentType = resolveIncidentType(context);
        plan.setIncidentType(incidentType);
        plan.setSummary(buildSummary(incidentType, context));

        List<AutonomyRecommendation> recommendations = buildRecommendations(incidentType);
        List<ActionCandidate> candidates = IncidentActionCatalog.candidatesFor(incidentType, context);
        List<PlannedAction> selectedActions = actionSelector.select(candidates, context);

        plan.setActions(selectedActions);
        plan.setRankingSummary(actionSelector.buildSelectionSummary(selectedActions));
        plan.setRecommendations(recommendations);
        return plan;
    }

    /** 按优先级链确定主 incident 类型 */
    private String resolveIncidentType(AutonomyContext context) {
        if (!context.isRedisHealthy()) {
            return "REDIS_UNAVAILABLE";
        }
        if (!context.isRabbitMqHealthy()) {
            return "RABBITMQ_UNAVAILABLE";
        }
        if (context.isMqDegraded()) {
            return "MQ_DEGRADED";
        }
        if (context.isCacheDegraded()) {
            return "CACHE_DEGRADED";
        }
        return "UNKNOWN";
    }

    private String buildSummary(String incidentType, AutonomyContext context) {
        return switch (incidentType) {
            case "REDIS_UNAVAILABLE" -> "Redis 不可用，启用本地缓存与自愈组合处置";
            case "RABBITMQ_UNAVAILABLE" -> "RabbitMQ 不可用，触发自愈并关注消息堆积";
            case "MQ_DEGRADED" -> String.format(
                    "MQ 消费失败累计 %d（阈值 %d），建议排查消费端与幂等",
                    context.getMqFailedCount(), context.getMqFailedWarnThreshold());
            case "CACHE_DEGRADED" -> String.format(
                    "缓存命中率 %.1f%% 低于阈值 %.1f%%，建议预热热点",
                    context.getCacheHitRate() * 100, context.getCacheHitRateWarnThreshold() * 100);
            default -> "存在预警但未匹配已知 incident 类型";
        };
    }

    /** 配置级推荐（不参与动作选优，由编排器写入 RECOMMEND 时间线） */
    private List<AutonomyRecommendation> buildRecommendations(String incidentType) {
        List<AutonomyRecommendation> recommendations = new ArrayList<>();
        switch (incidentType) {
            case "REDIS_UNAVAILABLE" -> {
                recommendations.add(new AutonomyRecommendation(
                        "缩短本地缓存 TTL 差",
                        "可将 distributed.ttl 与 local.ttl 比例调至 1:8，减少 Redis 恢复后陈旧数据窗口",
                        "ms.middleware.cache.distributed.ttl / local.ttl"));
                recommendations.add(new AutonomyRecommendation(
                        "开启热点自动预热",
                        "确保 ai.hotKey.autoWarmup=true，降低击穿风险",
                        "ms.middleware.ai.hotKey.auto-warmup=true"));
            }
            case "RABBITMQ_UNAVAILABLE" -> recommendations.add(new AutonomyRecommendation(
                    "检查 MQ 幂等窗口",
                    "故障期间可能重复投递，可适当延长幂等键过期时间（需人工确认）",
                    "ms.middleware.mq.idempotent.expiration-hours"));
            case "MQ_DEGRADED" -> recommendations.add(new AutonomyRecommendation(
                    "查看失败消息 Trace",
                    "在控制台聊天中提供 messageId 可进一步定位",
                    null));
            default -> {
            }
        }
        return recommendations;
    }
}
