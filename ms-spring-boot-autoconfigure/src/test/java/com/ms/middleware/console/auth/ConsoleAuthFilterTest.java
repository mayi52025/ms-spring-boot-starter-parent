package com.ms.middleware.console.auth;

import com.ms.middleware.MsMiddlewareProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleAuthFilterTest {

    private MsMiddlewareProperties properties;
    private ConsoleAuthSupport support;
    private ConsoleAuthFilter filter;

    @BeforeEach
    void setUp() {
        properties = new MsMiddlewareProperties();
        properties.getConsole().setEnabled(true);
        properties.getConsole().setAuthToken("demo-secret");
        support = new ConsoleAuthSupport();
        filter = new ConsoleAuthFilter(properties, support);
    }

    @Test
    void rejectsProtectedApiWithoutToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ms-console/api/issues");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
        assertEquals(null, chain.getRequest());
    }

    @Test
    void allowsProtectedApiWithHeaderToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ms-console/api/issues");
        request.addHeader(ConsoleAuthSupport.HEADER_NAME, "demo-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void allowsSseWithQueryToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ms-console/api/stream");
        request.setParameter("token", "demo-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void authStatusPublicWithoutToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ms-console/api/auth/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void skipsWhenAuthDisabled() throws ServletException, IOException {
        properties.getConsole().setAuthToken("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ms-console/api/issues");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }
}
