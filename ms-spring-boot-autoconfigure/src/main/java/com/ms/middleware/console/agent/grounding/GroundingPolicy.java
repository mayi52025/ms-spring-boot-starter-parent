package com.ms.middleware.console.agent.grounding;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 意图 → Insight Tool 路由（规则模式与 LLM strict 模式共用）。
 */
@Component
public class GroundingPolicy {

    private static final int DEFAULT_TRACE_LIMIT = 10;
    private static final int DEFAULT_RUN_LIMIT = 5;

    public GroundingResolution resolve(String message, String runId) {
        if (runId != null && !runId.isBlank()) {
            return new GroundingResolution(
                    GroundingIntent.RUN_DETAIL,
                    List.of(InsightToolInvocation.of(InsightToolName.DESCRIBE_RUN, runId.trim())));
        }

        String normalized = message != null ? message.toLowerCase() : "";

        if (containsAny(normalized, "问题", "故障", "issue")) {
            return new GroundingResolution(
                    GroundingIntent.ACTIVE_ISSUES,
                    List.of(InsightToolInvocation.of(InsightToolName.LIST_ACTIVE_ISSUES)));
        }

        if (containsAny(normalized, "最近失败", "失败消息", "failed trace", "failed traces")) {
            return new GroundingResolution(
                    GroundingIntent.RECENT_FAILED_TRACES,
                    List.of(InsightToolInvocation.of(
                            InsightToolName.LIST_RECENT_FAILED_TRACES,
                            String.valueOf(DEFAULT_TRACE_LIMIT))));
        }

        if (containsAny(normalized, "相似", "similar", "检索历史")) {
            String query = extractSimilarQuery(message);
            return new GroundingResolution(
                    GroundingIntent.SIMILAR_RUNS,
                    List.of(InsightToolInvocation.of(
                            InsightToolName.SEARCH_SIMILAR_RUNS,
                            query,
                            String.valueOf(DEFAULT_RUN_LIMIT))));
        }

        if (containsAny(normalized, "最近", "run")) {
            return new GroundingResolution(
                    GroundingIntent.RECENT_RUNS,
                    List.of(InsightToolInvocation.of(
                            InsightToolName.DESCRIBE_RECENT_RUNS,
                            String.valueOf(DEFAULT_RUN_LIMIT))));
        }

        if (containsAny(normalized, "指标", "metric", "mttr", "命中率")) {
            return new GroundingResolution(
                    GroundingIntent.METRICS,
                    List.of(InsightToolInvocation.of(InsightToolName.GET_METRICS_SUMMARY)));
        }

        if (containsAny(normalized, "trace", "消息", "messageid")) {
            String messageId = extractMessageId(message);
            if (messageId != null) {
                return new GroundingResolution(
                        GroundingIntent.TRACE_LOOKUP,
                        List.of(InsightToolInvocation.of(InsightToolName.SEARCH_TRACE, messageId)));
            }
        }

        return new GroundingResolution(GroundingIntent.CHITCHAT, List.of());
    }

    public String ruleModeHelpMessage() {
        return "我是 ms 中间件控制台助手（规则模式）。可问：「当前有什么问题」「最近 run」「最近失败」「指标」「trace <messageId>」「提供 runId 查详情」。";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    static String extractMessageId(String message) {
        if (message == null) {
            return null;
        }
        for (String token : message.split("\\s+")) {
            String cleaned = token.replaceAll("[^a-zA-Z0-9-]", "");
            if (cleaned.length() >= 8) {
                return cleaned;
            }
        }
        return null;
    }

    private static String extractSimilarQuery(String message) {
        if (message == null || message.isBlank()) {
            return "MQ";
        }
        String trimmed = message.trim();
        for (String prefix : List.of("相似", "similar", "检索历史")) {
            int idx = trimmed.toLowerCase().indexOf(prefix.toLowerCase());
            if (idx >= 0) {
                String rest = trimmed.substring(idx + prefix.length()).trim();
                if (!rest.isBlank()) {
                    return rest.replaceAll("^[：:\\s]+", "").trim();
                }
            }
        }
        return trimmed;
    }
}
