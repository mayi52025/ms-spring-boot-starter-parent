package com.ms.middleware.metrics;

import com.ms.middleware.MsMiddlewareProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MQ 消费失败滑动窗口计数器。
 *
 * <p>自治检测使用窗口内失败次数（可随时间衰减），避免 Cumulative Counter 只增不减导致无法 STABLE。</p>
 * <p>Prometheus 仍保留 {@code ms.mq.failed} 累计 Counter 供长期统计。</p>
 */
@Component
public class MqFailureSlidingWindow {

    private final Deque<Long> failureTimestamps = new ArrayDeque<>();
    private final long windowMs;

    public MqFailureSlidingWindow(MsMiddlewareProperties properties) {
        long minutes = properties.getAutonomy().getMqFailureWindowMinutes();
        this.windowMs = Math.max(1, minutes) * 60_000L;
    }

    /** 记录一次消费失败 */
    public synchronized void recordFailure() {
        prune();
        failureTimestamps.addLast(System.currentTimeMillis());
    }

    /** 窗口内失败次数，供 {@link com.ms.middleware.autonomy.context.AutonomyContextBuilder} 使用 */
    public synchronized int countInWindow() {
        prune();
        return failureTimestamps.size();
    }

    /** MQ incident STABLE 结案时清空，配合停止故障注入后可立即恢复 */
    public synchronized void clear() {
        failureTimestamps.clear();
    }

    private void prune() {
        long cutoff = System.currentTimeMillis() - windowMs;
        while (!failureTimestamps.isEmpty() && failureTimestamps.peekFirst() < cutoff) {
            failureTimestamps.removeFirst();
        }
    }
}
