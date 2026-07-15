package com.ms.middleware.console.agent;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.console.agent.grounding.GroundingMode;
import com.ms.middleware.console.agent.grounding.GroundingPolicy;
import com.ms.middleware.console.agent.grounding.GroundingResolution;
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
 * LLM 对话服务：{@code llm-enabled=true} 时由 {@link com.ms.middleware.console.chat.ConsoleChatService} 委托。
 */
@Service
@ConditionalOnProperty(prefix = "ms.middleware.console", name = "llm-enabled", havingValue = "true")
public class ConsoleLlmChatService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleLlmChatService.class);

    private final MsMiddlewareProperties properties;
    private final MiddlewareInsightLangChainTools insightTools;
    private final GroundingPolicy groundingPolicy;
    private final InsightToolGateway insightToolGateway;
    private final StrictGroundingExecutor strictGroundingExecutor;
    private final GroundingValidator groundingValidator;
    private volatile ConsoleLlmAgent agent;

    public ConsoleLlmChatService(MsMiddlewareProperties properties,
                                 MiddlewareInsightLangChainTools insightTools,
                                 GroundingPolicy groundingPolicy,
                                 InsightToolGateway insightToolGateway,
                                 StrictGroundingExecutor strictGroundingExecutor,
                                 GroundingValidator groundingValidator) {
        this.properties = properties;
        this.insightTools = insightTools;
        this.groundingPolicy = groundingPolicy;
        this.insightToolGateway = insightToolGateway;
        this.strictGroundingExecutor = strictGroundingExecutor;
        this.groundingValidator = groundingValidator;
    }

    public boolean isConfigured() {
        return !resolveApiKey().isBlank();
    }

    public ConsoleLlmChatResult chat(String message, String runId) {
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
        GroundingResolution resolution = groundingPolicy.resolve(message, runId);

        try (InsightToolGateway.AuditScope ignored = insightToolGateway.openAudit()) {
            StrictGroundingExecutor.PreparedContext prepared =
                    strictGroundingExecutor.prepare(message, runId, mode, insightToolGateway);
            try {
                ConsoleLlmAgent llmAgent = getOrCreateAgent();
                String llmReply = llmAgent.chat(prepared.userMessage());
                List<String> toolsUsed = insightToolGateway.currentToolNames();
                GroundingValidator.ValidationResult validation = groundingValidator.validate(
                        llmReply,
                        toolsUsed,
                        resolution,
                        mode,
                        prepared.prefetchedEvidence());
                if (!validation.grounded()) {
                    return ConsoleLlmChatResult.of(validation.fallbackReply(), toolsUsed, false);
                }
                boolean grounded = !resolution.opsQuestion() || !toolsUsed.isEmpty();
                return ConsoleLlmChatResult.of(llmReply, toolsUsed, grounded);
            } catch (Exception e) {
                log.warn("LLM chat failed: {}", e.getMessage());
                if (mode == GroundingMode.STRICT
                        && resolution.opsQuestion()
                        && prepared.prefetchedEvidence() != null
                        && !prepared.prefetchedEvidence().isBlank()) {
                    List<String> toolsUsed = insightToolGateway.currentToolNames();
                    return ConsoleLlmChatResult.of(
                            "（Grounding）LLM 不可用，以下为 Insight Tool 数据：\n\n" + prepared.prefetchedEvidence(),
                            toolsUsed,
                            true);
                }
                return ConsoleLlmChatResult.of(
                        "LLM 请求失败：" + e.getMessage() + "。可暂时关闭 llm-enabled 使用规则模式。",
                        insightToolGateway.currentToolNames(),
                        false);
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
