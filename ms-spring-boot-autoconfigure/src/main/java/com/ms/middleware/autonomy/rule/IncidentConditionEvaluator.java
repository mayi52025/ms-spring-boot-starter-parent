package com.ms.middleware.autonomy.rule;

import com.ms.middleware.autonomy.context.AutonomyContext;

/**
 * 解析 YAML 规则中的条件表达式（固定词典，避免引入脚本引擎）。
 *
 * <p>incident 识别条件与动作生效条件共用本类。</p>
 */
public final class IncidentConditionEvaluator {

    private IncidentConditionEvaluator() {
    }

    /**
     * 判断 incident 识别条件是否满足。
     *
     * @param condition 如 redis-unhealthy、mq-degraded
     * @param context   当前快照
     */
    public static boolean matchesIncidentCondition(String condition, AutonomyContext context) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        return switch (normalize(condition)) {
            case "redis-unhealthy" -> !context.isRedisHealthy();
            case "rabbitmq-unhealthy" -> !context.isRabbitMqHealthy();
            case "mq-degraded" -> context.isMqDegraded();
            case "cache-degraded" -> context.isCacheDegraded();
            default -> false;
        };
    }

    /**
     * 判断 Runbook 动作生效条件是否满足。
     *
     * @param when    如 always、hot-keys-present
     * @param context 当前快照
     */
    public static boolean matchesActionWhen(String when, AutonomyContext context) {
        if (when == null || when.isBlank() || "always".equalsIgnoreCase(when)) {
            return true;
        }
        return switch (normalize(when)) {
            case "hot-keys-present" -> context.getHotKeys() != null && !context.getHotKeys().isEmpty();
            default -> false;
        };
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase().replace('_', '-');
    }
}
