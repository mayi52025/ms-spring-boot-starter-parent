package com.ms.middleware.console.agent.grounding;

import java.time.Instant;

/**
 * 单次 Insight Tool 调用审计记录。
 */
public record ToolCallRecord(
        InsightToolName tool,
        String summary,
        long durationMs,
        Instant at) {
}
