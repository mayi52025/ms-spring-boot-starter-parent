package com.ms.middleware.console.chat;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.console.agent.ConsoleLlmChatResult;
import com.ms.middleware.console.agent.ConsoleLlmChatService;
import com.ms.middleware.console.agent.grounding.GroundingPolicy;
import com.ms.middleware.console.agent.grounding.GroundingResolution;
import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import com.ms.middleware.console.api.ConsoleChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 控制台对话：{@code llm-enabled=true} 走 LangChain4j + Insight Tool；否则规则模式。
 */
@Service
public class ConsoleChatService {

    private final GroundingPolicy groundingPolicy;
    private final InsightToolGateway insightToolGateway;
    private final MsMiddlewareProperties properties;
    private final ObjectProvider<ConsoleLlmChatService> llmChatService;

    public ConsoleChatService(GroundingPolicy groundingPolicy,
                              InsightToolGateway insightToolGateway,
                              MsMiddlewareProperties properties,
                              ObjectProvider<ConsoleLlmChatService> llmChatService) {
        this.groundingPolicy = groundingPolicy;
        this.insightToolGateway = insightToolGateway;
        this.properties = properties;
        this.llmChatService = llmChatService;
    }

    public ConsoleChatResponse chat(String message, String runId) {
        ConsoleLlmChatService llm = llmChatService.getIfAvailable();
        if (properties.getConsole().isLlmEnabled() && llm != null) {
            ConsoleLlmChatResult result = llm.chat(message, runId);
            boolean llmUsed = llm.isConfigured();
            return new ConsoleChatResponse(result.reply(), llmUsed, result.toolsUsed(), result.grounded());
        }
        return ruleBasedChat(message, runId);
    }

    private ConsoleChatResponse ruleBasedChat(String message, String runId) {
        GroundingResolution resolution = groundingPolicy.resolve(message, runId);
        try (InsightToolGateway.AuditScope ignored = insightToolGateway.openAudit()) {
            if (!resolution.opsQuestion()) {
                return new ConsoleChatResponse(groundingPolicy.ruleModeHelpMessage(), false, List.of(), true);
            }
            String reply = insightToolGateway.executeRequiredTools(resolution);
            List<String> toolsUsed = insightToolGateway.currentToolNames();
            return new ConsoleChatResponse(reply, false, toolsUsed, !toolsUsed.isEmpty());
        }
    }
}
