package com.ms.middleware.autonomy.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次巡检时的中间件状况快照
 */
public class AutonomyContext {

    private Instant capturedAt = Instant.now();
    private boolean redisHealthy = true;
    private boolean rabbitMqHealthy = true;
    private double cacheHitRate = 1.0;
    private long mqFailedCount;
    private long globalFailureCount;
    private List<String> hotKeys = new ArrayList<>();
    private List<String> issues = new ArrayList<>();

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public boolean isRedisHealthy() {
        return redisHealthy;
    }

    public void setRedisHealthy(boolean redisHealthy) {
        this.redisHealthy = redisHealthy;
    }

    public boolean isRabbitMqHealthy() {
        return rabbitMqHealthy;
    }

    public void setRabbitMqHealthy(boolean rabbitMqHealthy) {
        this.rabbitMqHealthy = rabbitMqHealthy;
    }

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

    public List<String> getHotKeys() {
        return hotKeys;
    }

    public void setHotKeys(List<String> hotKeys) {
        this.hotKeys = hotKeys != null ? hotKeys : new ArrayList<>();
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
    }

    public boolean hasIncident() {
        return !issues.isEmpty();
    }
}
