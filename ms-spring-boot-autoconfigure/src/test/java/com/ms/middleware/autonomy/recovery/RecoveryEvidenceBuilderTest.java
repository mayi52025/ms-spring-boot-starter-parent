package com.ms.middleware.autonomy.recovery;

import com.ms.middleware.autonomy.context.AutonomyContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 恢复证据构建：各 incident 前后指标对比与 STABLE 文案。
 */
class RecoveryEvidenceBuilderTest {

    @Test
    void mqDegradedEvidenceShowsBeforeAfterAndThreshold() {
        AutonomyContext before = context(5, 3, true, true, 0.9, 0.5);
        AutonomyContext after = context(0, 3, true, true, 0.9, 0.5);

        RecoveryEvidence evidence = RecoveryEvidenceBuilder.build("MQ_DEGRADED", before, after);

        assertEquals("MQ_DEGRADED", evidence.getIncidentType());
        assertEquals("MQ窗口失败 5→0（阈值<3）", evidence.getSummary());
        assertEquals("mqFailedCount < mqFailedWarnThreshold", evidence.getResolutionRule());
        assertEquals(1, evidence.getMetrics().size());
        assertEquals("5", evidence.getMetrics().get(0).getBeforeValue());
        assertEquals("0", evidence.getMetrics().get(0).getAfterValue());
    }

    @Test
    void redisEvidenceShowsHealthTransition() {
        AutonomyContext before = context(0, 3, false, true, 1.0, 0.5);
        AutonomyContext after = context(0, 3, true, true, 1.0, 0.5);

        RecoveryEvidence evidence = RecoveryEvidenceBuilder.build("REDIS_UNAVAILABLE", before, after);

        assertEquals("Redis 不可用→可用", evidence.getSummary());
        assertEquals("redisHealthy == true", evidence.getResolutionRule());
    }

    @Test
    void cacheDegradedEvidenceShowsHitRatePercent() {
        AutonomyContext before = context(0, 3, true, true, 0.3, 0.5);
        AutonomyContext after = context(0, 3, true, true, 0.85, 0.5);

        RecoveryEvidence evidence = RecoveryEvidenceBuilder.build("CACHE_DEGRADED", before, after);

        assertTrue(evidence.getSummary().contains("30.0%"));
        assertTrue(evidence.getSummary().contains("85.0%"));
    }

    @Test
    void formatStableMessageIncludesMttrAndSummary() {
        RecoveryEvidence evidence = RecoveryEvidenceBuilder.build(
                "MQ_DEGRADED", context(5, 3, true, true, 0.9, 0.5), context(0, 3, true, true, 0.9, 0.5));

        String message = RecoveryEvidenceBuilder.formatStableMessage(evidence, 42);

        assertTrue(message.contains("MTTR=42s"));
        assertTrue(message.contains("MQ窗口失败 5→0"));
        assertTrue(message.contains("本次自治结束"));
    }

    private static AutonomyContext context(long mqFailed,
                                           long mqThreshold,
                                           boolean redisHealthy,
                                           boolean rabbitHealthy,
                                           double hitRate,
                                           double hitThreshold) {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setMqFailedCount(mqFailed);
        ctx.setMqFailedWarnThreshold(mqThreshold);
        ctx.setRedisHealthy(redisHealthy);
        ctx.setRabbitMqHealthy(rabbitHealthy);
        ctx.setCacheHitRate(hitRate);
        ctx.setCacheHitRateWarnThreshold(hitThreshold);
        return ctx;
    }
}
