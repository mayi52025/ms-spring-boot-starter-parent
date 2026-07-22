package com.ms.middleware.console.agent.mcp;

import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 将 {@link MiddlewareInsightTool} 的三个只读方法暴露为 MCP Tool。
 *
 * <p>与控制台 LangChain4j {@code @Tool} 共用同一契约，禁止再造洞察 API。</p>
 */
public final class InsightMcpToolBridge {

    public static final String LIST_ACTIVE_ISSUES = "list_active_issues";
    public static final String DESCRIBE_RUN = "describe_run";
    public static final String GET_METRICS_SUMMARY = "get_metrics_summary";

    private final MiddlewareInsightTool insightTool;
    private final McpTokenGuard tokenGuard;

    public InsightMcpToolBridge(MiddlewareInsightTool insightTool, McpTokenGuard tokenGuard) {
        this.insightTool = insightTool;
        this.tokenGuard = tokenGuard;
    }

    public List<McpServerFeatures.SyncToolSpecification> toolSpecifications() {
        return List.of(
                syncTool(LIST_ACTIVE_ISSUES,
                        "列出当前进行中的自治事件（只读，等同 MiddlewareInsightTool.listActiveIssues）",
                        emptyObjectSchema(),
                        (exchange, request) -> textResult(guarded(insightTool::listActiveIssues))),
                syncTool(DESCRIBE_RUN,
                        "按 runId 返回自治事件摘要（只读，等同 MiddlewareInsightTool.describeRun）",
                        describeRunSchema(),
                        (exchange, request) -> textResult(guarded(() ->
                                insightTool.describeRun(stringArg(request, "runId"))))),
                syncTool(GET_METRICS_SUMMARY,
                        "返回中间件与自治指标摘要（只读，等同 MiddlewareInsightTool.getMetricsSummary）",
                        emptyObjectSchema(),
                        (exchange, request) -> textResult(guarded(insightTool::getMetricsSummary)))
        );
    }

    private String guarded(ToolCall call) {
        if (!tokenGuard.isAuthorized()) {
            return "Unauthorized: MCP token missing or invalid（请设置环境变量 " + McpTokenGuard.ENV_TOKEN + "）";
        }
        try {
            return call.run();
        } catch (Exception ex) {
            return "Tool 执行失败: " + ex.getMessage();
        }
    }

    private static McpServerFeatures.SyncToolSpecification syncTool(
            String name,
            String description,
            Map<String, Object> inputSchema,
            BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler) {
        McpSchema.Tool tool = McpSchema.Tool.builder(name, inputSchema)
                .description(description)
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private static McpSchema.CallToolResult textResult(String text) {
        boolean error = text != null && text.startsWith("Unauthorized:");
        return McpSchema.CallToolResult.builder()
                .addTextContent(text == null ? "" : text)
                .isError(error)
                .build();
    }

    private static String stringArg(McpSchema.CallToolRequest request, String key) {
        if (request == null || request.arguments() == null) {
            return "";
        }
        Object value = request.arguments().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Map<String, Object> emptyObjectSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> describeRunSchema() {
        Map<String, Object> runId = new LinkedHashMap<>();
        runId.put("type", "string");
        runId.put("description", "自治 runId");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("runId", runId);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("runId"));
        schema.put("additionalProperties", false);
        return schema;
    }

    @FunctionalInterface
    private interface ToolCall {
        String run();
    }
}
