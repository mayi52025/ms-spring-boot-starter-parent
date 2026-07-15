package com.ms.middleware.console.agent;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import com.ms.middleware.console.agent.grounding.GroundingPolicy;
import com.ms.middleware.console.agent.grounding.GroundingValidator;
import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import com.ms.middleware.console.agent.grounding.StrictGroundingExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ConsoleLlmChatServiceTest {

    @Mock
    private MiddlewareInsightTool insightTool;

    private ConsoleLlmChatService service;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        InsightToolGateway gateway = new InsightToolGateway(insightTool);
        GroundingPolicy policy = new GroundingPolicy();
        service = new ConsoleLlmChatService(
                properties,
                new MiddlewareInsightLangChainTools(gateway),
                policy,
                gateway,
                new StrictGroundingExecutor(policy),
                new GroundingValidator());
    }

    @Test
    void normalizeBaseUrlAppendsV1() {
        assertEquals("https://api.deepseek.com/v1", ConsoleLlmChatService.normalizeBaseUrl("https://api.deepseek.com"));
        assertEquals("https://api.deepseek.com/v1", ConsoleLlmChatService.normalizeBaseUrl("https://api.deepseek.com/"));
        assertEquals("https://api.deepseek.com/v1", ConsoleLlmChatService.normalizeBaseUrl("https://api.deepseek.com/v1"));
    }

    @Test
    void buildUserMessageIncludesRunId() {
        String msg = ConsoleLlmChatService.buildUserMessage("为何 STABLE？", "run-abc");
        assertTrue(msg.contains("run-abc"));
        assertTrue(msg.contains("为何 STABLE"));
    }

    @Test
    void isConfiguredFalseWithoutKey() {
        ConsoleLlmChatResult result = service.chat("hello", null);
        assertFalse(service.isConfigured());
        assertTrue(result.reply().contains("API Key"));
    }

    @Test
    void isConfiguredTrueWithKey() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getConsole().getLlm().setApiKey("sk-test");
        InsightToolGateway gateway = new InsightToolGateway(insightTool);
        GroundingPolicy policy = new GroundingPolicy();
        ConsoleLlmChatService configured = new ConsoleLlmChatService(
                properties,
                new MiddlewareInsightLangChainTools(gateway),
                policy,
                gateway,
                new StrictGroundingExecutor(policy),
                new GroundingValidator());
        assertTrue(configured.isConfigured());
    }
}
