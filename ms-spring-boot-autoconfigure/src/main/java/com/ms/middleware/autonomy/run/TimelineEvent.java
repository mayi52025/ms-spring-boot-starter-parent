package com.ms.middleware.autonomy.run;

import java.time.Instant;

/**
 * 自治 run 时间线上的单条事件。
 *
 * <p>持久化在 {@link AutonomyRun#getTimeline()}，并通过 SSE {@code event=timeline} 实时推到控制台。
 * phase 标准取值见 {@link AutonomyTimelinePhase}。</p>
 */
public class TimelineEvent {

    /** 事件发生时间 */
    private Instant at = Instant.now();
    /** 所属 runId */
    private String runId;
    /**
     * 事件阶段，见 {@link AutonomyTimelinePhase}。
     * 兼容历史值 ACTION（等同 AUTO）。
     */
    private String phase;
    /** 展示给运维的说明文字 */
    private String message;
    /** 日志级别：INFO / WARN / ERROR */
    private String level = "INFO";
    /**
     * 当 phase 为 ACCEPTED 时，关联被采纳的 {@link com.ms.middleware.autonomy.plan.AutonomyRecommendation#getRecommendationId()}。
     */
    private String recommendationId;
    /** 人工操作人（采纳/发布 API 写入，供审计） */
    private String operator;
    /** 请求来源 IP（控制台 API 写入，供审计） */
    private String clientIp;

    public TimelineEvent() {
    }

    public TimelineEvent(String runId, String phase, String message) {
        this.runId = runId;
        this.phase = phase;
        this.message = message;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
}
