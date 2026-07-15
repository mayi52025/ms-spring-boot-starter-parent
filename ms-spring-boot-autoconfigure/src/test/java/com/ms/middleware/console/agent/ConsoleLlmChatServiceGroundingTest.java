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

import java.util.List;

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

    private MsMiddlewareProperties properties;
    private ConsoleLlmChatService service;

    @BeforeEach
    void setUp() {
        properties = new MsMiddlewareProperties();
        properties.getConsole().setLlmEnabled(true);
        properties.getConsole().getLlm().setApiKey("sk-test");
        properties.getConsole().getLlm().setGroundingMode("strict");

        InsightToolGateway gateway = new InsightToolGateway(insightTool);
        GroundingPolicy policy = new GroundingPolicy();
        StrictGroundingExecutor executor = new StrictGroundingExecutor(policy);
        MiddlewareInsightLangChainTools tools = new MiddlewareInsightLangChainTools(gateway);

        service = new ConsoleLlmChatService(
                properties,
                tools,
                policy,
                gateway,
                executor,
                new GroundingValidator());
        service.overrideAgentForTest(llmAgent);
    }

    @Test
    void strictModeUsesPrefetchedEvidenceWhenLlmReturnsEmpty() {
        when(insightTool.listActiveIssues()).thenReturn("无活跃故障");
        when(llmAgent.chat(org.mockito.ArgumentMatchers.anyString())).thenReturn("");

        ConsoleLlmChatResult result = service.chat("当前有什么问题", null);

        assertFalse(result.grounded());
        assertTrue(result.reply().contains("无活跃故障"));
        assertEquals(List.of("listActiveIssues"), result.toolsUsed());
    }

    @Test
    void strictModeReturnsLlmSummaryWhenGrounded() {
        when(insightTool.getMetricsSummary()).thenReturn("mqFailedCount=0");
        when(llmAgent.chat(org.mockito.ArgumentMatchers.anyString())).thenReturn("当前 MQ 失败为 0");

        ConsoleLlmChatResult result = service.chat("看一下指标", null);

        assertTrue(result.grounded());
        assertEquals("当前 MQ 失败为 0", result.reply());
        assertEquals(List.of("getMetricsSummary"), result.toolsUsed());
    }
}
