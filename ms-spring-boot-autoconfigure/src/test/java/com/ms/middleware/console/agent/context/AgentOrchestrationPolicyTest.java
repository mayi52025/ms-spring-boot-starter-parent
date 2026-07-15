package com.ms.middleware.console.agent.context;

import com.ms.middleware.console.agent.grounding.GroundingIntent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOrchestrationPolicyTest {

    @Test
    void diagnosticMessageRequestsFailedTracesLayer() {
        assertTrue(AgentOrchestrationPolicy.shouldInjectFailedTraces("为何还没 stable", GroundingIntent.RUN_DETAIL));
        assertFalse(AgentOrchestrationPolicy.shouldInjectFailedTraces("看一下指标", GroundingIntent.METRICS));
    }

    @Test
    void historicalKeywordsTriggerRetrieval() {
        assertTrue(AgentOrchestrationPolicy.isRetrievalRequested("上次 mq 怎么修的", GroundingIntent.CHITCHAT));
        assertFalse(AgentOrchestrationPolicy.isRetrievalRequested("当前有什么问题", GroundingIntent.ACTIVE_ISSUES));
    }
}
