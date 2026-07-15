package com.ms.middleware.console.chat;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.console.agent.ConsoleLlmChatResult;
import com.ms.middleware.console.agent.ConsoleLlmChatService;
import com.ms.middleware.console.agent.context.ConsoleChatContextOrchestrator;
import com.ms.middleware.console.agent.grounding.GroundingMode;
import com.ms.middleware.console.agent.grounding.GroundingPolicy;
import com.ms.middleware.console.agent.grounding.GroundingResolution;
import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import com.ms.middleware.console.api.ConsoleChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 控制台对话：LLM 模式走 5.2 Grounding + 5.3 工作上下文；否则规则模式。
 */
@Service
public class ConsoleChatService {

    private final GroundingPolicy groundingPolicy;
    private final InsightToolGateway insightToolGateway;
    private final ConsoleChatContextOrchestrator contextOrchestrator;
    private final MsMiddlewareProperties properties;
    private final ObjectProvider<ConsoleLlmChatService> llmChatService;

    public ConsoleChatService(GroundingPolicy groundingPolicy,
                              InsightToolGateway insightToolGateway,
                              ConsoleChatContextOrchestrator contextOrchestrator,
                              MsMiddlewareProperties properties,
                              ObjectProvider<ConsoleLlmChatService> llmChatService) {
        this.groundingPolicy = groundingPolicy;
        this.insightToolGateway = insightToolGateway;
        this.contextOrchestrator = contextOrchestrator;
        this.properties = properties;
        this.llmChatService = llmChatService;
    }

    public ConsoleChatResponse chat(String message, String runId, String sessionId) {
        ConsoleLlmChatService llm = llmChatService.getIfAvailable();
        if (properties.getConsole().isLlmEnabled() && llm != null) {
            ConsoleLlmChatResult result = llm.chat(message, runId, sessionId);
            boolean llmUsed = llm.isConfigured();
            return new ConsoleChatResponse(
                    result.reply(),
                    llmUsed,
                    result.toolsUsed(),
                    result.grounded(),
                    result.contextHints(),
                    result.boundRunId());
        }
        return ruleBasedChat(message, runId, sessionId);
    }

    private ConsoleChatResponse ruleBasedChat(String message, String runId, String sessionId) {
        GroundingMode mode = GroundingMode.fromConfig(properties.getConsole().getLlm().getGroundingMode());
        ConsoleChatContextOrchestrator.PreparedChatContext prepared =
                contextOrchestrator.prepare(message, runId, sessionId, mode);
        GroundingResolution resolution = prepared.grounding();

        try (InsightToolGateway.AuditScope ignored = insightToolGateway.openAudit()) {
            if (!resolution.opsQuestion()) {
                return new ConsoleChatResponse(
                        groundingPolicy.ruleModeHelpMessage(),
                        false,
                        List.of(),
                        true,
                        prepared.assembledContext().contextHints(),
                        prepared.effectiveRunId());
            }
            String reply = insightToolGateway.executeRequiredTools(resolution);
            List<String> toolsUsed = insightToolGateway.currentToolNames();
            contextOrchestrator.recordTurn(prepared, message, toolsUsed);
            return new ConsoleChatResponse(
                    reply,
                    false,
                    toolsUsed,
                    !toolsUsed.isEmpty(),
                    prepared.assembledContext().contextHints(),
                    prepared.effectiveRunId());
        }
    }
}
