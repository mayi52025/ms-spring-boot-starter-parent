package com.ms.middleware.console.agent.grounding;

import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrictGroundingExecutorTest {

    @Mock
    private MiddlewareInsightTool insightTool;

    private InsightToolGateway gateway;
    private StrictGroundingExecutor executor;

    @BeforeEach
    void setUp() {
        gateway = new InsightToolGateway(insightTool);
        executor = new StrictGroundingExecutor(new GroundingPolicy());
    }

    @Test
    void strictModePrefetchesActiveIssuesBeforeLlm() {
        when(insightTool.listActiveIssues()).thenReturn("无活跃故障");

        try (InsightToolGateway.AuditScope ignored = gateway.openAudit()) {
            StrictGroundingExecutor.PreparedContext prepared =
                    executor.prepare("当前有什么问题", null, GroundingMode.STRICT, gateway);

            assertTrue(prepared.userMessage().contains("无活跃故障"));
            assertTrue(prepared.prefetchedEvidence().contains("listActiveIssues"));
            verify(insightTool).listActiveIssues();
        }
    }

    @Test
    void relaxedModeDoesNotPrefetch() {
        StrictGroundingExecutor.PreparedContext prepared =
                executor.prepare("当前有什么问题", null, GroundingMode.RELAXED, gateway);

        assertTrue(prepared.prefetchedEvidence().isBlank());
        assertTrue(prepared.userMessage().contains("当前有什么问题"));
    }
}
