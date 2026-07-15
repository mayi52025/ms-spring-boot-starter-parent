package com.ms.middleware.console.agent;

import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * LangChain4j Tool 包装：将 {@link MiddlewareInsightTool} 暴露给 LLM，禁止 LLM 直读 Ledger。
 */
public class MiddlewareInsightLangChainTools {

    private final MiddlewareInsightTool insightTool;

    public MiddlewareInsightLangChainTools(MiddlewareInsightTool insightTool) {
        this.insightTool = insightTool;
    }

    @Tool("按 runId 查询自治事件详情，含状态、问题、计划与时间线概览")
    public String describeRun(@P("自治 runId") String runId) {
        return insightTool.describeRun(runId);
    }

    @Tool("列出当前进行中的活跃故障与自治事件")
    public String listActiveIssues() {
        return insightTool.listActiveIssues();
    }

    @Tool("返回中间件与自治指标摘要：缓存命中率、MQ 失败、MTTR 等")
    public String getMetricsSummary() {
        return insightTool.getMetricsSummary();
    }

    @Tool("按 messageId 查询 MQ 消费 Trace")
    public String searchTrace(@P("MQ messageId") String messageId) {
        return insightTool.searchTrace(messageId);
    }

    @Tool("列出最近消费失败的消息摘要，便于排障")
    public String listRecentFailedTraces(@P("返回条数，默认 10") int limit) {
        int safe = limit > 0 ? limit : 10;
        return insightTool.listRecentFailedTraces(safe);
    }

    @Tool("返回最近若干条自治 run 摘要")
    public String describeRecentRuns(@P("返回条数，默认 5") int limit) {
        int safe = limit > 0 ? limit : 5;
        return insightTool.describeRecentRuns(safe);
    }

    @Tool("按关键词检索相似历史自治 run")
    public String searchSimilarRuns(
            @P("检索关键词，如 MQ、Redis、incident 类型") String query,
            @P("返回条数，默认 5") int topK) {
        int safe = topK > 0 ? topK : 5;
        return insightTool.searchSimilarRuns(query, safe);
    }
}
