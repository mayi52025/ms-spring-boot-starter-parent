package com.ms.middleware.console.chat;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import com.ms.middleware.console.agent.ConsoleLlmChatResult;
import com.ms.middleware.console.agent.ConsoleLlmChatService;
import com.ms.middleware.console.agent.context.ConsoleChatContextOrchestrator;
import com.ms.middleware.console.agent.context.ConversationStateStore;
import com.ms.middleware.console.agent.context.ContextAssembler;
import com.ms.middleware.console.agent.context.AgentOrchestrationPolicy;
import com.ms.middleware.console.agent.context.RunContextCache;
import com.ms.middleware.console.agent.context.RunSnapshotBuilder;
import com.ms.middleware.console.agent.context.TestRetrievalContextProviders;
import com.ms.middleware.console.agent.grounding.GroundingPolicy;
import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.console.api.ConsoleChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Mock
    private MiddlewareInsightService insightService;
    @Mock
    private RunSnapshotBuilder snapshotBuilder;

    private ConsoleChatService chatService;

    @BeforeEach
    void setUp() {
        GroundingPolicy policy = new GroundingPolicy();
        InsightToolGateway gateway = new InsightToolGateway(insightTool);
        ContextAssembler assembler = new ContextAssembler(
                insightService,
                snapshotBuilder,
                new RunContextCache(),
                TestRetrievalContextProviders.empty());
        ConsoleChatContextOrchestrator orchestrator = new ConsoleChatContextOrchestrator(
                new MsMiddlewareProperties(),
                insightService,
                new AgentOrchestrationPolicy(policy, insightService),
                new ConversationStateStore(),
                assembler);
        when(llmChatServiceProvider.getIfAvailable()).thenReturn(llmChatService);
        chatService = new ConsoleChatService(policy, gateway, orchestrator, properties, llmChatServiceProvider);
    }

    @Test
    void delegatesToLlmWhenEnabled() {
        when(properties.getConsole()).thenReturn(console);
        when(console.isLlmEnabled()).thenReturn(true);
        when(llmChatService.chat("当前有什么问题", null, "sess-1"))
                .thenReturn(ConsoleLlmChatResult.of(
                        "无活跃故障",
                        List.of("listActiveIssues"),
                        true,
                        List.of("dialogState"),
                        "run-1"));
        when(llmChatService.isConfigured()).thenReturn(true);

        ConsoleChatResponse response = chatService.chat("当前有什么问题", null, "sess-1");

        assertTrue(response.isLlmEnabled());
        assertEquals(List.of("listActiveIssues"), response.getToolsUsed());
        assertEquals(List.of("dialogState"), response.getContextHints());
        assertTrue(response.isGrounded());
        verify(llmChatService).chat("当前有什么问题", null, "sess-1");
    }

    @Test
    void usesRuleModeWhenLlmDisabled() {
        when(properties.getConsole()).thenReturn(console);
        when(console.isLlmEnabled()).thenReturn(false);
        when(console.getLlm()).thenReturn(new MsMiddlewareProperties.LlmProperties());
        when(insightTool.listActiveIssues()).thenReturn("正常");

        ConsoleChatResponse response = chatService.chat("当前有什么问题", null, "sess-2");

        assertFalse(response.isLlmEnabled());
        assertEquals(List.of("listActiveIssues"), response.getToolsUsed());
        verify(insightTool).listActiveIssues();
    }
}
