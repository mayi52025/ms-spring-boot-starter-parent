package com.ms.middleware.autonomy.insight;

import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.mq.trace.MessageTrace;

import java.util.List;
import java.util.Optional;

/**
 * 中间件洞察统一门面：控制台、规则引擎、未来 LangChain4j Tool 均通过此接口读数据。
 */
public interface MiddlewareInsightService {

    Optional<AutonomyRun> getRun(String runId);

    List<AutonomyRun> listActiveRuns();

    List<AutonomyRun> listRecentRuns(int limit);

    Optional<MessageTrace> getTrace(String messageId);

    MiddlewareMetricsSnapshot getMetrics();

    /**
     * Phase 5+ pgvector 语义检索；Phase 1 按 incidentType 过滤最近 run。
     */
    List<AutonomyRun> searchSimilarRuns(String query, int topK);
}
