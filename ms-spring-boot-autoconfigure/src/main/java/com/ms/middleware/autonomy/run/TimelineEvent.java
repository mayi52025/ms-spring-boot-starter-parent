package com.ms.middleware.autonomy.run;

import java.time.Instant;

/**
 * 时间线事件（推送到 AI 控制台）
 */
public class TimelineEvent {

    private Instant at = Instant.now();
    private String runId;
    private String phase;
    private String message;
    private String level = "INFO";

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
}
