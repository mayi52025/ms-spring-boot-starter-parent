package com.ms.middleware.console.auth;

import com.ms.middleware.MsMiddlewareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleAuthSupportTest {

    private ConsoleAuthSupport support;
    private MsMiddlewareProperties.ConsoleProperties console;

    @BeforeEach
    void setUp() {
        support = new ConsoleAuthSupport();
        console = new MsMiddlewareProperties.ConsoleProperties();
        console.setEnabled(true);
        console.setBasePath("/ms-console");
    }

    @Test
    void authRequiredWhenTokenConfigured() {
        console.setAuthToken("secret");
        assertTrue(support.isAuthRequired(console));
    }

    @Test
    void authNotRequiredWhenTokenBlank() {
        console.setAuthToken("");
        assertFalse(support.isAuthRequired(console));
    }

    @Test
    void resolvesTokenFromHeaderFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ConsoleAuthSupport.HEADER_NAME, "header-token");
        request.setParameter(ConsoleAuthSupport.QUERY_PARAM, "query-token");
        assertEquals("header-token", support.resolveToken(request));
    }

    @Test
    void tokenMatchesUsesConstantTimeComparison() {
        assertTrue(support.tokenMatches("abc", "abc"));
        assertFalse(support.tokenMatches("abc", "abd"));
    }

    @Test
    void detectsProtectedApiPath() {
        assertTrue(support.isProtectedApiPath(console, "/ms-console/api/issues"));
        assertFalse(support.isProtectedApiPath(console, "/ms-console/index.html"));
    }
}
