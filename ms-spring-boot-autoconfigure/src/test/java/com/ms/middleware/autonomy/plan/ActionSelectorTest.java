package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link ActionSelector} 规则选优：词典序排序稳定，PLAN 说明可解释。
 */
class ActionSelectorTest {

    private final ActionSelector selector = new ActionSelector();

    /** MQ 明显超阈值：限流（LOW）排第一，延迟重试（MEDIUM）排第二 */
    @Test
    void mqDegradedSelectsThrottleFirstByRunbookAndRisk() {
        AutonomyContext ctx = mqContext(15, 10);
        List<PlannedAction> selected = selector.select(
                IncidentActionCatalog.defaultCandidatesFor("MQ_DEGRADED", ctx), ctx);

        assertEquals(2, selected.size());
        assertEquals(AutonomyActionType.THROTTLE_CONSUMER, selected.get(0).getActionType());
        assertEquals(1, selected.get(0).getRank());
        assertEquals(AutonomyActionType.DELAYED_RETRY_BATCH, selected.get(1).getActionType());
        assertEquals(2, selected.get(1).getRank());
        assertEquals(0, selected.get(0).getScore(), "规则选优不使用浮点 score");
    }

    /** MQ 明显超阈值时，限流证据强度应达自动执行门槛 */
    @Test
    void mqWellAboveThresholdMeetsAutoEvidence() {
        AutonomyContext ctx = mqContext(15, 10);
        List<PlannedAction> selected = selector.select(
                IncidentActionCatalog.defaultCandidatesFor("MQ_DEGRADED", ctx), ctx);

        assertTrue(selected.get(0).getConfidence() >= 0.7,
                "明显超阈值时限流证据应足够 AUTO，实际=" + selected.get(0).getConfidence());
    }

    /** Redis 宕机：治根因的自愈动作排第一（词典序「根因优先」） */
    @Test
    void redisDownSelectsRootCauseRecoveryFirst() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setRedisHealthy(false);
        ctx.getHotKeys().add("order:1");

        List<PlannedAction> selected = selector.select(
                IncidentActionCatalog.defaultCandidatesFor("REDIS_UNAVAILABLE", ctx), ctx);

        assertEquals(AutonomyActionType.TRIGGER_REDIS_RECOVERY, selected.get(0).getActionType());
        assertEquals(1, selected.get(0).getRank());
    }

    /** 选优说明应包含规则依据与证据强度，不出现 score= */
    @Test
    void selectionSummaryExplainsRuleBasis() {
        AutonomyContext ctx = mqContext(15, 10);
        List<PlannedAction> selected = selector.select(
                IncidentActionCatalog.defaultCandidatesFor("MQ_DEGRADED", ctx), ctx);
        String summary = selector.buildSelectionSummary(selected);

        assertTrue(summary.contains("根因优先"));
        assertTrue(summary.contains("THROTTLE_CONSUMER"));
        assertTrue(summary.contains("证据强度"));
        assertTrue(summary.contains("自动执行候选"));
    }

    private AutonomyContext mqContext(long failedCount, long threshold) {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setMqFailedCount(failedCount);
        ctx.setMqFailedWarnThreshold(threshold);
        ctx.getIssues().add("MQ 消费失败累计偏高: " + failedCount);
        return ctx;
    }
}
