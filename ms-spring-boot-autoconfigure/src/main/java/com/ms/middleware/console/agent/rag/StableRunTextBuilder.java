package com.ms.middleware.console.agent.rag;

import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.run.AutonomyRun;

/**
 * 将 STABLE run 拼成短摘要（索引质量优先于灌全量 timeline）。
 */
public final class StableRunTextBuilder {

    private StableRunTextBuilder() {
    }

    public static String build(AutonomyRun run) {
        if (run == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("历史自治 run 摘要\n");
        sb.append("runId=").append(nullToDash(run.getRunId())).append('\n');
        sb.append("tenant=").append(nullToDash(run.getTenant())).append('\n');
        sb.append("status=").append(run.getStatus() != null ? run.getStatus().name() : "—").append('\n');

        AutonomyPlan plan = run.getPlan();
        String incident = plan != null && plan.getIncidentType() != null ? plan.getIncidentType() : "UNKNOWN";
        sb.append("incident=").append(incident).append('\n');
        if (plan != null && plan.getSummary() != null && !plan.getSummary().isBlank()) {
            sb.append("planSummary=").append(plan.getSummary().trim()).append('\n');
        }

        PlannedAction primary = findPrimaryAction(plan);
        if (primary != null) {
            sb.append("primaryAction=");
            if (primary.getActionType() != null) {
                sb.append(primary.getActionType().name());
            } else {
                sb.append("—");
            }
            if (primary.getReason() != null && !primary.getReason().isBlank()) {
                sb.append(" | ").append(primary.getReason().trim());
            }
            if (primary.getExecutionStatus() != null) {
                sb.append(" | exec=").append(primary.getExecutionStatus());
            }
            sb.append('\n');
        }

        if (run.getRecoveryEvidence() != null && run.getRecoveryEvidence().getSummary() != null) {
            sb.append("recovery=").append(run.getRecoveryEvidence().getSummary().trim()).append('\n');
        }
        run.getMttrSeconds().ifPresent(mttr -> sb.append("mttrSeconds=").append(mttr).append('\n'));
        return sb.toString().trim();
    }

    private static PlannedAction findPrimaryAction(AutonomyPlan plan) {
        if (plan == null || plan.getActions() == null || plan.getActions().isEmpty()) {
            return null;
        }
        for (PlannedAction action : plan.getActions()) {
            if (action != null && action.getRank() == 1) {
                return action;
            }
        }
        return plan.getActions().get(0);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
