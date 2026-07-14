package com.ms.middleware.autonomy.run;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.AutonomyRecommendation;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.recovery.RecoveryEvidence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * 一次自治运行的完整记录（对应控制台里的一条「故障事件」）。
 *
 * <p>生命周期：DETECTED → PLANNED → EXECUTING → STABLE（或 ESCALATED / CLOSED）。
 * 时间线 {@link #timeline} 与 SSE 推送一一对应，phase 见 {@link AutonomyTimelinePhase}。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutonomyRun {

    /** 账本 JSON schema 版本，便于后续字段演进 */
    private int schemaVersion = AutonomyRunSerde.CURRENT_SCHEMA_VERSION;
    /**
     * 反序列化降级标记：true 表示 Redis 中 JSON 不完整，仅展示 stub 摘要。
     * 不写回 Redis（NON_DEFAULT）。
     */
    private boolean ledgerCorrupted;

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
    /** 故障首次检测时的上下文基线，用于 STABLE recoveryEvidence 前后对比 */
    private AutonomyContext incidentBaseline;
    /** STABLE 时写入的恢复证据，供 API/控制台展示 */
    private RecoveryEvidence recoveryEvidence;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean isLedgerCorrupted() {
        return ledgerCorrupted;
    }

    public void setLedgerCorrupted(boolean ledgerCorrupted) {
        this.ledgerCorrupted = ledgerCorrupted;
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

    public AutonomyContext getIncidentBaseline() {
        return incidentBaseline;
    }

    public void setIncidentBaseline(AutonomyContext incidentBaseline) {
        this.incidentBaseline = incidentBaseline;
    }

    public RecoveryEvidence getRecoveryEvidence() {
        return recoveryEvidence;
    }

    public void setRecoveryEvidence(RecoveryEvidence recoveryEvidence) {
        this.recoveryEvidence = recoveryEvidence;
    }

    @JsonProperty("issues")
    public List<String> getIssueList() {
        return context != null ? context.getIssues() : List.of();
    }

    @JsonIgnore
    public List<String> getIssues() {
        return getIssueList();
    }

    @JsonIgnore
    public List<PlannedAction> getActions() {
        return plan != null ? plan.getActions() : List.of();
    }

    @JsonIgnore
    public List<AutonomyRecommendation> getRecommendations() {
        return plan != null ? plan.getRecommendations() : List.of();
    }

    /**
     * 恢复耗时（秒）；未稳定时返回 empty。
     */
    @JsonIgnore
    public OptionalLong getMttrSeconds() {
        if (stabilizedAt == null || startedAt == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0, stabilizedAt.getEpochSecond() - startedAt.getEpochSecond()));
    }

    /** REST / SSE 使用的 MTTR 字段（避免 OptionalLong 序列化失败） */
    @JsonProperty("mttrSeconds")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getMttrSecondsForJson() {
        return getMttrSeconds().isPresent() ? getMttrSeconds().getAsLong() : null;
    }
}
