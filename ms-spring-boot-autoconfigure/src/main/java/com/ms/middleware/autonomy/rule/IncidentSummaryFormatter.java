package com.ms.middleware.autonomy.rule;

import com.ms.middleware.autonomy.context.AutonomyContext;

/**
 * 将 YAML 中的 PLAN 摘要模板替换为运行时数值。
 */
public final class IncidentSummaryFormatter {

    private IncidentSummaryFormatter() {
    }

    /**
     * 替换模板占位符；未知占位符原样保留。
     */
    public static String format(String template, AutonomyContext context) {
        if (template == null || template.isBlank()) {
            return "";
        }
        String result = template;
        result = result.replace("{mqFailedCount}", String.valueOf(context.getMqFailedCount()));
        result = result.replace("{mqFailedWarnThreshold}", String.valueOf(context.getMqFailedWarnThreshold()));
        result = result.replace("{cacheHitRatePercent}",
                String.format("%.1f", context.getCacheHitRate() * 100));
        result = result.replace("{cacheHitRateWarnThresholdPercent}",
                String.format("%.1f", context.getCacheHitRateWarnThreshold() * 100));
        return result;
    }
}
