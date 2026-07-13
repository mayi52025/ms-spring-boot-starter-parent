package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link EvidenceStrengthEvaluator}：证据强度反映现场信号，与选优排序无关。
 */
class EvidenceStrengthEvaluatorTest {

    /** 刚好踩线：证据偏弱，倾向 ADVISE 不 AUTO */
    @Test
    void mqExactlyAtThresholdHasWeakEvidence() {
        AutonomyContext ctx = mqContext(10, 10);
        double evidence = EvidenceStrengthEvaluator.evaluate(AutonomyActionType.THROTTLE_CONSUMER, ctx);

        assertTrue(evidence < 0.7, "踩线时证据应不足 AUTO，实际=" + evidence);
        assertEquals(0.65, evidence, 0.001);
    }

    /** 明显超阈值：证据足够 AUTO */
    @Test
    void mqWellAboveThresholdHasStrongEvidence() {
        AutonomyContext ctx = mqContext(15, 10);
        double evidence = EvidenceStrengthEvaluator.evaluate(AutonomyActionType.THROTTLE_CONSUMER, ctx);

        assertTrue(evidence >= 0.7, "明显超阈值时证据应足够 AUTO，实际=" + evidence);
    }

    /** MEDIUM 动作证据上限较低，通常不走 AUTO */
    @Test
    void delayedRetryCappedBelowAutoThreshold() {
        AutonomyContext ctx = mqContext(20, 10);
        double evidence = EvidenceStrengthEvaluator.evaluate(AutonomyActionType.DELAYED_RETRY_BATCH, ctx);

        assertTrue(evidence < 0.7, "延迟重试证据应偏低，实际=" + evidence);
    }

    /** Redis 探活失败时，自愈证据应很高 */
    @Test
    void redisDownRecoveryHasHighEvidence() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setRedisHealthy(false);

        double evidence = EvidenceStrengthEvaluator.evaluate(AutonomyActionType.TRIGGER_REDIS_RECOVERY, ctx);
        assertTrue(evidence >= 0.9);
    }

    private AutonomyContext mqContext(long failedCount, long threshold) {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setMqFailedCount(failedCount);
        ctx.setMqFailedWarnThreshold(threshold);
        return ctx;
    }
}
