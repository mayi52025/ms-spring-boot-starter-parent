package com.ms.middleware.console.api;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.FailedMessageTraceView;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.console.auth.ConsoleAuthSupport;
import com.ms.middleware.console.chat.ConsoleChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 控制台失败 Trace API 切片测试。
 */
@ExtendWith(MockitoExtension.class)
class AutonomyConsoleControllerTracesTest {

    @Mock
    private MiddlewareInsightService insightService;
    @Mock
    private ConsoleChatService chatService;
    @Mock
    private MsMiddlewareProperties properties;
    @Mock
    private ConsoleAuthSupport consoleAuthSupport;

    @InjectMocks
    private AutonomyConsoleController controller;

    @Test
    void failedTracesEndpointReturnsList() throws Exception {
        FailedMessageTraceView view = new FailedMessageTraceView();
        view.setMessageId("abc-12345678");
        view.setQueue("order-created");
        view.setErrorMessage("demo failure");
        view.setProcessTimeMs(15L);
        view.setProcessTime(Instant.parse("2026-07-14T03:00:00Z"));
        when(insightService.listFailedTraces(5)).thenReturn(List.of(view));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/ms-console/api/traces/failed").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.traces[0].messageId").value("abc-12345678"))
                .andExpect(jsonPath("$.traces[0].queue").value("order-created"));
    }

    @Test
    void authStatusEndpointReportsRequiredFlag() throws Exception {
        MsMiddlewareProperties.ConsoleProperties console = new MsMiddlewareProperties.ConsoleProperties();
        when(properties.getConsole()).thenReturn(console);
        when(consoleAuthSupport.isAuthRequired(console)).thenReturn(true);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/ms-console/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authRequired").value(true));
    }
}
