package com.ms.middleware.console.agent;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.console.agent.context.ConsoleChatContextOrchestrator;
import com.ms.middleware.console.agent.grounding.GroundingMode;
import com.ms.middleware.console.agent.grounding.GroundingValidator;
import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import com.ms.middleware.console.agent.grounding.StrictGroundingExecutor;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * LLM 对话服务：5.2 Grounding + 5.3 工作上下文编排。
 */
@Service
@ConditionalOnProperty(prefix = "ms.middleware.console", name = "llm-enabled", havingValue = "true")
public class ConsoleLlmChatService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleLlmChatService.class);

    private final MsMiddlewareProperties properties;
    private final MiddlewareInsightLangChainTools insightTools;
    private final InsightToolGateway insightToolGateway;
    private final StrictGroundingExecutor strictGroundingExecutor;
    private final GroundingValidator groundingValidator;
    private final ConsoleChatContextOrchestrator contextOrchestrator;
    private volatile ConsoleLlmAgent agent;

    public ConsoleLlmChatService(MsMiddlewareProperties properties,
                                 MiddlewareInsightLangChainTools insightTools,
                                 InsightToolGateway insightToolGateway,
                                 StrictGroundingExecutor strictGroundingExecutor,
                                 GroundingValidator groundingValidator,
                                 ConsoleChatContextOrchestrator contextOrchestrator) {
        this.properties = properties;
        this.insightTools = insightTools;
        this.insightToolGateway = insightToolGateway;
        this.strictGroundingExecutor = strictGroundingExecutor;
        this.groundingValidator = groundingValidator;
        this.contextOrchestrator = contextOrchestrator;
    }

    public boolean isConfigured() {
        return !resolveApiKey().isBlank();
    }

    public ConsoleLlmChatResult chat(String message, String runId, String sessionId) {
        if (message == null || message.isBlank()) {
            return ConsoleLlmChatResult.of("请输入问题。", List.of(), true);
        }
        if (!isConfigured()) {
            return ConsoleLlmChatResult.of(
                    "LLM 已启用但未配置 API Key。请设置环境变量 MS_LLM_API_KEY 或 ms.middleware.console.llm.api-key。",
                    List.of(),
                    false);
        }

        GroundingMode mode = resolveGroundingMode();
        ConsoleChatContextOrchestrator.PreparedChatContext preparedContext =
                contextOrchestrator.prepare(message, runId, sessionId, mode);

        try (InsightToolGateway.AuditScope ignored = insightToolGateway.openAudit()) {
            StrictGroundingExecutor.PreparedContext groundingPrepared = strictGroundingExecutor.prepareWithComposedMessage(
                    preparedContext.messageForGrounding(),
                    preparedContext.grounding(),
                    mode,
                    insightToolGateway);
            try {
                ConsoleLlmAgent llmAgent = getOrCreateAgent();
                String llmReply = llmAgent.chat(groundingPrepared.userMessage());
                List<String> toolsUsed = insightToolGateway.currentToolNames();
                GroundingValidator.ValidationResult validation = groundingValidator.validate(
                        llmReply,
                        toolsUsed,
                        preparedContext.grounding(),
                        mode,
                        groundingPrepared.prefetchedEvidence());
                contextOrchestrator.recordTurn(preparedContext, message, toolsUsed);

                List<String> contextHints = preparedContext.assembledContext().contextHints();
                String boundRunId = preparedContext.effectiveRunId();

                if (!validation.grounded()) {
                    return ConsoleLlmChatResult.of(
                            validation.fallbackReply(), toolsUsed, false, contextHints, boundRunId);
                }
                boolean grounded = !preparedContext.grounding().opsQuestion() || !toolsUsed.isEmpty();
                return ConsoleLlmChatResult.of(llmReply, toolsUsed, grounded, contextHints, boundRunId);
            } catch (Exception e) {
                log.warn("LLM chat failed: {}", e.getMessage());
                List<String> toolsUsed = insightToolGateway.currentToolNames();
                contextOrchestrator.recordTurn(preparedContext, message, toolsUsed);
                if (mode == GroundingMode.STRICT
                        && preparedContext.grounding().opsQuestion()
                        && groundingPrepared.prefetchedEvidence() != null
                        && !groundingPrepared.prefetchedEvidence().isBlank()) {
                    return ConsoleLlmChatResult.of(
                            "（Grounding）LLM 不可用，以下为 Insight Tool 数据：\n\n" + groundingPrepared.prefetchedEvidence(),
                            toolsUsed,
                            true,
                            preparedContext.assembledContext().contextHints(),
                            preparedContext.effectiveRunId());
                }
                return ConsoleLlmChatResult.of(
                        "LLM 请求失败：" + e.getMessage() + "。可暂时关闭 llm-enabled 使用规则模式。",
                        toolsUsed,
                        false,
                        preparedContext.assembledContext().contextHints(),
                        preparedContext.effectiveRunId());
            }
        }
    }

    private GroundingMode resolveGroundingMode() {
        return GroundingMode.fromConfig(properties.getConsole().getLlm().getGroundingMode());
    }

    private ConsoleLlmAgent getOrCreateAgent() {
        if (agent == null) {
            synchronized (this) {
                if (agent == null) {
                    MsMiddlewareProperties.LlmProperties llm = properties.getConsole().getLlm();
                    ChatLanguageModel model = OpenAiChatModel.builder()
                            .baseUrl(normalizeBaseUrl(llm.getBaseUrl()))
                            .apiKey(resolveApiKey())
                            .modelName(llm.getModel())
                            .temperature(llm.getTemperature())
                            .timeout(Duration.ofSeconds(Math.max(10, llm.getTimeoutSeconds())))
                            .logRequests(false)
                            .logResponses(false)
                            .build();
                    agent = AiServices.builder(ConsoleLlmAgent.class)
                            .chatLanguageModel(model)
                            .tools(insightTools)
                            .build();
                }
            }
        }
        return agent;
    }

    void overrideAgentForTest(ConsoleLlmAgent testAgent) {
        this.agent = testAgent;
    }

    private String resolveApiKey() {
        return properties.getConsole().getLlm().resolveApiKey();
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.deepseek.com/v1";
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/v1") || trimmed.endsWith("/v1/")) {
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }
        return trimmed.endsWith("/") ? trimmed + "v1" : trimmed + "/v1";
    }

    static String buildUserMessage(String message, String runId) {
        return StrictGroundingExecutor.buildBaseUserMessage(message, runId);
    }
}
