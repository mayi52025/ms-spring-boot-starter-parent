package com.ms.middleware.console.agent.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextAssemblerTest {

    @Mock
    private MiddlewareInsightService insightService;
    @Mock
    private RunSnapshotBuilder snapshotBuilder;
    @Mock
    private RetrievalContextProvider retrievalProvider;

    private ContextAssembler assembler;
    private MsMiddlewareProperties properties;

    @BeforeEach
    void setUp() {
        assembler = new ContextAssembler(
                insightService,
                snapshotBuilder,
                new RunContextCache(),
                retrievalProvider);
        properties = new MsMiddlewareProperties();
    }

    @Test
    void respectsCharBudgetAndBuildsHints() {
        properties.getConsole().getContext().setMaxChars(320);
        when(snapshotBuilder.build(eq("run-1"), anyInt())).thenReturn(Optional.of(new RunContextSnapshot(
                "run-1",
                AutonomyRunStatus.EXECUTING,
                "order-system",
                "MQ_DEGRADED",
                List.of("MQ失败"),
                null,
                List.of("· DETECT — 失败=3"),
                null,
                true)));
        when(retrievalProvider.retrieve(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(Optional.of("keyword-hit"));
        when(retrievalProvider.sourceLabel()).thenReturn("KEYWORD_FALLBACK");

        AgentOrchestrationDecision decision = new AgentOrchestrationDecision(
                null,
                ContextScope.RUN,
                List.of(ContextLayer.RUN_ANCHOR, ContextLayer.RUN_SNAPSHOT, ContextLayer.RETRIEVAL),
                true,
                "MQ");

        AssembledContext assembled = assembler.assemble(
                decision,
                new ConversationState(),
                "run-1",
                properties.getConsole().getContext(),
                false);

        assertTrue(assembled.textBlock().length() <= 400);
        assertFalse(assembled.contextHints().isEmpty());
    }
}
