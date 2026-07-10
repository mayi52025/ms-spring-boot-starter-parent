package com.ms.middleware.autonomy.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自治引擎 Micrometer 指标（Phase 2）。
 *
 * <ul>
 *   <li>{@code ms.autonomy.run.total} — 完成并 STABLE 的 run 计数</li>
 *   <li>{@code ms.autonomy.mttr} — 每次恢复的耗时分布（秒）</li>
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
        String safeTenant = tenant != null ? tenant : "unknown";
        String safeIncident = incidentType != null && !incidentType.isBlank() ? incidentType : "UNKNOWN";

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

    public long getLastMttrSeconds() {
        return lastMttrSeconds.get();
    }

    public long getCompletedRunCount() {
        return completedRunCount.get();
    }
}
