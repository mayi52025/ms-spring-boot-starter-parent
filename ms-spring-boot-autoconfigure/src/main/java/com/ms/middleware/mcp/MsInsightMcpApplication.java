package com.ms.middleware.mcp;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.tool.MiddlewareInsightTool;
import com.ms.middleware.console.agent.mcp.McpTokenGuard;
import com.ms.middleware.console.agent.mcp.MsInsightMcpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Phase 5.5：独立 stdio MCP 进程入口。
 *
 * <p>与 order-system 同 Redis/账本配置时，进程内直调 {@link MiddlewareInsightTool}；
 * 不嵌入业务 Web JVM。Cursor 通过 {@code middleware-demo/mcp/run-mcp.ps1} 拉起。</p>
 *
 * <p>仅扫描本包；Autonomy / Redis 等由 {@code AutoConfiguration.imports} 装配。
 * 排除 DataSource 自动配置（classpath 上有 JDBC 是为 RAG，MCP 只读进程不需要库）。</p>
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MsInsightMcpApplication {

    public static void main(String[] args) {
        // stdio MCP：禁止 banner / 启动日志污染 stdout
        System.setProperty("spring.main.banner-mode", "off");
        if (System.getProperty("logging.config") == null) {
            System.setProperty("logging.config", "classpath:mcp-logback.xml");
        }

        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(MsInsightMcpApplication.class)
                .web(WebApplicationType.NONE)
                // 默认属性优先级低于 application.yml；拉起本 main 即启动 MCP
                .properties(
                        "spring.cloud.compatibility-verifier.enabled=false",
                        "ms.middleware.console.enabled=false",
                        "ms.middleware.console.llm-enabled=false",
                        "ms.middleware.console.rag.enabled=false"
                )
                .run(args);

        try {
            MiddlewareInsightTool insightTool = ctx.getBean(MiddlewareInsightTool.class);
            MsMiddlewareProperties properties = ctx.getBean(MsMiddlewareProperties.class);
            McpTokenGuard guard = new McpTokenGuard(properties.getConsole().getMcp());
            MsInsightMcpServer.startAndBlock(insightTool, guard);
        } finally {
            SpringApplication.exit(ctx);
        }
    }
}
