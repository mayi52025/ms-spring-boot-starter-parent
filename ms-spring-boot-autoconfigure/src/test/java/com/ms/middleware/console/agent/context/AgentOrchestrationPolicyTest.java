package com.ms.middleware.console.agent.context;

import com.ms.middleware.console.agent.grounding.GroundingIntent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void opsSynonymsAlsoTriggerRetrievalWithout文档Keyword() {
        assertTrue(AgentOrchestrationPolicy.isRetrievalRequested("tick 锁多实例会怎样", GroundingIntent.CHITCHAT));
        assertTrue(AgentOrchestrationPolicy.isRetrievalRequested("限流止血怎么做", GroundingIntent.CHITCHAT));
        assertTrue(AgentOrchestrationPolicy.isRetrievalRequested("手册里 mttr 怎么看", GroundingIntent.CHITCHAT));
    }

    @Test
    void tickRoutesToDocumentKind() {
        AgentOrchestrationDecision decision = new AgentOrchestrationDecision(
                null, ContextScope.GLOBAL, List.of(), true, "tick 锁要注意什么");
        assertEquals(RetrievalQuery.RetrievalKind.DOCUMENT,
                AgentOrchestrationPolicy.toRetrievalQuery(decision).kind());
    }
}
