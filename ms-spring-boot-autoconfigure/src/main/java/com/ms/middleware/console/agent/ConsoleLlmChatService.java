package com.ms.middleware.console.agent;

import com.ms.middleware.MsMiddlewareProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * LLM 对话服务：{@code llm-enabled=true} 时由 {@link com.ms.middleware.console.chat.ConsoleChatService} 委托。
 */
@Service
@ConditionalOnProperty(prefix = "ms.middleware.console", name = "llm-enabled", havingValue = "true")
public class ConsoleLlmChatService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleLlmChatService.class);

    private final MsMiddlewareProperties properties;
    private final MiddlewareInsightLangChainTools insightTools;
    private volatile ConsoleLlmAgent agent;

    public ConsoleLlmChatService(MsMiddlewareProperties properties,
                                 MiddlewareInsightLangChainTools insightTools) {
        this.properties = properties;
        this.insightTools = insightTools;
    }

    public boolean isConfigured() {
        return !resolveApiKey().isBlank();
    }

    public String chat(String message, String runId) {
        if (message == null || message.isBlank()) {
            return "请输入问题。";
        }
        if (!isConfigured()) {
            return "LLM 已启用但未配置 API Key。请设置环境变量 MS_LLM_API_KEY 或 ms.middleware.console.llm.api-key。";
        }
        try {
            ConsoleLlmAgent llmAgent = getOrCreateAgent();
            return llmAgent.chat(buildUserMessage(message, runId));
        } catch (Exception e) {
            log.warn("LLM chat failed: {}", e.getMessage());
            return "LLM 请求失败：" + e.getMessage() + "。可暂时关闭 llm-enabled 使用规则模式。";
        }
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
        if (runId != null && !runId.isBlank()) {
            return "【当前上下文 runId=" + runId.trim() + "】\n用户问题：" + message.trim();
        }
        return message.trim();
    }
}
