package com.ms.middleware.autonomy.act;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.mq.MsMessageQueue;
import com.ms.middleware.mq.trace.MessageTrace;
import com.ms.middleware.mq.trace.MessageTraceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link MqDelayedRetryExecutor} 从 trace 取失败消息并延迟重投。
 */
@ExtendWith(MockitoExtension.class)
class MqDelayedRetryExecutorTest {

    @Mock
    private MsMessageQueue messageQueue;

    private MessageTraceManager traceManager;
    private MqDelayedRetryExecutor executor;

    @BeforeEach
    void setUp() {
        traceManager = MessageTraceManager.getInstance();
        traceManager.clear();

        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getAutonomy().getMq().setDelayedRetryBatchSize(5);
        properties.getAutonomy().getMq().setDelayedRetryDelayMs(3000);

        executor = new MqDelayedRetryExecutor(messageQueue, traceManager, properties);
    }

    /** 有失败 trace 且带 retryPayload 时应 sendDelayed */
    @Test
    void retriesFailedTraceWithPayload() {
        MessageTrace trace = failedTrace("msg-1", Map.of("orderId", 1001));
        traceManager.recordSend(trace);
        traceManager.recordProcess("msg-1", false, "boom", 10);
        traceManager.storeRetryPayload("msg-1", Map.of("orderId", 1001));

        when(messageQueue.sendDelayed(eq("ex"), eq("rk"), any(), eq(3000L))).thenReturn(true);

        int count = executor.retryFailedBatch();

        assertEquals(1, count);
        verify(messageQueue).sendDelayed(eq("ex"), eq("rk"), any(), eq(3000L));
    }

    /** 无失败 trace 时返回 0 */
    @Test
    void returnsZeroWhenNoFailedTraces() {
        assertEquals(0, executor.retryFailedBatch());
    }

    private MessageTrace failedTrace(String messageId, Object payload) {
        MessageTrace trace = new MessageTrace();
        trace.setMessageId(messageId);
        trace.setExchange("ex");
        trace.setRoutingKey("rk");
        trace.setSendTime(Instant.now());
        trace.setSuccess(false);
        Map<String, Object> extra = new HashMap<>();
        extra.put(MqDelayedRetryExecutor.retryPayloadKey(), payload);
        trace.setExtra(extra);
        return trace;
    }
}
