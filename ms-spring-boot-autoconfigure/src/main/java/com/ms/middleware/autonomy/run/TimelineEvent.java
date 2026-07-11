package com.ms.middleware.autonomy.run;

import java.time.Instant;

/**
 * 时间线单条事件，JSON 序列化后通过 SSE {@code event=timeline} 推送到浏览器。
 *
 * <p>phase 标准取值见 {@link AutonomyTimelinePhase}；兼容旧值 {@code ACTION}（等同 AUTO）。</p>
 */
public class TimelineEvent {

    private Instant at = Instant.now();
    private String runId;
    private String phase;
    private String message;
    private String level = "INFO";
    /** Step 4 采纳时关联 {@link com.ms.middleware.autonomy.plan.AutonomyRecommendation#getRecommendationId()} */
    private String recommendationId;

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
}
