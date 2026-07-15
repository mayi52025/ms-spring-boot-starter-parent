package com.ms.middleware.console.agent;

import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * LangChain4j Tool 包装：经 {@link InsightToolGateway} 暴露只读 Insight，并记录调用审计。
 */
public class MiddlewareInsightLangChainTools {

    private final InsightToolGateway gateway;

    public MiddlewareInsightLangChainTools(InsightToolGateway gateway) {
        this.gateway = gateway;
    }

    @Tool("按 runId 查询自治事件详情，含状态、问题、计划与时间线概览")
    public String describeRun(@P("自治 runId") String runId) {
        return gateway.describeRun(runId);
    }

    @Tool("列出当前进行中的活跃故障与自治事件")
    public String listActiveIssues() {
        return gateway.listActiveIssues();
    }

    @Tool("返回中间件与自治指标摘要：缓存命中率、MQ 失败、MTTR 等")
    public String getMetricsSummary() {
        return gateway.getMetricsSummary();
    }

    @Tool("按 messageId 查询 MQ 消费 Trace")
    public String searchTrace(@P("MQ messageId") String messageId) {
        return gateway.searchTrace(messageId);
    }

    @Tool("列出最近消费失败的消息摘要，便于排障")
    public String listRecentFailedTraces(@P("返回条数，默认 10") int limit) {
        return gateway.listRecentFailedTraces(limit);
    }

    @Tool("返回最近若干条自治 run 摘要")
    public String describeRecentRuns(@P("返回条数，默认 5") int limit) {
        return gateway.describeRecentRuns(limit);
    }

    @Tool("按关键词检索相似历史自治 run")
    public String searchSimilarRuns(
            @P("检索关键词，如 MQ、Redis、incident 类型") String query,
            @P("返回条数，默认 5") int topK) {
        return gateway.searchSimilarRuns(query, topK);
    }
}
