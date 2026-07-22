package com.ms.middleware.console.agent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 最小只读 MCP Server：stdio JSON-RPC，工具委托 {@link MiddlewareInsightTool}。
 *
 * <p>禁止向 stdout 打业务日志（会污染协议流）；日志走 stderr（见 {@code mcp-logback.xml}）。</p>
 */
public final class MsInsightMcpServer {

    private static final Logger log = LoggerFactory.getLogger(MsInsightMcpServer.class);

    public static final String SERVER_NAME = "ms-insight-mcp";
    public static final String SERVER_VERSION = "1.0.0";

    private MsInsightMcpServer() {
    }

    /**
     * 注册 3 个只读 Tool 并阻塞直至进程结束（生产 / Cursor 入口）。
     */
    public static void startAndBlock(MiddlewareInsightTool insightTool, McpTokenGuard tokenGuard) {
        McpSyncServer server = start(insightTool, tokenGuard, System.in, System.out);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeQuietly(server), "ms-insight-mcp-shutdown"));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closeQuietly(server);
        }
    }

    /**
     * 启动 stdio Server（可注入流，供烟雾测试）；不阻塞。
     */
    public static McpSyncServer start(MiddlewareInsightTool insightTool,
                                      McpTokenGuard tokenGuard,
                                      InputStream input,
                                      OutputStream output) {
        if (insightTool == null) {
            throw new IllegalArgumentException("MiddlewareInsightTool 不能为空");
        }
        if (tokenGuard == null) {
            throw new IllegalArgumentException("McpTokenGuard 不能为空");
        }
        if (input == null || output == null) {
            throw new IllegalArgumentException("input/output 不能为空");
        }
        tokenGuard.assertProcessAuthorized();

        InsightMcpToolBridge bridge = new InsightMcpToolBridge(insightTool, tokenGuard);
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper, input, output);

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .instructions("MS Middleware 只读洞察 MCP：list_active_issues / describe_run / get_metrics_summary。"
                        + " 无写操作；采纳与 PUBLISH 请走控制台。")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .jsonMapper(jsonMapper)
                .tools(bridge.toolSpecifications())
                .build();

        log.info("MCP stdio server started: {} v{} tools={}",
                SERVER_NAME, SERVER_VERSION, bridge.toolSpecifications().size());
        return server;
    }

    private static void closeQuietly(McpSyncServer server) {
        if (server == null) {
            return;
        }
        try {
            server.closeGracefully();
        } catch (Exception ignored) {
            server.close();
        }
    }
}
