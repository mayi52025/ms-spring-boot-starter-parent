package com.ms.middleware.console.agent.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.agent.grounding.GroundingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsoleChatContextOrchestratorTest {

    @Mock
    private MiddlewareInsightService insightService;
    @Mock
    private RunSnapshotBuilder snapshotBuilder;

    private ConsoleChatContextOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        AgentOrchestrationPolicy policy = new AgentOrchestrationPolicy(
                new com.ms.middleware.console.agent.grounding.GroundingPolicy(),
                insightService);
        ContextAssembler assembler = new ContextAssembler(
                insightService,
                snapshotBuilder,
                new RunContextCache(),
                TestRetrievalContextProviders.empty());
        orchestrator = new ConsoleChatContextOrchestrator(
                properties,
                insightService,
                policy,
                new ConversationStateStore(),
                assembler);
    }

    @Test
    void followUpIntentAutoBindsSingleActiveRun() {
        AutonomyRun run = new AutonomyRun();
        run.setRunId("run-only");
        when(insightService.listActiveRuns()).thenReturn(List.of(run));

        var prepared = orchestrator.prepare("为何还没 STABLE", null, "sess-1", GroundingMode.RELAXED);

        assertEquals("run-only", prepared.effectiveRunId());
        assertTrue(prepared.messageForGrounding().contains("run-only"));
    }

    @Test
    void homeGlobalQuestionDoesNotAutoBind() {
        var prepared = orchestrator.prepare("当前有什么问题", null, "sess-1", GroundingMode.RELAXED);

        assertNull(prepared.effectiveRunId());
    }

    @Test
    void metricsQuestionDoesNotAutoBind() {
        var prepared = orchestrator.prepare("看一下指标", null, "sess-2", GroundingMode.RELAXED);

        assertNull(prepared.effectiveRunId());
    }

    @Test
    void explicitRunIdAlwaysWins() {
        var prepared = orchestrator.prepare("当前有什么问题", "run-explicit", "sess-3", GroundingMode.RELAXED);

        assertEquals("run-explicit", prepared.effectiveRunId());
    }

    @Test
    void shouldAutoBindRules() {
        assertFalse(ConsoleChatContextOrchestrator.shouldAutoBindOnEmptyRunId("当前有什么问题"));
        assertFalse(ConsoleChatContextOrchestrator.shouldAutoBindOnEmptyRunId("指标怎么样"));
        assertTrue(ConsoleChatContextOrchestrator.shouldAutoBindOnEmptyRunId("为何还没 STABLE"));
        assertTrue(ConsoleChatContextOrchestrator.shouldAutoBindOnEmptyRunId("刚才那个故障怎么了"));
    }
}
