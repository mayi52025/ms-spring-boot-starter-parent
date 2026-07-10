package com.ms.middleware.console.chat;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.api.ConsoleChatResponse;
import org.springframework.stereotype.Service;

/**
 * 控制台对话（Phase 5 接入 LangChain4j；当前为基于 Insight 的规则回复）。
 */
@Service
public class ConsoleChatService {

    private final MiddlewareInsightService insightService;
    private final MsMiddlewareProperties properties;

    public ConsoleChatService(MiddlewareInsightService insightService,
                              MsMiddlewareProperties properties) {
        this.insightService = insightService;
        this.properties = properties;
    }

    /**
     * 规则模式聊天：按关键词路由到 Insight 查询。
     * 支持：runId 详情 / 「问题」「故障」/ 「最近」「run」/ 「指标」「metric」
     */
    public ConsoleChatResponse chat(String message, String runId) {
        if (properties.getConsole().isChatEnabled()) {
            return new ConsoleChatResponse(
                    "聊天 LLM 将在 Phase 5 接入 LangChain4j。当前请使用 runId 查询账本。",
                    false);
        }

        String normalized = message != null ? message.toLowerCase() : "";

        if (runId != null && !runId.isBlank()) {
            return insightService.getRun(runId)
                    .map(run -> new ConsoleChatResponse(formatRunSummary(run), false))
                    .orElse(new ConsoleChatResponse("未找到 runId: " + runId, false));
        }

        if (normalized.contains("问题") || normalized.contains("故障") || normalized.contains("issue")) {
            var active = insightService.listActiveRuns();
            if (active.isEmpty()) {
                return new ConsoleChatResponse("当前没有进行中的自治事件，中间件状态正常。", false);
            }
            StringBuilder sb = new StringBuilder("当前活跃事件：\n");
            for (AutonomyRun run : active) {
                sb.append("- runId=").append(run.getRunId())
                        .append(" tenant=").append(run.getTenant())
                        .append(" 问题=").append(run.getIssues())
                        .append("\n");
            }
            return new ConsoleChatResponse(sb.toString(), false);
        }

        if (normalized.contains("最近") || normalized.contains("run")) {
            var recent = insightService.listRecentRuns(10);
            if (recent.isEmpty()) {
                return new ConsoleChatResponse("尚无自治记录。", false);
            }
            return new ConsoleChatResponse(formatRunSummary(recent.get(0)), false);
        }

        if (normalized.contains("指标") || normalized.contains("metric")) {
            var m = insightService.getMetrics();
            return new ConsoleChatResponse(String.format(
                    "缓存命中率=%.1f%%, MQ失败=%d, 全局失败=%d, 活跃run=%d",
                    m.getCacheHitRate() * 100,
                    m.getMqFailedCount(),
                    m.getGlobalFailureCount(),
                    m.getActiveRunCount()), false);
        }

        return new ConsoleChatResponse(
                "我是 ms 中间件控制台助手（规则模式）。可问：「当前有什么问题」「最近 run」「指标」「提供 runId 查详情」。",
                false);
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
}
