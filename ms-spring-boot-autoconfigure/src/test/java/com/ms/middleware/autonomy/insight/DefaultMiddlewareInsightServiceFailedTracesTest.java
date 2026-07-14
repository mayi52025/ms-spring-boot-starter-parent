package com.ms.middleware.autonomy.insight;

import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.run.InMemoryAutonomyLedger;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.mq.trace.MessageTrace;
import com.ms.middleware.mq.trace.MessageTraceManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证失败 Trace 列表委托 {@link MessageTraceManager#listFailedTraces(int)}。
 */
class DefaultMiddlewareInsightServiceFailedTracesTest {

    private MessageTraceManager traceManager;
    private DefaultMiddlewareInsightService service;

    @BeforeEach
    void setUp() {
        traceManager = MessageTraceManager.getInstance();
        traceManager.clear();

        List<Object> events = new ArrayList<>();
        ApplicationEventPublisher publisher = events::add;
        AutonomyTenantProvider tenantProvider = () -> "order-system";
        InMemoryAutonomyLedger ledger = new InMemoryAutonomyLedger(publisher, tenantProvider, 20);
        MsMetrics metrics = new MsMetrics(new SimpleMeterRegistry());
        AutonomyMetrics autonomyMetrics = new AutonomyMetrics(new SimpleMeterRegistry());
        service = new DefaultMiddlewareInsightService(ledger, metrics, autonomyMetrics, tenantProvider);
    }

    @AfterEach
    void tearDown() {
        traceManager.clear();
    }

    @Test
    void listFailedTracesReturnsRecentFailures() {
        seedFailedTrace("msg-fail-001", "order-created", "demo error");
        seedFailedTrace("msg-fail-002", "order-created", "demo error 2");

        List<FailedMessageTraceView> traces = service.listFailedTraces(5);

        assertEquals(2, traces.size());
        assertTrue(traces.stream().anyMatch(t -> "msg-fail-001".equals(t.getMessageId())));
        assertTrue(traces.stream().anyMatch(t -> "order-created".equals(t.getQueue())));
    }

    @Test
    void listFailedTracesEmptyWhenNoFailures() {
        assertTrue(service.listFailedTraces(10).isEmpty());
    }

    private void seedFailedTrace(String messageId, String queue, String error) {
        MessageTrace trace = new MessageTrace();
        trace.setMessageId(messageId);
        trace.setQueue(queue);
        trace.setSendTime(Instant.now());
        traceManager.recordSend(trace);
        traceManager.recordReceive(messageId, queue, "order-system");
        traceManager.recordProcess(messageId, false, error, 12);
    }
}
