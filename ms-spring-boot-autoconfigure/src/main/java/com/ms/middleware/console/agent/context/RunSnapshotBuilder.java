package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.recovery.RecoveryEvidence;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.TimelineEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 从 Ledger 构建 run 快照（timeline 尾部 + recovery，不含 Tool 层重复的全文 describeRun）。
 */
@Component
public class RunSnapshotBuilder {

    private final MiddlewareInsightService insightService;

    public RunSnapshotBuilder(MiddlewareInsightService insightService) {
        this.insightService = insightService;
    }

    public Optional<RunContextSnapshot> build(String runId, int timelineLimit) {
        if (runId == null || runId.isBlank()) {
            return Optional.empty();
        }
        return insightService.getRun(runId.trim()).map(run -> toSnapshot(run, timelineLimit));
    }

    RunContextSnapshot toSnapshot(AutonomyRun run, int timelineLimit) {
        List<String> timelineLines = formatTimelineTail(run, timelineLimit);
        String incident = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
        Long mttr = run.getMttrSeconds().isPresent() ? run.getMttrSeconds().getAsLong() : null;
        String recovery = formatRecovery(run.getRecoveryEvidence());
        boolean wartime = AgentOrchestrationPolicy.isWartime(Optional.of(run));
        return new RunContextSnapshot(
                run.getRunId(),
                run.getStatus(),
                run.getTenant(),
                incident,
                run.getIssues() != null ? List.copyOf(run.getIssues()) : List.of(),
                mttr,
                timelineLines,
                recovery,
                wartime);
    }

    private static List<String> formatTimelineTail(AutonomyRun run, int limit) {
        List<TimelineEvent> timeline = run.getTimeline();
        if (timeline == null || timeline.isEmpty()) {
            return List.of();
        }
        int safe = Math.max(1, limit);
        int from = Math.max(0, timeline.size() - safe);
        List<String> lines = new ArrayList<>();
        for (int i = from; i < timeline.size(); i++) {
            TimelineEvent event = timeline.get(i);
            lines.add(String.format("· %s %s — %s",
                    event.getAt() != null ? event.getAt().toString() : "-",
                    event.getPhase() != null ? event.getPhase() : "?",
                    event.getMessage() != null ? event.getMessage() : ""));
        }
        return lines;
    }

    private static String formatRecovery(RecoveryEvidence evidence) {
        if (evidence == null) {
            return null;
        }
        if (evidence.getSummary() != null && !evidence.getSummary().isBlank()) {
            return evidence.getSummary().trim();
        }
        return null;
    }
}
