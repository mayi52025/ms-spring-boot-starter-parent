package com.ms.middleware.console.chat;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import com.ms.middleware.console.agent.ConsoleLlmChatService;
import com.ms.middleware.console.api.ConsoleChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsoleChatServiceLlmTest {

    @Mock
    private MiddlewareInsightTool insightTool;
    @Mock
    private MsMiddlewareProperties properties;
    @Mock
    private ObjectProvider<ConsoleLlmChatService> llmChatServiceProvider;
    @Mock
    private ConsoleLlmChatService llmChatService;
    @Mock
    private MsMiddlewareProperties.ConsoleProperties console;

    private ConsoleChatService chatService;

    @BeforeEach
    void setUp() {
        when(llmChatServiceProvider.getIfAvailable()).thenReturn(llmChatService);
        chatService = new ConsoleChatService(insightTool, properties, llmChatServiceProvider);
    }

    @Test
    void delegatesToLlmWhenEnabled() {
        when(properties.getConsole()).thenReturn(console);
        when(console.isLlmEnabled()).thenReturn(true);
        when(llmChatService.chat("当前有什么问题", null)).thenReturn("无活跃故障");
        when(llmChatService.isConfigured()).thenReturn(true);

        ConsoleChatResponse response = chatService.chat("当前有什么问题", null);

        assertTrue(response.isLlmEnabled());
        verify(llmChatService).chat("当前有什么问题", null);
    }

    @Test
    void usesRuleModeWhenLlmDisabled() {
        when(properties.getConsole()).thenReturn(console);
        when(console.isLlmEnabled()).thenReturn(false);
        when(insightTool.listActiveIssues()).thenReturn("正常");

        ConsoleChatResponse response = chatService.chat("当前有什么问题", null);

        assertFalse(response.isLlmEnabled());
        verify(insightTool).listActiveIssues();
    }
}
