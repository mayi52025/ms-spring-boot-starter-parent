package com.ms.middleware.console.auth;

import com.ms.middleware.MsMiddlewareProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 控制台 API 鉴权过滤器（Phase 4 Step 3）。
 *
 * <p>当 {@code ms.middleware.console.auth-token} 非空时，保护 {@code {base-path}/api/**}；
 * 支持 Header {@link ConsoleAuthSupport#HEADER_NAME} 或 query {@code token}（SSE 用）。</p>
 * <p>auth-token 为空时不生效，兼容本地 Demo。</p>
 */
public class ConsoleAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleAuthFilter.class);

    private final MsMiddlewareProperties properties;
    private final ConsoleAuthSupport authSupport;

    public ConsoleAuthFilter(MsMiddlewareProperties properties, ConsoleAuthSupport authSupport) {
        this.properties = properties;
        this.authSupport = authSupport;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        MsMiddlewareProperties.ConsoleProperties console = properties.getConsole();
        if (!authSupport.isAuthRequired(console)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (authSupport.isPublicAuthStatusPath(console, uri)) {
            return true;
        }
        return !authSupport.isProtectedApiPath(console, uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MsMiddlewareProperties.ConsoleProperties console = properties.getConsole();
        String presented = authSupport.resolveToken(request);
        if (authSupport.tokenMatches(console.getAuthToken(), presented)) {
            filterChain.doFilter(request, response);
            return;
        }
        logger.debug("Console API unauthorized: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid console token\"}");
    }
}
