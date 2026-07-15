package com.ms.middleware.console.agent.grounding;

import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Insight Tool 唯一网关：白名单只读 Tool + 调用审计。
 */
@Component
public class InsightToolGateway {

    private final MiddlewareInsightTool insightTool;
    private final ThreadLocal<List<ToolCallRecord>> audit = ThreadLocal.withInitial(ArrayList::new);

    public InsightToolGateway(MiddlewareInsightTool insightTool) {
        this.insightTool = insightTool;
    }

    public AuditScope openAudit() {
        audit.get().clear();
        return new AuditScope();
    }

    public String describeRun(String runId) {
        return record(InsightToolName.DESCRIBE_RUN, "runId=" + runId,
                () -> insightTool.describeRun(runId));
    }

    public String listActiveIssues() {
        return record(InsightToolName.LIST_ACTIVE_ISSUES, "",
                insightTool::listActiveIssues);
    }

    public String getMetricsSummary() {
        return record(InsightToolName.GET_METRICS_SUMMARY, "",
                insightTool::getMetricsSummary);
    }

    public String searchTrace(String messageId) {
        return record(InsightToolName.SEARCH_TRACE, "messageId=" + messageId,
                () -> insightTool.searchTrace(messageId));
    }

    public String listRecentFailedTraces(int limit) {
        int safe = limit > 0 ? limit : 10;
        return record(InsightToolName.LIST_RECENT_FAILED_TRACES, "limit=" + safe,
                () -> insightTool.listRecentFailedTraces(safe));
    }

    public String describeRecentRuns(int limit) {
        int safe = limit > 0 ? limit : 5;
        return record(InsightToolName.DESCRIBE_RECENT_RUNS, "limit=" + safe,
                () -> insightTool.describeRecentRuns(safe));
    }

    public String searchSimilarRuns(String query, int topK) {
        int safe = topK > 0 ? topK : 5;
        return record(InsightToolName.SEARCH_SIMILAR_RUNS, "query=" + query + ", topK=" + safe,
                () -> insightTool.searchSimilarRuns(query, safe));
    }

    public String execute(InsightToolInvocation invocation) {
        return switch (invocation.tool()) {
            case DESCRIBE_RUN -> describeRun(requireArg(invocation, 0));
            case LIST_ACTIVE_ISSUES -> listActiveIssues();
            case GET_METRICS_SUMMARY -> getMetricsSummary();
            case SEARCH_TRACE -> searchTrace(requireArg(invocation, 0));
            case LIST_RECENT_FAILED_TRACES -> listRecentFailedTraces(parseIntArg(invocation, 0, 10));
            case DESCRIBE_RECENT_RUNS -> describeRecentRuns(parseIntArg(invocation, 0, 5));
            case SEARCH_SIMILAR_RUNS -> searchSimilarRuns(requireArg(invocation, 0), parseIntArg(invocation, 1, 5));
        };
    }

    public String executeRequiredTools(GroundingResolution resolution) {
        if (resolution.requiredTools().isEmpty()) {
            return "";
        }
        StringBuilder evidence = new StringBuilder();
        for (InsightToolInvocation invocation : resolution.requiredTools()) {
            String result = execute(invocation);
            evidence.append("【")
                    .append(invocation.tool().langChainName())
                    .append("】\n")
                    .append(result)
                    .append("\n\n");
        }
        return evidence.toString().trim();
    }

    public List<String> currentToolNames() {
        return audit.get().stream()
                .map(record -> record.tool().langChainName())
                .distinct()
                .toList();
    }

    public List<ToolCallRecord> currentRecords() {
        return Collections.unmodifiableList(new ArrayList<>(audit.get()));
    }

    private String record(InsightToolName tool, String summary, ToolSupplier supplier) {
        long start = System.nanoTime();
        String result = supplier.get();
        long durationMs = Math.max(0, (System.nanoTime() - start) / 1_000_000);
        audit.get().add(new ToolCallRecord(tool, summary, durationMs, Instant.now()));
        return result;
    }

    private static String requireArg(InsightToolInvocation invocation, int index) {
        String value = invocation.arg(index);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing argument for tool " + invocation.tool());
        }
        return value.trim();
    }

    private static int parseIntArg(InsightToolInvocation invocation, int index, int defaultValue) {
        String value = invocation.arg(index);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface ToolSupplier {
        String get();
    }

    public final class AuditScope implements AutoCloseable {
        @Override
        public void close() {
            audit.remove();
        }
    }
}
