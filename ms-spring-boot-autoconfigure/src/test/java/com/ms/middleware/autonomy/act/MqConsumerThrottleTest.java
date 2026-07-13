package com.ms.middleware.autonomy.act;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.rate.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link MqConsumerThrottle} 启用/关闭与消费许可逻辑。
 */
@ExtendWith(MockitoExtension.class)
class MqConsumerThrottleTest {

    @Mock
    private RateLimiter rateLimiter;

    private MqConsumerThrottle throttle;
    private MsMiddlewareProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MsMiddlewareProperties();
        properties.getAutonomy().getMq().setThrottleLimit(20);
        properties.getAutonomy().getMq().setThrottleWindowSeconds(30);
        throttle = new MqConsumerThrottle(rateLimiter, properties);
    }

    /** 未启用时应直接放行 */
    @Test
    void permitGrantedWhenDisabled() throws InterruptedException {
        throttle.awaitPermit();
        assertFalse(throttle.isEnabled());
    }

    /** 启用后应写入限流参数并 reset Redis 键 */
    @Test
    void enableSetsLimitAndResetsKey() {
        when(rateLimiter.reset(MqConsumerThrottle.RATE_LIMIT_KEY)).thenReturn(true);

        assertTrue(throttle.enable(0, 0));
        assertTrue(throttle.isEnabled());
        assertEquals(20, throttle.getLimit());
        assertEquals(30, throttle.getWindowSeconds());
        verify(rateLimiter).reset(MqConsumerThrottle.RATE_LIMIT_KEY);
    }

    /** 关闭时应 reset 并标记 disabled */
    @Test
    void disableClearsState() {
        when(rateLimiter.reset(MqConsumerThrottle.RATE_LIMIT_KEY)).thenReturn(true);
        throttle.enable(10, 60);
        throttle.disable();

        assertFalse(throttle.isEnabled());
        verify(rateLimiter, org.mockito.Mockito.atLeast(2)).reset(MqConsumerThrottle.RATE_LIMIT_KEY);
    }

    /** 启用后 awaitPermit 应调用令牌桶限流 */
    @Test
    void awaitPermitUsesTokenBucketWhenEnabled() throws InterruptedException {
        when(rateLimiter.reset(MqConsumerThrottle.RATE_LIMIT_KEY)).thenReturn(true);
        when(rateLimiter.tryAcquireWithTokenBucket(
                eq(MqConsumerThrottle.RATE_LIMIT_KEY), anyInt(), anyLong(), any(TimeUnit.class), anyInt()))
                .thenReturn(true);

        throttle.enable(15, 60);
        throttle.awaitPermit();

        verify(rateLimiter).tryAcquireWithTokenBucket(
                eq(MqConsumerThrottle.RATE_LIMIT_KEY), eq(15), eq(60L), eq(TimeUnit.SECONDS), eq(15));
    }
}
