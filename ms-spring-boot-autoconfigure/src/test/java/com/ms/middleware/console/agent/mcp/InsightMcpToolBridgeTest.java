package com.ms.middleware.console.agent.mcp;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsightMcpToolBridgeTest {

    @Test
    void registersThreeReadOnlyTools() {
        MiddlewareInsightTool tool = mock(MiddlewareInsightTool.class);
        when(tool.listActiveIssues()).thenReturn("当前没有进行中的自治事件，中间件状态正常。");
        when(tool.getMetricsSummary()).thenReturn("缓存命中率=99.0%, MQ失败=0");
        when(tool.describeRun("run-1")).thenReturn("runId=run-1 状态=STABLE");

        MsMiddlewareProperties.McpProperties mcp = new MsMiddlewareProperties.McpProperties();
        McpTokenGuard guard = new McpTokenGuard(mcp);
        InsightMcpToolBridge bridge = new InsightMcpToolBridge(tool, guard);

        List<McpServerFeatures.SyncToolSpecification> specs = bridge.toolSpecifications();
        assertThat(specs).hasSize(3);
        assertThat(specs).extracting(s -> s.tool().name())
                .containsExactly(
                        InsightMcpToolBridge.LIST_ACTIVE_ISSUES,
                        InsightMcpToolBridge.DESCRIBE_RUN,
                        InsightMcpToolBridge.GET_METRICS_SUMMARY);

        McpSchema.CallToolResult issues = invoke(specs.get(0), Map.of());
        assertThat(issues.content()).isNotEmpty();
        assertThat(((McpSchema.TextContent) issues.content().get(0)).text())
                .contains("没有进行中的自治事件");

        McpSchema.CallToolResult run = invoke(specs.get(1), Map.of("runId", "run-1"));
        assertThat(((McpSchema.TextContent) run.content().get(0)).text()).contains("run-1");

        McpSchema.CallToolResult metrics = invoke(specs.get(2), Map.of());
        assertThat(((McpSchema.TextContent) metrics.content().get(0)).text()).contains("缓存命中率");
    }

    private static McpSchema.CallToolResult invoke(
            McpServerFeatures.SyncToolSpecification spec,
            Map<String, Object> args) {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(spec.tool().name(), args);
        return spec.callHandler().apply(null, request);
    }
}
