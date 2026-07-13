package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;

/**
 * 证据强度评估器：回答「对当前故障判断有多确定」，供 Policy 门控使用。
 *
 * <p>与选优排序分离——排序由 Runbook 规则决定，confidence 只反映现场信号强弱，
 * 避免用浮点打分冒充智能。</p>
 */
public final class EvidenceStrengthEvaluator {

    private EvidenceStrengthEvaluator() {
    }

    /**
     * 评估某动作在当前上下文下的证据强度（0～1）。
     *
     * @param actionType 候选动作
     * @param context    当前中间件快照
     * @return 越高表示越确信「该动作适用于当前故障」
     */
    public static double evaluate(AutonomyActionType actionType, AutonomyContext context) {
        return switch (actionType) {
            case TRIGGER_REDIS_RECOVERY -> context.isRedisHealthy() ? 0.50 : 0.95;
            case TRIGGER_RABBITMQ_RECOVERY -> context.isRabbitMqHealthy() ? 0.50 : 0.90;
            case ENSURE_L1_DEGRADE -> context.isRedisHealthy() ? 0.50 : 0.88;
            case THROTTLE_CONSUMER -> mqThrottleEvidence(context);
            case DELAYED_RETRY_BATCH -> Math.min(0.55, mqThrottleEvidence(context) * 0.75);
            case WARMUP_HOT_KEYS -> warmupEvidence(context);
        };
    }

    /**
     * MQ 限流证据：刚踩线即可 LOW 档自动止血，明显超标时证据更高。
     */
    private static double mqThrottleEvidence(AutonomyContext context) {
        if (!context.isMqDegraded()) {
            return 0.40;
        }
        long threshold = Math.max(1, context.getMqFailedWarnThreshold());
        long count = context.getMqFailedCount();
        if (count >= threshold * 2) {
            return 0.92;
        }
        if (count >= threshold + threshold / 2) {
            return 0.82;
        }
        if (count > threshold) {
            return 0.78;
        }
        // 已达阈值：信号足够触发 LOW 风险自动止血（Policy 低档门槛默认 0.55）
        return 0.65;
    }

    /** 预热证据：需有热点；缓存命中率越低信号越强 */
    private static double warmupEvidence(AutonomyContext context) {
        if (context.getHotKeys().isEmpty()) {
            return 0.40;
        }
        if (context.isCacheDegraded()) {
            double gap = context.getCacheHitRateWarnThreshold() - context.getCacheHitRate();
            return gap > 0.2 ? 0.82 : 0.74;
        }
        return 0.76;
    }
}
