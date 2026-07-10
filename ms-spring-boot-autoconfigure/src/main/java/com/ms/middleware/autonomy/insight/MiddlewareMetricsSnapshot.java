package com.ms.middleware.autonomy.insight;

/**
 * 中间件指标快照，供控制台与后续 LangChain4j Tool 使用。
 */
public class MiddlewareMetricsSnapshot {

    private double cacheHitRate;
    private long mqFailedCount;
    private long globalFailureCount;
    private long activeRunCount;

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
}
