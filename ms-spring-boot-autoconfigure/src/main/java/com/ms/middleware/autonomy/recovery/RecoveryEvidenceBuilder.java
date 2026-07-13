package com.ms.middleware.autonomy.recovery;

import com.ms.middleware.autonomy.context.AutonomyContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 根据 incident 类型对比故障基线与 STABLE 快照，生成 {@link RecoveryEvidence}。
 */
public final class RecoveryEvidenceBuilder {

    private RecoveryEvidenceBuilder() {
    }

    /**
     * 构建恢复证据；baseline 为空时退化为使用 after 作为前后（摘要仍可读）。
     */
    public static RecoveryEvidence build(String incidentType, AutonomyContext baseline, AutonomyContext after) {
        AutonomyContext before = baseline != null ? baseline : after;
        AutonomyContext stable = after != null ? after : before;
        String type = incidentType != null && !incidentType.isBlank() ? incidentType : "UNKNOWN";

        RecoveryEvidence evidence = new RecoveryEvidence();
        evidence.setIncidentType(type);

        return switch (type) {
            case "MQ_DEGRADED" -> buildMqDegraded(evidence, before, stable);
            case "REDIS_UNAVAILABLE" -> buildRedis(evidence, before, stable);
            case "RABBITMQ_UNAVAILABLE" -> buildRabbit(evidence, before, stable);
            case "CACHE_DEGRADED" -> buildCache(evidence, before, stable);
            default -> buildGeneric(evidence, before, stable);
        };
    }

    /** STABLE 时间线文案：MTTR + 恢复摘要 */
    public static String formatStableMessage(RecoveryEvidence evidence, long mttrSeconds) {
        if (evidence == null || evidence.getSummary() == null || evidence.getSummary().isBlank()) {
            return String.format(Locale.ROOT, "中间件指标恢复正常，MTTR=%ds，本次自治结束", mttrSeconds);
        }
        return String.format(Locale.ROOT,
                "中间件指标恢复正常，MTTR=%ds | %s，本次自治结束",
                mttrSeconds, evidence.getSummary());
    }

    public static String formatStableMessageWithoutMttr(RecoveryEvidence evidence) {
        if (evidence == null || evidence.getSummary() == null || evidence.getSummary().isBlank()) {
            return "中间件指标恢复正常，本次自治结束";
        }
        return "中间件指标恢复正常 | " + evidence.getSummary() + "，本次自治结束";
    }

    private static RecoveryEvidence buildMqDegraded(RecoveryEvidence evidence,
                                                    AutonomyContext before,
                                                    AutonomyContext after) {
        long threshold = after.getMqFailedWarnThreshold();
        long beforeCount = before.getMqFailedCount();
        long afterCount = after.getMqFailedCount();
        evidence.setResolutionRule("mqFailedCount < mqFailedWarnThreshold");
        evidence.setSummary(String.format(Locale.ROOT,
                "MQ窗口失败 %d→%d（阈值<%d）", beforeCount, afterCount, threshold));
        evidence.setMetrics(List.of(new RecoveryMetricDelta(
                "mqFailedCount",
                "MQ窗口失败次数",
                String.valueOf(beforeCount),
                String.valueOf(afterCount),
                "<" + threshold)));
        return evidence;
    }

    private static RecoveryEvidence buildRedis(RecoveryEvidence evidence,
                                                 AutonomyContext before,
                                                 AutonomyContext after) {
        evidence.setResolutionRule("redisHealthy == true");
        evidence.setSummary(formatHealthTransition("Redis", before.isRedisHealthy(), after.isRedisHealthy()));
        evidence.setMetrics(List.of(new RecoveryMetricDelta(
                "redisHealthy",
                "Redis 可用性",
                healthLabel(before.isRedisHealthy()),
                healthLabel(after.isRedisHealthy()),
                "可用")));
        return evidence;
    }

    private static RecoveryEvidence buildRabbit(RecoveryEvidence evidence,
                                                  AutonomyContext before,
                                                  AutonomyContext after) {
        evidence.setResolutionRule("rabbitMqHealthy == true");
        evidence.setSummary(formatHealthTransition("RabbitMQ", before.isRabbitMqHealthy(), after.isRabbitMqHealthy()));
        evidence.setMetrics(List.of(new RecoveryMetricDelta(
                "rabbitMqHealthy",
                "RabbitMQ 可用性",
                healthLabel(before.isRabbitMqHealthy()),
                healthLabel(after.isRabbitMqHealthy()),
                "可用")));
        return evidence;
    }

    private static RecoveryEvidence buildCache(RecoveryEvidence evidence,
                                                 AutonomyContext before,
                                                 AutonomyContext after) {
        double threshold = after.getCacheHitRateWarnThreshold();
        double beforeRate = before.getCacheHitRate();
        double afterRate = after.getCacheHitRate();
        evidence.setResolutionRule("cacheHitRate >= cacheHitRateWarnThreshold");
        evidence.setSummary(String.format(Locale.ROOT,
                "缓存命中率 %.1f%%→%.1f%%（阈值≥%.0f%%）",
                beforeRate * 100, afterRate * 100, threshold * 100));
        evidence.setMetrics(List.of(new RecoveryMetricDelta(
                "cacheHitRate",
                "缓存命中率",
                formatPercent(beforeRate),
                formatPercent(afterRate),
                "≥" + formatPercent(threshold))));
        return evidence;
    }

    private static RecoveryEvidence buildGeneric(RecoveryEvidence evidence,
                                                   AutonomyContext before,
                                                   AutonomyContext after) {
        evidence.setResolutionRule("!hasIncident()");
        boolean hadIssues = before.hasIncident();
        boolean stillIssues = after.hasIncident();
        evidence.setSummary(hadIssues && !stillIssues
                ? "活跃 issues 已清空"
                : "中间件上下文已更新");
        List<RecoveryMetricDelta> metrics = new ArrayList<>();
        metrics.add(new RecoveryMetricDelta(
                "issueCount",
                "活跃问题数",
                String.valueOf(before.getIssues().size()),
                String.valueOf(after.getIssues().size()),
                "0"));
        evidence.setMetrics(metrics);
        return evidence;
    }

    private static String formatHealthTransition(String name, boolean before, boolean after) {
        return name + " " + healthLabel(before) + "→" + healthLabel(after);
    }

    private static String healthLabel(boolean healthy) {
        return healthy ? "可用" : "不可用";
    }

    private static String formatPercent(double rate) {
        return String.format(Locale.ROOT, "%.1f%%", rate * 100);
    }
}
