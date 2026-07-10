package com.ms.middleware.autonomy.insight;

import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.run.AutonomyLedger;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.mq.trace.MessageTrace;
import com.ms.middleware.mq.trace.MessageTraceManager;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link MiddlewareInsightService} 默认实现：聚合账本 + MsMetrics。
 * 控制台、规则聊天、未来 LangChain4j Tool 都应依赖此接口而非直接访问 Ledger。
 */
public class DefaultMiddlewareInsightService implements MiddlewareInsightService {

    private final AutonomyLedger ledger;
    private final MsMetrics metrics;

    public DefaultMiddlewareInsightService(AutonomyLedger ledger, MsMetrics metrics) {
        this.ledger = ledger;
        this.metrics = metrics;
    }

    @Override
    public Optional<AutonomyRun> getRun(String runId) {
        return ledger.get(runId);
    }

    @Override
    public List<AutonomyRun> listActiveRuns() {
        return ledger.listActive();
    }

    @Override
    public List<AutonomyRun> listRecentRuns(int limit) {
        return ledger.listRecent(limit);
    }

    @Override
    public Optional<MessageTrace> getTrace(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return Optional.empty();
        }
        MessageTrace trace = MessageTraceManager.getInstance().getTrace(messageId);
        return Optional.ofNullable(trace);
    }

    @Override
    public MiddlewareMetricsSnapshot getMetrics() {
        MiddlewareMetricsSnapshot snapshot = new MiddlewareMetricsSnapshot();
        snapshot.setCacheHitRate(metrics.getCacheHitRate());
        snapshot.setMqFailedCount(metrics.getMessageFailedCount());
        snapshot.setGlobalFailureCount(metrics.getFailureCount());
        snapshot.setActiveRunCount(ledger.listActive().size());
        return snapshot;
    }

    /** Phase 1 关键词匹配；Phase 5+ 可换 pgvector 语义检索 */
    @Override
    public List<AutonomyRun> searchSimilarRuns(String query, int topK) {
        if (query == null || query.isBlank()) {
            return ledger.listRecent(topK);
        }
        String normalized = query.trim().toUpperCase(Locale.ROOT);
        return ledger.listRecent(200).stream()
                .filter(run -> matchesQuery(run, normalized))
                .limit(topK)
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(AutonomyRun run, String normalized) {
        AutonomyPlan plan = run.getPlan();
        if (plan != null && plan.getIncidentType() != null
                && plan.getIncidentType().toUpperCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        return run.getIssues().stream()
                .anyMatch(issue -> issue.toUpperCase(Locale.ROOT).contains(normalized));
    }
}
