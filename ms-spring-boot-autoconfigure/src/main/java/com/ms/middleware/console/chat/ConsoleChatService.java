package com.ms.middleware.console.chat;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import com.ms.middleware.console.api.ConsoleChatResponse;
import org.springframework.stereotype.Service;

/**
 * 控制台对话（Phase 5 接入 LangChain4j；当前为基于 {@link MiddlewareInsightTool} 的规则回复）。
 */
@Service
public class ConsoleChatService {

    private final MiddlewareInsightTool insightTool;
    private final MsMiddlewareProperties properties;

    public ConsoleChatService(MiddlewareInsightTool insightTool,
                              MsMiddlewareProperties properties) {
        this.insightTool = insightTool;
        this.properties = properties;
    }

    /**
     * 规则模式聊天：按关键词路由到 Insight Tool。
     * 支持：runId 详情 / 「问题」「故障」/ 「最近失败」/ 「最近」「run」/ 「指标」「metric」/ messageId 追踪
     */
    public ConsoleChatResponse chat(String message, String runId) {
        if (properties.getConsole().isChatEnabled()) {
            return new ConsoleChatResponse(
                    "聊天 LLM 将在 Phase 5 接入 LangChain4j。当前请使用 runId 查询账本。",
                    false);
        }

        String normalized = message != null ? message.toLowerCase() : "";

        if (runId != null && !runId.isBlank()) {
            return new ConsoleChatResponse(insightTool.describeRun(runId), false);
        }

        if (normalized.contains("问题") || normalized.contains("故障") || normalized.contains("issue")) {
            return new ConsoleChatResponse(insightTool.listActiveIssues(), false);
        }

        if (normalized.contains("最近失败") || normalized.contains("失败消息")
                || normalized.contains("failed trace") || normalized.contains("failed traces")) {
            return new ConsoleChatResponse(insightTool.listRecentFailedTraces(10), false);
        }

        if (normalized.contains("最近") || normalized.contains("run")) {
            return new ConsoleChatResponse(insightTool.describeRecentRuns(5), false);
        }

        if (normalized.contains("指标") || normalized.contains("metric")) {
            return new ConsoleChatResponse(insightTool.getMetricsSummary(), false);
        }

        if (normalized.contains("trace") || normalized.contains("消息") || normalized.contains("messageid")) {
            String messageId = extractMessageId(message);
            if (messageId != null) {
                return new ConsoleChatResponse(insightTool.searchTrace(messageId), false);
            }
        }

        return new ConsoleChatResponse(
                "我是 ms 中间件控制台助手（规则模式）。可问：「当前有什么问题」「最近 run」「最近失败」「指标」「trace <messageId>」「提供 runId 查详情」。",
                false);
    }

    /** 从聊天文本中提取 messageId（简单启发式：连续 8+ 位字母数字） */
    private String extractMessageId(String message) {
        if (message == null) {
            return null;
        }
        for (String token : message.split("\\s+")) {
            String cleaned = token.replaceAll("[^a-zA-Z0-9-]", "");
            if (cleaned.length() >= 8) {
                return cleaned;
            }
        }
        return null;
    }
}
