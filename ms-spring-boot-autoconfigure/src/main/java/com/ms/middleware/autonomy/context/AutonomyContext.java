package com.ms.middleware.autonomy.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次自治巡检时的「中间件状况快照」。
 *
 * <p>由 {@link AutonomyContextBuilder} 每次 tick 重新构建，不缓存。
 * 决策引擎、STABLE 判定、控制台展示都依赖本对象，避免各处阈值不一致。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutonomyContext {

    /** 快照采集时刻 */
    private Instant capturedAt = Instant.now();
    /** Redis 探活结果（来自 FaultSelfHealing） */
    private boolean redisHealthy = true;
    /** RabbitMQ 探活结果 */
    private boolean rabbitMqHealthy = true;
    /** 当前缓存命中率 0～1 */
    private double cacheHitRate = 1.0;
    /** MQ 消费失败累计次数（来自 MsMetrics） */
    private long mqFailedCount;
    /** 全局中间件失败计数 */
    private long globalFailureCount;
    /**
     * MQ 失败预警阈值，与配置项 {@code ms.middleware.autonomy.mq-failed-warn-threshold} 一致。
     * 写入上下文后，issues 生成、规则引擎、STABLE 结案共用同一标准。
     */
    private long mqFailedWarnThreshold = 10;
    /**
     * 缓存命中率预警阈值，与 {@code ms.middleware.autonomy.cache-hit-rate-warn-threshold} 一致。
     */
    private double cacheHitRateWarnThreshold = 0.5;
    /** 当前热点 Key 列表（可选，来自 HotKeyManager） */
    private List<String> hotKeys = new ArrayList<>();
    /** 人类可读的问题描述，供控制台「当前问题」展示；非空即 {@link #hasIncident()} */
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

    public long getMqFailedWarnThreshold() {
        return mqFailedWarnThreshold;
    }

    public void setMqFailedWarnThreshold(long mqFailedWarnThreshold) {
        this.mqFailedWarnThreshold = mqFailedWarnThreshold;
    }

    public double getCacheHitRateWarnThreshold() {
        return cacheHitRateWarnThreshold;
    }

    public void setCacheHitRateWarnThreshold(double cacheHitRateWarnThreshold) {
        this.cacheHitRateWarnThreshold = cacheHitRateWarnThreshold;
    }

    /**
     * 是否达到 MQ 消费失败预警线。
     * 与 issues 里「MQ 消费失败累计偏高」、规则引擎 {@code MQ_DEGRADED}、STABLE 判定使用同一公式。
     * 仅计算字段，不参与 JSON 持久化（旧账本可能含 mqDegraded 字段，由 ignoreUnknown 忽略）。
     */
    @JsonIgnore
    public boolean isMqDegraded() {
        return mqFailedCount >= mqFailedWarnThreshold;
    }

    /**
     * 是否达到缓存命中率预警线。
     * 仅计算字段，不参与 JSON 持久化。
     */
    @JsonIgnore
    public boolean isCacheDegraded() {
        return cacheHitRate < cacheHitRateWarnThreshold;
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

    /** 存在需自治处理的异常或预警（issues 非空） */
    public boolean hasIncident() {
        return !issues.isEmpty();
    }
}
