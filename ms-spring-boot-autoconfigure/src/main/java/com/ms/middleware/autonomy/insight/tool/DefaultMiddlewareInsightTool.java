package com.ms.middleware.autonomy.insight.tool;

import com.ms.middleware.autonomy.insight.FailedMessageTraceView;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.insight.MiddlewareMetricsSnapshot;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.mq.trace.MessageTrace;

/**
 * {@link MiddlewareInsightTool} 默认实现：委托 {@link MiddlewareInsightService} 并格式化为文本。
 */
public class DefaultMiddlewareInsightTool implements MiddlewareInsightTool {

    private final MiddlewareInsightService insightService;

    public DefaultMiddlewareInsightTool(MiddlewareInsightService insightService) {
        this.insightService = insightService;
    }

    @Override
    public String describeRun(String runId) {
        if (runId == null || runId.isBlank()) {
            return "runId 不能为空";
        }
        return insightService.getRun(runId)
                .map(this::formatRunSummary)
                .orElse("未找到 runId: " + runId);
    }

    @Override
    public String listActiveIssues() {
        var active = insightService.listActiveRuns();
        if (active.isEmpty()) {
            return "当前没有进行中的自治事件，中间件状态正常。";
        }
        StringBuilder sb = new StringBuilder("当前活跃事件：\n");
        for (AutonomyRun run : active) {
            sb.append("- runId=").append(run.getRunId())
                    .append(" tenant=").append(run.getTenant())
                    .append(" 问题=").append(run.getIssues())
                    .append("\n");
        }
        return sb.toString();
    }

    @Override
    public String getMetricsSummary() {
        MiddlewareMetricsSnapshot m = insightService.getMetrics();
        return String.format(
                "缓存命中率=%.1f%%, MQ失败=%d, 全局失败=%d, 活跃run=%d, 最近MTTR=%ds, 已完成自治=%d",
                m.getCacheHitRate() * 100,
                m.getMqFailedCount(),
                m.getGlobalFailureCount(),
                m.getActiveRunCount(),
                m.getLastMttrSeconds(),
                m.getCompletedAutonomyRuns());
    }

    @Override
    public String searchTrace(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return "messageId 不能为空";
        }
        return insightService.getTrace(messageId)
                .map(this::formatTrace)
                .orElse("未找到 messageId: " + messageId);
    }

    @Override
    public String listRecentFailedTraces(int limit) {
        int safeLimit = limit > 0 ? limit : 10;
        var traces = insightService.listFailedTraces(safeLimit);
        if (traces.isEmpty()) {
            return "当前没有记录到消费失败的消息（内存 Trace，重启后清空）。";
        }
        StringBuilder sb = new StringBuilder("最近消费失败消息（最多 " + traces.size() + " 条）：\n");
        for (FailedMessageTraceView trace : traces) {
            sb.append("- messageId=").append(trace.getMessageId());
            if (trace.getQueue() != null) {
                sb.append(" queue=").append(trace.getQueue());
            }
            if (trace.getErrorMessage() != null) {
                sb.append(" error=").append(trace.getErrorMessage());
            }
            sb.append("\n");
        }
        sb.append("可复制 messageId，在对话输入：trace <messageId>");
        return sb.toString();
    }

    @Override
    public String describeRecentRuns(int limit) {
        int safeLimit = limit > 0 ? limit : 5;
        var recent = insightService.listRecentRuns(safeLimit);
        if (recent.isEmpty()) {
            return "尚无自治记录。";
        }
        if (recent.size() == 1) {
            return formatRunSummary(recent.get(0));
        }
        StringBuilder sb = new StringBuilder("最近 " + recent.size() + " 条自治 run：\n");
        for (AutonomyRun run : recent) {
            sb.append("- runId=").append(run.getRunId())
                    .append(" status=").append(run.getStatus())
                    .append(" incident=")
                    .append(run.getPlan() != null ? run.getPlan().getIncidentType() : "—")
                    .append("\n");
        }
        return sb.toString();
    }

    @Override
    public String searchSimilarRuns(String query, int topK) {
        if (query == null || query.isBlank()) {
            return "查询关键词不能为空";
        }
        int limit = topK > 0 ? topK : 5;
        var runs = insightService.searchSimilarRuns(query, limit);
        if (runs.isEmpty()) {
            return "未找到与「" + query + "」相似的历史 run";
        }
        StringBuilder sb = new StringBuilder("相似历史 run（最多 " + limit + " 条）：\n");
        for (AutonomyRun run : runs) {
            sb.append("- runId=").append(run.getRunId())
                    .append(" incident=")
                    .append(run.getPlan() != null ? run.getPlan().getIncidentType() : "—")
                    .append(" status=").append(run.getStatus())
                    .append("\n");
        }
        return sb.toString();
    }

    private String formatRunSummary(AutonomyRun run) {
        StringBuilder sb = new StringBuilder();
        sb.append("runId=").append(run.getRunId())
                .append(" tenant=").append(run.getTenant())
                .append(" 状态=").append(run.getStatus()).append("\n");
        run.getMttrSeconds().ifPresent(mttr -> sb.append("MTTR=").append(mttr).append("s\n"));
        sb.append("问题列表: ").append(run.getIssues()).append("\n");
        if (run.getPlan() != null) {
            sb.append("摘要: ").append(run.getPlan().getSummary()).append("\n");
            sb.append("推荐数: ").append(run.getRecommendations().size()).append("\n");
        }
        sb.append("时间线条数: ").append(run.getTimeline().size());
        return sb.toString();
    }

    private String formatTrace(MessageTrace trace) {
        return String.format("messageId=%s success=%s error=%s processMs=%d",
                trace.getMessageId(),
                trace.isSuccess(),
                trace.getErrorMessage() != null ? trace.getErrorMessage() : "—",
                trace.getProcessTimeMs());
    }
}
