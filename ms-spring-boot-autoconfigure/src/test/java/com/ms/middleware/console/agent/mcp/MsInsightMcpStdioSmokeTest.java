package com.ms.middleware.console.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真 stdio JSON-RPC 烟雾：不启 Spring / Redis，证明 list_active_issues 可经 MCP 调通。
 */
class MsInsightMcpStdioSmokeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void listActiveIssuesOverStdio() throws Exception {
        MiddlewareInsightTool tool = mock(MiddlewareInsightTool.class);
        when(tool.listActiveIssues()).thenReturn("当前没有进行中的自治事件，中间件状态正常。");

        MsMiddlewareProperties.McpProperties mcpProps = new MsMiddlewareProperties.McpProperties();
        mcpProps.setAuthToken("");
        McpTokenGuard guard = new McpTokenGuard(mcpProps);

        PipedOutputStream clientToServer = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientToServer, 64 * 1024);
        PipedInputStream serverToClient = new PipedInputStream(64 * 1024);
        PipedOutputStream serverOut = new PipedOutputStream(serverToClient);

        AtomicReference<McpSyncServer> serverRef = new AtomicReference<>();
        Thread serverThread = new Thread(() -> {
            McpSyncServer server = MsInsightMcpServer.start(tool, guard, serverIn, serverOut);
            serverRef.set(server);
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "mcp-smoke-server");
        serverThread.setDaemon(true);
        serverThread.start();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientToServer, StandardCharsets.UTF_8), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(serverToClient, StandardCharsets.UTF_8))) {

            ObjectNode clientInfo = MAPPER.createObjectNode();
            clientInfo.put("name", "ms-mcp-smoke");
            clientInfo.put("version", "1.0.0");

            ObjectNode initParams = MAPPER.createObjectNode();
            initParams.put("protocolVersion", ProtocolVersions.MCP_2024_11_05);
            initParams.set("capabilities", MAPPER.createObjectNode());
            initParams.set("clientInfo", clientInfo);

            writer.println(jsonRpc(1, "initialize", initParams));
            JsonNode initResult = readResponse(reader, 1, 10);
            assertThat(initResult.path("result").path("serverInfo").path("name").asText())
                    .isEqualTo(MsInsightMcpServer.SERVER_NAME);

            writer.println(notification("notifications/initialized", MAPPER.createObjectNode()));

            ObjectNode callParams = MAPPER.createObjectNode();
            callParams.put("name", InsightMcpToolBridge.LIST_ACTIVE_ISSUES);
            callParams.set("arguments", MAPPER.createObjectNode());
            writer.println(jsonRpc(2, "tools/call", callParams));

            JsonNode callResult = readResponse(reader, 2, 10);
            String text = callResult.path("result").path("content").path(0).path("text").asText();
            assertThat(text).contains("没有进行中的自治事件");
            assertThat(callResult.path("result").path("isError").asBoolean(false)).isFalse();
        } finally {
            McpSyncServer server = serverRef.get();
            if (server != null) {
                try {
                    server.closeGracefully();
                } catch (Exception ignored) {
                    server.close();
                }
            }
            serverThread.interrupt();
        }
    }

    private static String jsonRpc(int id, String method, JsonNode params) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("jsonrpc", "2.0");
        root.put("id", id);
        root.put("method", method);
        root.set("params", params);
        return MAPPER.writeValueAsString(root);
    }

    private static String notification(String method, JsonNode params) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("jsonrpc", "2.0");
        root.put("method", method);
        root.set("params", params);
        return MAPPER.writeValueAsString(root);
    }

    private static JsonNode readResponse(BufferedReader reader, int expectId, int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        ByteArrayOutputStream ignored = new ByteArrayOutputStream();
        while (System.nanoTime() < deadline) {
            if (!reader.ready()) {
                Thread.sleep(20);
                continue;
            }
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                continue;
            }
            JsonNode node = MAPPER.readTree(line);
            if (node.has("id") && node.get("id").asInt() == expectId) {
                return node;
            }
            ignored.write(line.getBytes(StandardCharsets.UTF_8));
        }
        throw new AssertionError("timeout waiting for json-rpc id=" + expectId
                + " other=" + ignored.toString(StandardCharsets.UTF_8));
    }
}
