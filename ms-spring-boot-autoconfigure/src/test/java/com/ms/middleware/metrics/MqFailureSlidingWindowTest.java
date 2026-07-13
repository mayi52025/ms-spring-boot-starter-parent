package com.ms.middleware.metrics;

import com.ms.middleware.MsMiddlewareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MQ 失败滑动窗口：窗口内计数与 STABLE 清空。
 */
class MqFailureSlidingWindowTest {

    private MqFailureSlidingWindow window;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getAutonomy().setMqFailureWindowMinutes(5);
        window = new MqFailureSlidingWindow(properties);
    }

    @Test
    void countInWindowTracksRecentFailures() {
        window.recordFailure();
        window.recordFailure();
        assertEquals(2, window.countInWindow());
    }

    @Test
    void clearRemovesAllFailures() {
        window.recordFailure();
        window.recordFailure();
        window.clear();
        assertEquals(0, window.countInWindow());
    }
}
