package com.ms.middleware.autonomy.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自治引擎 Micrometer 指标。
 *
 * <ul>
 *   <li>Phase 2：run.total、mttr、last_mttr_seconds、completed_runs</li>
 *   <li>Phase 3 Step 6：action.auto.total、recommendation.total、recommendation.accepted.total、plan.confidence</li>
 * </ul>
 */
public class AutonomyMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicLong lastMttrSeconds = new AtomicLong(0);
    private final AtomicLong completedRunCount = new AtomicLong(0);

    public AutonomyMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge("ms.autonomy.last_mttr_seconds", lastMttrSeconds, AtomicLong::get);
        meterRegistry.gauge("ms.autonomy.completed_runs", completedRunCount, AtomicLong::get);
    }

    /**
     * run 进入 STABLE 时调用。
     */
    public void recordRunStabilized(String tenant, String incidentType, long mttrSeconds) {
        String safeTenant = safeTenant(tenant);
        String safeIncident = safeIncident(incidentType);

        meterRegistry.counter("ms.autonomy.run.total",
                        "tenant", safeTenant,
                        "incident_type", safeIncident)
                .increment();

        meterRegistry.timer("ms.autonomy.mttr",
                        "tenant", safeTenant,
                        "incident_type", safeIncident)
                .record(mttrSeconds, TimeUnit.SECONDS);

        lastMttrSeconds.set(mttrSeconds);
        completedRunCount.incrementAndGet();
    }

    /**
     * 自动或人工触发执行 rank#1 动作时调用。
     *
     * @param trigger orchestrator 自动执行填 {@code auto}，人工采纳备选填 {@code human}
     */
    public void recordActionAuto(String tenant, String incidentType, String actionType, String trigger) {
        meterRegistry.counter("ms.autonomy.action.auto.total",
                        "tenant", safeTenant(tenant),
                        "incident_type", safeIncident(incidentType),
                        "action_type", safeTag(actionType),
                        "trigger", safeTag(trigger))
                .increment();
    }

    /** 写入 RECOMMEND 时间线时调用（每条配置推荐计一次） */
    public void recordRecommendation(String tenant, String incidentType) {
        meterRegistry.counter("ms.autonomy.recommendation.total",
                        "tenant", safeTenant(tenant),
                        "incident_type", safeIncident(incidentType))
                .increment();
    }

    /** 人工采纳配置推荐时调用 */
    public void recordRecommendationAccepted(String tenant, String incidentType) {
        meterRegistry.counter("ms.autonomy.recommendation.accepted.total",
                        "tenant", safeTenant(tenant),
                        "incident_type", safeIncident(incidentType))
                .increment();
    }

    /** 人工拒绝配置推荐时调用 */
    public void recordRecommendationRejected(String tenant, String incidentType) {
        meterRegistry.counter("ms.autonomy.recommendation.rejected.total",
                        "tenant", safeTenant(tenant),
                        "incident_type", safeIncident(incidentType))
                .increment();
    }

    /** Redis 账本反序列化失败或 stub 降级时调用 */
    public void recordLedgerDeserializeError(String tenant) {
        meterRegistry.counter("ms.autonomy.ledger.deserialize.errors.total",
                        "tenant", safeTenant(tenant))
                .increment();
    }

    /** 当前实例获得 tick 分布式锁并执行 doTick */
    public void recordTickLeader(String tenant) {
        meterRegistry.counter("ms.autonomy.tick.leader.total",
                        "tenant", safeTenant(tenant))
                .increment();
    }

    /** 未获 tick 锁跳过本轮 */
    public void recordTickLockSkipped(String tenant) {
        meterRegistry.counter("ms.autonomy.tick.lock.skipped.total",
                        "tenant", safeTenant(tenant))
                .increment();
    }

    /**
     * PLAN 完成后记录 rank#1 证据强度，供 SLO 与门控效果观测。
     */
    public void recordPlanConfidence(String tenant, String incidentType, double confidence) {
        meterRegistry.summary("ms.autonomy.plan.confidence",
                        "tenant", safeTenant(tenant),
                        "incident_type", safeIncident(incidentType))
                .record(confidence);
    }

    public long getLastMttrSeconds() {
        return lastMttrSeconds.get();
    }

    public long getCompletedRunCount() {
        return completedRunCount.get();
    }

    private static String safeTenant(String tenant) {
        return tenant != null ? tenant : "unknown";
    }

    private static String safeIncident(String incidentType) {
        return incidentType != null && !incidentType.isBlank() ? incidentType : "UNKNOWN";
    }

    private static String safeTag(String value) {
        return value != null && !value.isBlank() ? value : "unknown";
    }
}
