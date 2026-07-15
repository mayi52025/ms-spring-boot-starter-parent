package com.ms.middleware.console.agent.grounding;

/**
 * Insight Tool 白名单（只读，禁止写 Nacos / 采纳等写操作）。
 */
public enum InsightToolName {

    DESCRIBE_RUN("describeRun"),
    LIST_ACTIVE_ISSUES("listActiveIssues"),
    GET_METRICS_SUMMARY("getMetricsSummary"),
    SEARCH_TRACE("searchTrace"),
    LIST_RECENT_FAILED_TRACES("listRecentFailedTraces"),
    DESCRIBE_RECENT_RUNS("describeRecentRuns"),
    SEARCH_SIMILAR_RUNS("searchSimilarRuns");

    private final String langChainName;

    InsightToolName(String langChainName) {
        this.langChainName = langChainName;
    }

    public String langChainName() {
        return langChainName;
    }
}
