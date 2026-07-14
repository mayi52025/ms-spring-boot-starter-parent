package com.ms.middleware.console.auth;

import com.ms.middleware.MsMiddlewareProperties;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 控制台 shared-secret 鉴权辅助：token 提取与恒定时间比较。
 */
public class ConsoleAuthSupport {

    /** HTTP Header：{@code X-MS-Console-Token} */
    public static final String HEADER_NAME = "X-MS-Console-Token";
    /** Query 参数名，供 SSE EventSource 等无法自定义 Header 的场景 */
    public static final String QUERY_PARAM = "token";

    /**
     * 是否启用控制台鉴权：console 已开且 auth-token 非空。
     */
    public boolean isAuthRequired(MsMiddlewareProperties.ConsoleProperties console) {
        return console != null && console.isEnabled() && hasConfiguredToken(console);
    }

    public boolean hasConfiguredToken(MsMiddlewareProperties.ConsoleProperties console) {
        return console.getAuthToken() != null && !console.getAuthToken().isBlank();
    }

    /**
     * 从 Header 或 query 解析 token；Header 优先。
     */
    public String resolveToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String query = request.getParameter(QUERY_PARAM);
        if (query != null && !query.isBlank()) {
            return query.trim();
        }
        return null;
    }

    /** 恒定时间比较，避免 timing 侧信道 */
    public boolean tokenMatches(String configured, String presented) {
        if (configured == null || presented == null) {
            return false;
        }
        byte[] expected = configured.getBytes(StandardCharsets.UTF_8);
        byte[] actual = presented.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /** 规范化 base-path，确保以 / 开头且不以 / 结尾 */
    public String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return "/ms-console";
        }
        String normalized = basePath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public String apiPathPrefix(MsMiddlewareProperties.ConsoleProperties console) {
        return normalizeBasePath(console.getBasePath()) + "/api/";
    }

    public boolean isProtectedApiPath(MsMiddlewareProperties.ConsoleProperties console, String requestUri) {
        if (requestUri == null) {
            return false;
        }
        String prefix = apiPathPrefix(console);
        return requestUri.startsWith(prefix);
    }

    public boolean isPublicAuthStatusPath(MsMiddlewareProperties.ConsoleProperties console, String requestUri) {
        if (requestUri == null) {
            return false;
        }
        return requestUri.equals(apiPathPrefix(console) + "auth/status")
                || requestUri.endsWith("/api/auth/status");
    }
}
