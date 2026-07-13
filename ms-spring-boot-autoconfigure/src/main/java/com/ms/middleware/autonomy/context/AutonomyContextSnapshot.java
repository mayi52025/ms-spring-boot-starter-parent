package com.ms.middleware.autonomy.context;

import java.util.ArrayList;

/**
 * 自治上下文快照工具：故障基线只采一次，避免 run.context 被后续 tick 覆盖。
 */
public final class AutonomyContextSnapshot {

    private AutonomyContextSnapshot() {
    }

    /** 复制决策与 STABLE 判定相关的信号字段（不含 issues 文案） */
    public static AutonomyContext copy(AutonomyContext source) {
        if (source == null) {
            return null;
        }
        AutonomyContext copy = new AutonomyContext();
        copy.setCapturedAt(source.getCapturedAt());
        copy.setRedisHealthy(source.isRedisHealthy());
        copy.setRabbitMqHealthy(source.isRabbitMqHealthy());
        copy.setCacheHitRate(source.getCacheHitRate());
        copy.setMqFailedCount(source.getMqFailedCount());
        copy.setGlobalFailureCount(source.getGlobalFailureCount());
        copy.setMqFailedWarnThreshold(source.getMqFailedWarnThreshold());
        copy.setCacheHitRateWarnThreshold(source.getCacheHitRateWarnThreshold());
        copy.setHotKeys(new ArrayList<>(source.getHotKeys()));
        copy.setIssues(new ArrayList<>(source.getIssues()));
        return copy;
    }
}
