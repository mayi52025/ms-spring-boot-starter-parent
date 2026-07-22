package com.ms.middleware.console.agent.mcp;

import com.ms.middleware.MsMiddlewareProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpTokenGuardTest {

    @Test
    void blankTokenAllowsLocalDemoWhenEnvAbsent() {
        // 仅当本机未设置 MS_MCP_TOKEN 时验证「放行」路径
        if (System.getenv(McpTokenGuard.ENV_TOKEN) != null
                && !System.getenv(McpTokenGuard.ENV_TOKEN).isBlank()) {
            return;
        }
        MsMiddlewareProperties.McpProperties mcp = new MsMiddlewareProperties.McpProperties();
        mcp.setAuthToken("");
        McpTokenGuard guard = new McpTokenGuard(mcp);
        assertThat(guard.isAuthRequired()).isFalse();
        assertThat(guard.isAuthorized()).isTrue();
        guard.assertProcessAuthorized();
    }

    @Test
    void tokenMatchesIsConstantTimeEquality() {
        assertThat(McpTokenGuard.tokenMatches("demo-secret", "demo-secret")).isTrue();
        assertThat(McpTokenGuard.tokenMatches("demo-secret", "wrong")).isFalse();
        assertThat(McpTokenGuard.tokenMatches("demo-secret", null)).isFalse();
    }

    @Test
    void configuredTokenWithoutMatchingEnvFailsAssert() {
        MsMiddlewareProperties.McpProperties mcp = new MsMiddlewareProperties.McpProperties();
        mcp.setAuthToken("ms-mcp-test-token-that-must-not-match-env-9f3a");
        McpTokenGuard guard = new McpTokenGuard(mcp);
        assertThat(guard.isAuthRequired()).isTrue();
        if (!"ms-mcp-test-token-that-must-not-match-env-9f3a"
                .equals(System.getenv(McpTokenGuard.ENV_TOKEN))) {
            assertThat(guard.isAuthorized()).isFalse();
            assertThatThrownBy(guard::assertProcessAuthorized)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(McpTokenGuard.ENV_TOKEN);
        }
    }
}
