package com.ms.middleware.autonomy.insight;

/**
 * 中间件指标快照，供控制台与后续 LangChain4j Tool 使用。
 */
public class MiddlewareMetricsSnapshot {

    private double cacheHitRate;
    private long mqFailedCount;
    private long globalFailureCount;
    private long activeRunCount;
    /** 最近一次 STABLE run 的 MTTR（秒） */
    private long lastMttrSeconds;
    /** 累计完成（STABLE）的自治 run 数 */
    private long completedAutonomyRuns;

    public double getCacheHitRate() {
        return cacheHitRate;
    }

    public void setCacheHitRate(double cacheHitRate) {
        this.cacheHitRate = cacheHitRate;
    }

    public long getMqFailedCount() {
        return mqFailedCount;
    }

    public void setMqFailedCount(long mqFailedCount) {
        this.mqFailedCount = mqFailedCount;
    }

    public long getGlobalFailureCount() {
        return globalFailureCount;
    }

    public void setGlobalFailureCount(long globalFailureCount) {
        this.globalFailureCount = globalFailureCount;
    }

    public long getActiveRunCount() {
        return activeRunCount;
    }

    public void setActiveRunCount(long activeRunCount) {
        this.activeRunCount = activeRunCount;
    }

    public long getLastMttrSeconds() {
        return lastMttrSeconds;
    }

    public void setLastMttrSeconds(long lastMttrSeconds) {
        this.lastMttrSeconds = lastMttrSeconds;
    }

    public long getCompletedAutonomyRuns() {
        return completedAutonomyRuns;
    }

    public void setCompletedAutonomyRuns(long completedAutonomyRuns) {
        this.completedAutonomyRuns = completedAutonomyRuns;
    }
}
