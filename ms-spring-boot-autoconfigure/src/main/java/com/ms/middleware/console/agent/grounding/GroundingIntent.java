package com.ms.middleware.console.agent.grounding;

/**
 * 控制台聊天意图（与规则模式、LLM Grounding 共用）。
 */
public enum GroundingIntent {

    CHITCHAT,
    RUN_DETAIL,
    ACTIVE_ISSUES,
    RECENT_FAILED_TRACES,
    RECENT_RUNS,
    METRICS,
    TRACE_LOOKUP,
    SIMILAR_RUNS
}
