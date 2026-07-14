package com.ms.middleware.autonomy.insight.tool;

import com.ms.middleware.autonomy.insight.FailedMessageTraceView;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.insight.MiddlewareMetricsSnapshot;
import com.ms.middleware.autonomy.run.AutonomyRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link DefaultMiddlewareInsightTool} 委托 Insight 并格式化为文本。
 */
@ExtendWith(MockitoExtension.class)
class DefaultMiddlewareInsightToolTest {

    @Mock
    private MiddlewareInsightService insightService;

    @Test
    void getMetricsSummaryIncludesMttrAndCompletedRuns() {
        MiddlewareMetricsSnapshot snapshot = new MiddlewareMetricsSnapshot();
        snapshot.setCacheHitRate(0.85);
        snapshot.setMqFailedCount(3);
        snapshot.setGlobalFailureCount(1);
        snapshot.setActiveRunCount(2);
        snapshot.setLastMttrSeconds(120);
        snapshot.setCompletedAutonomyRuns(5);
        when(insightService.getMetrics()).thenReturn(snapshot);

        DefaultMiddlewareInsightTool tool = new DefaultMiddlewareInsightTool(insightService);
        String summary = tool.getMetricsSummary();

        assertTrue(summary.contains("85.0%"));
        assertTrue(summary.contains("最近MTTR=120s"));
        assertTrue(summary.contains("已完成自治=5"));
    }

    @Test
    void describeRunReturnsNotFoundWhenMissing() {
        when(insightService.getRun("missing")).thenReturn(Optional.empty());
        DefaultMiddlewareInsightTool tool = new DefaultMiddlewareInsightTool(insightService);
        assertTrue(tool.describeRun("missing").contains("未找到"));
    }

    @Test
    void describeRecentRunsFormatsList() {
        AutonomyRun run = new AutonomyRun();
        run.setRunId("abc12345");
        when(insightService.listRecentRuns(3)).thenReturn(List.of(run));

        DefaultMiddlewareInsightTool tool = new DefaultMiddlewareInsightTool(insightService);
        assertTrue(tool.describeRecentRuns(3).contains("abc12345"));
    }

    @Test
    void listRecentFailedTracesFormatsMessageIds() {
        FailedMessageTraceView view = new FailedMessageTraceView();
        view.setMessageId("msg-trace-001");
        view.setQueue("order-created");
        view.setErrorMessage("intentional failure");
        when(insightService.listFailedTraces(5)).thenReturn(List.of(view));

        DefaultMiddlewareInsightTool tool = new DefaultMiddlewareInsightTool(insightService);
        String text = tool.listRecentFailedTraces(5);

        assertTrue(text.contains("msg-trace-001"));
        assertTrue(text.contains("order-created"));
        assertTrue(text.contains("trace <messageId>"));
    }
}
