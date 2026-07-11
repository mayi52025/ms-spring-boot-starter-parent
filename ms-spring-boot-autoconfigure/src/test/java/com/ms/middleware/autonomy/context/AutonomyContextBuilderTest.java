package com.ms.middleware.autonomy.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.health.FaultSelfHealing;
import com.ms.middleware.metrics.MsMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link AutonomyContextBuilder} 的阈值统一逻辑：
 * issues 生成、isMqDegraded、STABLE 结案使用同一套阈值标准。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutonomyContextBuilderTest {

    /** 与测试配置 ms.middleware.autonomy.mq-failed-warn-threshold 一致 */
    private static final long MQ_THRESHOLD = 10;

    @Mock
    private FaultSelfHealing faultSelfHealing;
    @Mock
    private MsMetrics metrics;
    @Mock
    private ObjectProvider<com.ms.middleware.ai.HotKeyManager> hotKeyManagerProvider;

    private AutonomyContextBuilder builder;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getAutonomy().setMqFailedWarnThreshold(MQ_THRESHOLD);
        properties.getAutonomy().setCacheHitRateWarnThreshold(0.5);

        when(faultSelfHealing.getComponentHealth("Redis")).thenReturn(true);
        when(faultSelfHealing.getComponentHealth("RabbitMQ")).thenReturn(true);
        when(metrics.getCacheHitRate()).thenReturn(0.8);
        when(metrics.getFailureCount()).thenReturn(0L);

        builder = new AutonomyContextBuilder(properties, faultSelfHealing, metrics, hotKeyManagerProvider);
    }

    /** 失败次数低于阈值：不应标记 MQ 降级，也不应出现在 issues */
    @Test
    void mqFailedBelowThresholdDoesNotRaiseIssue() {
        when(metrics.getMessageFailedCount()).thenReturn(MQ_THRESHOLD - 1);

        AutonomyContext ctx = builder.build();

        assertFalse(ctx.isMqDegraded());
        assertTrue(ctx.getIssues().stream().noneMatch(i -> i.contains("MQ 消费失败")));
    }

    /** 失败次数达到阈值：应标记 MQ 降级并写入 issues */
    @Test
    void mqFailedAtThresholdRaisesIssueAndDegraded() {
        when(metrics.getMessageFailedCount()).thenReturn(MQ_THRESHOLD);

        AutonomyContext ctx = builder.build();

        assertTrue(ctx.isMqDegraded());
        assertEquals(MQ_THRESHOLD, ctx.getMqFailedWarnThreshold());
        assertTrue(ctx.getIssues().stream().anyMatch(i -> i.contains("MQ 消费失败")));
    }

    /** STABLE：MQ 失败计数降到阈值以下视为 MQ_DEGRADED 已恢复 */
    @Test
    void mqDegradedResolvedWhenCountDropsBelowThreshold() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setMqFailedWarnThreshold(MQ_THRESHOLD);
        ctx.setMqFailedCount(MQ_THRESHOLD - 1);

        assertTrue(builder.isIncidentResolved("MQ_DEGRADED", ctx));
    }

    /** STABLE：仍等于阈值时 incident 未恢复 */
    @Test
    void mqDegradedNotResolvedWhenStillAtThreshold() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setMqFailedWarnThreshold(MQ_THRESHOLD);
        ctx.setMqFailedCount(MQ_THRESHOLD);

        assertFalse(builder.isIncidentResolved("MQ_DEGRADED", ctx));
    }

    /** Redis 宕机时 issues 应体现 Redis，即使 MQ 失败计数也很高 */
    @Test
    void incidentPriorityRedisOverridesMqSignal() {
        when(faultSelfHealing.getComponentHealth("Redis")).thenReturn(false);
        when(metrics.getMessageFailedCount()).thenReturn(100L);

        AutonomyContext ctx = builder.build();

        assertTrue(ctx.isMqDegraded());
        assertTrue(ctx.getIssues().stream().anyMatch(i -> i.contains("Redis 不可用")));
    }
}
