package com.ms.middleware.console.agent.mcp;

import com.ms.middleware.MsMiddlewareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MCP 进程级只读鉴权：与控制台 shared-secret 同级意识，不开放写配置。
 *
 * <p>期望 token 来自 {@code ms.middleware.console.mcp.auth-token} 或 {@code MS_MCP_TOKEN}；
 * 未配置时本地 Demo 放行并打 warn。已配置时要求环境变量 {@code MS_MCP_TOKEN} 与期望值匹配。</p>
 */
public final class McpTokenGuard {

    private static final Logger log = LoggerFactory.getLogger(McpTokenGuard.class);

    public static final String ENV_TOKEN = "MS_MCP_TOKEN";

    private final String expectedToken;
    private final boolean authRequired;

    public McpTokenGuard(MsMiddlewareProperties.McpProperties mcp) {
        this.expectedToken = mcp == null ? "" : nullToEmpty(mcp.resolveExpectedToken());
        this.authRequired = !this.expectedToken.isBlank();
        if (!authRequired) {
            log.warn("MCP auth-token / {} 未配置：本地 Demo 放行只读 Tool，生产请配置共享密钥", ENV_TOKEN);
        }
    }

    /** 启动时校验：已配置密钥则要求环境变量匹配，否则拒绝启动。 */
    public void assertProcessAuthorized() {
        if (!authRequired) {
            return;
        }
        String presented = nullToEmpty(System.getenv(ENV_TOKEN));
        if (!tokenMatches(expectedToken, presented)) {
            throw new IllegalStateException(
                    "MCP 鉴权失败：已配置 auth-token，但环境变量 " + ENV_TOKEN + " 缺失或不匹配");
        }
    }

    /** Tool 调用前再校验一次（防误用）。 */
    public boolean isAuthorized() {
        if (!authRequired) {
            return true;
        }
        return tokenMatches(expectedToken, nullToEmpty(System.getenv(ENV_TOKEN)));
    }

    public boolean isAuthRequired() {
        return authRequired;
    }

    static boolean tokenMatches(String configured, String presented) {
        if (configured == null || presented == null) {
            return false;
        }
        byte[] expected = configured.getBytes(StandardCharsets.UTF_8);
        byte[] actual = presented.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
