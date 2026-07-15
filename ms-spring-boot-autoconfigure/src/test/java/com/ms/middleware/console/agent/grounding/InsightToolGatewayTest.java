package com.ms.middleware.console.agent.grounding;

import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightToolGatewayTest {

    @Mock
    private MiddlewareInsightTool insightTool;

    private InsightToolGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new InsightToolGateway(insightTool);
    }

    @Test
    void recordsToolCallsInAudit() {
        when(insightTool.listActiveIssues()).thenReturn("无活跃故障");

        try (InsightToolGateway.AuditScope ignored = gateway.openAudit()) {
            String result = gateway.listActiveIssues();

            assertEquals("无活跃故障", result);
            assertEquals(List.of("listActiveIssues"), gateway.currentToolNames());
            assertEquals(1, gateway.currentRecords().size());
            assertEquals(InsightToolName.LIST_ACTIVE_ISSUES, gateway.currentRecords().get(0).tool());
        }
    }

    @Test
    void executeRequiredToolsForResolution() {
        when(insightTool.getMetricsSummary()).thenReturn("mqFailedCount=0");

        GroundingResolution resolution = new GroundingResolution(
                GroundingIntent.METRICS,
                List.of(InsightToolInvocation.of(InsightToolName.GET_METRICS_SUMMARY)));

        try (InsightToolGateway.AuditScope ignored = gateway.openAudit()) {
            String evidence = gateway.executeRequiredTools(resolution);

            assertTrue(evidence.contains("getMetricsSummary"));
            assertTrue(evidence.contains("mqFailedCount=0"));
            verify(insightTool).getMetricsSummary();
        }
    }
}
