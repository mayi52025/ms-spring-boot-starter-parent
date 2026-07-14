package com.ms.middleware.autonomy.insight.tool;

/**
 * 中间件洞察 Tool 契约：为控制台规则聊天与未来 LangChain4j @Tool 提供统一读数入口。
 *
 * <p>所有方法返回 LLM/聊天友好的文本，内部委托 {@link com.ms.middleware.autonomy.insight.MiddlewareInsightService}，
 * 避免 LLM 层直接依赖 Ledger 或 MsMetrics。</p>
 *
 * <p>扩展方式：实现本接口并注册 Spring Bean（{@code @ConditionalOnMissingBean} 可覆盖默认实现）。</p>
 */
public interface MiddlewareInsightTool {

    /**
     * 按 runId 返回自治事件摘要（状态、问题、计划、时间线概览）。
     */
    String describeRun(String runId);

    /**
     * 列出当前进行中的自治事件。
     */
    String listActiveIssues();

    /**
     * 返回中间件与自治指标摘要（命中率、MQ 失败、MTTR 等）。
     */
    String getMetricsSummary();

    /**
     * 按 messageId 查询 MQ 消息追踪。
     */
    String searchTrace(String messageId);

    /**
     * 列出最近消费失败的 messageId 摘要（文本，供规则聊天）。
     */
    String listRecentFailedTraces(int limit);

    /**
     * 返回最近若干条自治 run 摘要。
     */
    String describeRecentRuns(int limit);

    /**
     * 按关键词检索相似历史 run（Phase 1 为 incident 过滤，Phase 5+ 可接向量检索）。
     */
    String searchSimilarRuns(String query, int topK);
}
