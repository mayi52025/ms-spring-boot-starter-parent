package com.ms.middleware.console.chat;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import com.ms.middleware.console.agent.ConsoleLlmChatService;
import com.ms.middleware.console.api.ConsoleChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 控制台对话：{@code llm-enabled=true} 走 LangChain4j + Insight Tool；否则规则模式。
 */
@Service
public class ConsoleChatService {

    private final MiddlewareInsightTool insightTool;
    private final MsMiddlewareProperties properties;
    private final ObjectProvider<ConsoleLlmChatService> llmChatService;

    public ConsoleChatService(MiddlewareInsightTool insightTool,
                              MsMiddlewareProperties properties,
                              ObjectProvider<ConsoleLlmChatService> llmChatService) {
        this.insightTool = insightTool;
        this.properties = properties;
        this.llmChatService = llmChatService;
    }

    public ConsoleChatResponse chat(String message, String runId) {
        ConsoleLlmChatService llm = llmChatService.getIfAvailable();
        if (properties.getConsole().isLlmEnabled() && llm != null) {
            String reply = llm.chat(message, runId);
            boolean llmUsed = llm.isConfigured();
            return new ConsoleChatResponse(reply, llmUsed);
        }
        return new ConsoleChatResponse(ruleBasedChat(message, runId), false);
    }

    private String ruleBasedChat(String message, String runId) {
        String normalized = message != null ? message.toLowerCase() : "";

        if (runId != null && !runId.isBlank()) {
            return insightTool.describeRun(runId);
        }

        if (normalized.contains("问题") || normalized.contains("故障") || normalized.contains("issue")) {
            return insightTool.listActiveIssues();
        }

        if (normalized.contains("最近失败") || normalized.contains("失败消息")
                || normalized.contains("failed trace") || normalized.contains("failed traces")) {
            return insightTool.listRecentFailedTraces(10);
        }

        if (normalized.contains("最近") || normalized.contains("run")) {
            return insightTool.describeRecentRuns(5);
        }

        if (normalized.contains("指标") || normalized.contains("metric")) {
            return insightTool.getMetricsSummary();
        }

        if (normalized.contains("trace") || normalized.contains("消息") || normalized.contains("messageid")) {
            String messageId = extractMessageId(message);
            if (messageId != null) {
                return insightTool.searchTrace(messageId);
            }
        }

        return "我是 ms 中间件控制台助手（规则模式）。可问：「当前有什么问题」「最近 run」「最近失败」「指标」「trace <messageId>」「提供 runId 查详情」。";
    }

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
