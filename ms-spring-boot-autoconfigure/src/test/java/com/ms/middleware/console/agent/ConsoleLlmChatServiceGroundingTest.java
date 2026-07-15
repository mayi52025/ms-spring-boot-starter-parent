package com.ms.middleware.console.agent;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import com.ms.middleware.console.agent.context.ConsoleChatContextOrchestrator;
import com.ms.middleware.console.agent.context.ConversationStateStore;
import com.ms.middleware.console.agent.context.ContextAssembler;
import com.ms.middleware.console.agent.context.AgentOrchestrationPolicy;
import com.ms.middleware.console.agent.context.RunContextCache;
import com.ms.middleware.console.agent.context.RunSnapshotBuilder;
import com.ms.middleware.console.agent.context.TestRetrievalContextProviders;
import com.ms.middleware.console.agent.grounding.GroundingPolicy;
import com.ms.middleware.console.agent.grounding.GroundingValidator;
import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import com.ms.middleware.console.agent.grounding.StrictGroundingExecutor;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsoleLlmChatServiceGroundingTest {

    @Mock
    private MiddlewareInsightTool insightTool;
    @Mock
    private ConsoleLlmAgent llmAgent;
    @Mock
    private MiddlewareInsightService insightService;
    @Mock
    private RunSnapshotBuilder snapshotBuilder;

    private ConsoleLlmChatService service;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getConsole().setLlmEnabled(true);
        properties.getConsole().getLlm().setApiKey("sk-test");
        properties.getConsole().getLlm().setGroundingMode("strict");

        InsightToolGateway gateway = new InsightToolGateway(insightTool);
        GroundingPolicy policy = new GroundingPolicy();
        StrictGroundingExecutor executor = new StrictGroundingExecutor(policy);
        ContextAssembler assembler = new ContextAssembler(
                insightService,
                snapshotBuilder,
                new RunContextCache(),
                TestRetrievalContextProviders.empty());
        ConsoleChatContextOrchestrator contextOrchestrator = new ConsoleChatContextOrchestrator(
                properties,
                insightService,
                new AgentOrchestrationPolicy(policy, insightService),
                new ConversationStateStore(),
                assembler);

        service = new ConsoleLlmChatService(
                properties,
                new MiddlewareInsightLangChainTools(gateway),
                gateway,
                executor,
                new GroundingValidator(),
                contextOrchestrator);
        service.overrideAgentForTest(llmAgent);
    }

    @Test
    void strictModeUsesPrefetchedEvidenceWhenLlmReturnsEmpty() {
        when(insightTool.listActiveIssues()).thenReturn("无活跃故障");
        when(llmAgent.chat(org.mockito.ArgumentMatchers.anyString())).thenReturn("");

        ConsoleLlmChatResult result = service.chat("当前有什么问题", null, "sess-1");

        assertFalse(result.grounded());
        assertTrue(result.reply().contains("无活跃故障"));
        assertEquals(List.of("listActiveIssues"), result.toolsUsed());
    }

    @Test
    void strictModeReturnsLlmSummaryWhenGrounded() {
        when(insightTool.getMetricsSummary()).thenReturn("mqFailedCount=0");
        when(llmAgent.chat(org.mockito.ArgumentMatchers.anyString())).thenReturn("当前 MQ 失败为 0");

        ConsoleLlmChatResult result = service.chat("看一下指标", null, "sess-2");

        assertTrue(result.grounded());
        assertEquals("当前 MQ 失败为 0", result.reply());
        assertEquals(List.of("getMetricsSummary"), result.toolsUsed());
    }
}
