package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.AutonomyRunStatus;

import java.util.List;

/**
 * run 结构化快照（供 L1 工作上下文，避免重复 describeRun 全文）。
 */
public record RunContextSnapshot(
        String runId,
        AutonomyRunStatus status,
        String tenant,
        String incidentType,
        List<String> issues,
        Long mttrSeconds,
        List<String> timelineLines,
        String recoverySummary,
        boolean wartime) {
}
