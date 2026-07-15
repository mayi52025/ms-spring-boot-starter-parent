package com.ms.middleware.console.agent;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
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
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getConsole().setLlmEnabled(true);
        properties.getConsole().getLlm().setApiKey("");
        ConsoleLlmChatService service = new ConsoleLlmChatService(properties, new MiddlewareInsightLangChainTools(insightTool));
        assertFalse(service.isConfigured());
        assertTrue(service.chat("hello", null).contains("API Key"));
    }

    @Test
    void isConfiguredTrueWithKey() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getConsole().getLlm().setApiKey("sk-test");
        ConsoleLlmChatService service = new ConsoleLlmChatService(properties, new MiddlewareInsightLangChainTools(insightTool));
        assertTrue(service.isConfigured());
    }
}
