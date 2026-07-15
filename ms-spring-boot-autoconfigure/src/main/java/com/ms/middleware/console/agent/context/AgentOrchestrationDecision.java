package com.ms.middleware.console.agent.context;

import com.ms.middleware.console.agent.grounding.GroundingResolution;

import java.util.List;

/**
 * 统一编排决策：Grounding Tool + 上下文分层一次判定，避免双路由漂移。
 */
public record AgentOrchestrationDecision(
        GroundingResolution grounding,
        ContextScope scope,
        List<ContextLayer> contextLayers,
        boolean retrievalRequested,
        String retrievalQuery) {

    public AgentOrchestrationDecision {
        contextLayers = contextLayers != null ? List.copyOf(contextLayers) : List.of();
    }
}
