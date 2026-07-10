package com.ms.middleware.autonomy.run;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.AutonomyRecommendation;
import com.ms.middleware.autonomy.plan.PlannedAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * 一次自治运行的完整记录（对应控制台里的一条「故障事件」）。
 *
 * <p>生命周期：DETECTED → PLANNED → EXECUTING → STABLE（或 ESCALATED / CLOSED）。
 * 时间线 {@link #timeline} 与 SSE 推送一一对应，phase 常见值：DETECT / PLAN / ACTION / ADVISE / RECOMMEND / STABLE。</p>
 */
public class AutonomyRun {

    private String runId;
    /** 租户标识，默认 spring.application.name，多实例部署时区分 run */
    private String tenant;
    private AutonomyRunStatus status = AutonomyRunStatus.DETECTED;
    private Instant startedAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant stabilizedAt;
    private AutonomyContext context;
    private AutonomyPlan plan;
    private List<TimelineEvent> timeline = new ArrayList<>();

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public AutonomyRunStatus getStatus() {
        return status;
    }

    public void setStatus(AutonomyRunStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getStabilizedAt() {
        return stabilizedAt;
    }

    public void setStabilizedAt(Instant stabilizedAt) {
        this.stabilizedAt = stabilizedAt;
    }

    public AutonomyContext getContext() {
        return context;
    }

    public void setContext(AutonomyContext context) {
        this.context = context;
    }

    public AutonomyPlan getPlan() {
        return plan;
    }

    public void setPlan(AutonomyPlan plan) {
        this.plan = plan;
    }

    public List<TimelineEvent> getTimeline() {
        return timeline;
    }

    public void addTimeline(TimelineEvent event) {
        timeline.add(event);
    }

    public List<String> getIssues() {
        return context != null ? context.getIssues() : List.of();
    }

    public List<PlannedAction> getActions() {
        return plan != null ? plan.getActions() : List.of();
    }

    public List<AutonomyRecommendation> getRecommendations() {
        return plan != null ? plan.getRecommendations() : List.of();
    }

    /**
     * 恢复耗时（秒）；未稳定时返回 empty。
     */
    public OptionalLong getMttrSeconds() {
        if (stabilizedAt == null || startedAt == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0, stabilizedAt.getEpochSecond() - startedAt.getEpochSecond()));
    }
}
