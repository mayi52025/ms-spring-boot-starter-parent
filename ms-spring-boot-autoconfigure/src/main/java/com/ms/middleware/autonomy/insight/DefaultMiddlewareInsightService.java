package com.ms.middleware.autonomy.insight;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.run.AutonomyLedger;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.mq.trace.MessageTrace;
import com.ms.middleware.mq.trace.MessageTraceManager;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link MiddlewareInsightService} 默认实现：聚合账本 + MsMetrics。
 *
 * <p>控制台、规则聊天、未来 LangChain4j Tool 都应依赖此接口而非直接访问 Ledger。
 * Step 6：在 Ledger 已按 tenant 隔离的基础上，列表接口再做一层防御性过滤，保证 /api/issues、/history 仅返回当前应用数据。</p>
 */
public class DefaultMiddlewareInsightService implements MiddlewareInsightService {

    private final AutonomyLedger ledger;
    private final MsMetrics metrics;
    private final AutonomyMetrics autonomyMetrics;
    private final AutonomyTenantProvider tenantProvider;

    public DefaultMiddlewareInsightService(AutonomyLedger ledger,
                                           MsMetrics metrics,
                                           AutonomyMetrics autonomyMetrics,
                                           AutonomyTenantProvider tenantProvider) {
        this.ledger = ledger;
        this.metrics = metrics;
        this.autonomyMetrics = autonomyMetrics;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Optional<AutonomyRun> getRun(String runId) {
        return ledger.get(runId).filter(this::belongsToCurrentTenant);
    }

    @Override
    public List<AutonomyRun> listActiveRuns() {
        return ledger.listActive().stream()
                .filter(this::belongsToCurrentTenant)
                .toList();
    }

    @Override
    public List<AutonomyRun> listHistoryRuns(int limit) {
        return ledger.listRecent(limit).stream()
                .filter(this::belongsToCurrentTenant)
                .filter(run -> run.getStatus() == AutonomyRunStatus.STABLE
                        || run.getStatus() == AutonomyRunStatus.CLOSED)
                .toList();
    }

    @Override
    public List<AutonomyRun> listRecentRuns(int limit) {
        return ledger.listRecent(limit).stream()
                .filter(this::belongsToCurrentTenant)
                .toList();
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
    public List<FailedMessageTraceView> listFailedTraces(int limit) {
        int safeLimit = limit > 0 ? Math.min(limit, 100) : 20;
        return MessageTraceManager.getInstance().listFailedTraces(safeLimit).stream()
                .map(this::toFailedView)
                .toList();
    }

    @Override
    public MiddlewareMetricsSnapshot getMetrics() {
        MiddlewareMetricsSnapshot snapshot = new MiddlewareMetricsSnapshot();
        snapshot.setCacheHitRate(metrics.getCacheHitRate());
        snapshot.setMqFailedCount(metrics.getMessageFailedCount());
        snapshot.setGlobalFailureCount(metrics.getFailureCount());
        // 活跃 run 数仅统计当前 tenant
        snapshot.setActiveRunCount(listActiveRuns().size());
        snapshot.setLastMttrSeconds(autonomyMetrics.getLastMttrSeconds());
        snapshot.setCompletedAutonomyRuns(autonomyMetrics.getCompletedRunCount());
        return snapshot;
    }

    /** Phase 1 关键词匹配；Phase 5+ 可换 pgvector 语义检索 */
    @Override
    public List<AutonomyRun> searchSimilarRuns(String query, int topK) {
        if (query == null || query.isBlank()) {
            return listRecentRuns(topK);
        }
        String normalized = query.trim().toUpperCase(Locale.ROOT);
        return ledger.listRecent(200).stream()
                .filter(this::belongsToCurrentTenant)
                .filter(run -> matchesQuery(run, normalized))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /** 防御性校验：即使 Ledger 实现疏漏，控制台也不展示其它 tenant 的 run */
    private boolean belongsToCurrentTenant(AutonomyRun run) {
        return run != null && tenantProvider.getTenant().equals(run.getTenant());
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

    private FailedMessageTraceView toFailedView(MessageTrace trace) {
        FailedMessageTraceView view = new FailedMessageTraceView();
        view.setMessageId(trace.getMessageId());
        view.setQueue(trace.getQueue());
        view.setErrorMessage(trace.getErrorMessage());
        view.setProcessTimeMs(trace.getProcessTimeMs());
        view.setProcessTime(trace.getProcessTime());
        return view;
    }
}
