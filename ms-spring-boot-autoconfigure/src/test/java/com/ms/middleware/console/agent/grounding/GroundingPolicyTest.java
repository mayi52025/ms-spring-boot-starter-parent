package com.ms.middleware.console.agent.grounding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundingPolicyTest {

    private final GroundingPolicy policy = new GroundingPolicy();

    @Test
    void runIdContextRequiresDescribeRun() {
        GroundingResolution resolution = policy.resolve("为何 STABLE？", "run-abc");

        assertEquals(GroundingIntent.RUN_DETAIL, resolution.intent());
        assertTrue(resolution.opsQuestion());
        assertEquals(1, resolution.requiredTools().size());
        assertEquals(InsightToolName.DESCRIBE_RUN, resolution.requiredTools().get(0).tool());
        assertEquals("run-abc", resolution.requiredTools().get(0).arg(0));
    }

    @Test
    void activeIssuesIntent() {
        GroundingResolution resolution = policy.resolve("当前有什么问题", null);

        assertEquals(GroundingIntent.ACTIVE_ISSUES, resolution.intent());
        assertEquals(InsightToolName.LIST_ACTIVE_ISSUES, resolution.requiredTools().get(0).tool());
    }

    @Test
    void recentFailedTracesIntent() {
        GroundingResolution resolution = policy.resolve("最近失败消息有哪些", null);

        assertEquals(GroundingIntent.RECENT_FAILED_TRACES, resolution.intent());
        assertEquals(InsightToolName.LIST_RECENT_FAILED_TRACES, resolution.requiredTools().get(0).tool());
    }

    @Test
    void metricsIntent() {
        GroundingResolution resolution = policy.resolve("看一下指标和 MTTR", null);

        assertEquals(GroundingIntent.METRICS, resolution.intent());
        assertEquals(InsightToolName.GET_METRICS_SUMMARY, resolution.requiredTools().get(0).tool());
    }

    @Test
    void chitchatWhenUnknown() {
        GroundingResolution resolution = policy.resolve("你好", null);

        assertEquals(GroundingIntent.CHITCHAT, resolution.intent());
        assertFalse(resolution.opsQuestion());
        assertTrue(resolution.requiredTools().isEmpty());
    }
}
