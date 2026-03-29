package com.ms.middleware.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 指标收集器
 * 使用 Micrometer 收集系统运行指标
 */
public class MetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        logger.info("Metrics collector initialized");
    }

    /**
     * 记录计数器
     * @param name 指标名称
     * @param tags 标签
     */
    public void incrementCounter(String name, String... tags) {
        Counter counter = counters.computeIfAbsent(name, n -> Counter.builder(n).tags(tags).register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录计时器
     * @param name 指标名称
     * @param durationMs 持续时间（毫秒）
     * @param tags 标签
     */
    public void recordTimer(String name, long durationMs, String... tags) {
        Timer timer = timers.computeIfAbsent(name, n -> Timer.builder(n).tags(tags).register(meterRegistry));
        timer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录仪表盘
     * @param name 指标名称
     * @param value 值
     * @param tags 标签
     */
    public void recordGauge(String name, double value, String... tags) {
        Gauge.builder(name, () -> value).tags(tags).register(meterRegistry);
    }

    /**
     * 记录缓存命中
     * @param cacheType 缓存类型
     */
    public void recordCacheHit(String cacheType) {
        incrementCounter("ms.middleware.cache.hit", "type", cacheType);
    }

    /**
     * 记录缓存未命中
     * @param cacheType 缓存类型
     */
    public void recordCacheMiss(String cacheType) {
        incrementCounter("ms.middleware.cache.miss", "type", cacheType);
    }

    /**
     * 记录缓存操作时间
     * @param cacheType 缓存类型
     * @param operation 操作类型
     * @param durationMs 持续时间（毫秒）
     */
    public void recordCacheOperationTime(String cacheType, String operation, long durationMs) {
        recordTimer("ms.middleware.cache.operation.time", durationMs, "type", cacheType, "operation", operation);
    }

    /**
     * 记录消息发送
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param success 是否成功
     */
    public void recordMessageSend(String exchange, String routingKey, boolean success) {
        incrementCounter("ms.middleware.mq.send", "exchange", exchange, "routingKey", routingKey, "success", String.valueOf(success));
    }

    /**
     * 记录消息接收
     * @param queue 队列
     * @param success 是否成功
     */
    public void recordMessageReceive(String queue, boolean success) {
        incrementCounter("ms.middleware.mq.receive", "queue", queue, "success", String.valueOf(success));
    }

    /**
     * 记录消息处理时间
     * @param queue 队列
     * @param durationMs 持续时间（毫秒）
     */
    public void recordMessageProcessTime(String queue, long durationMs) {
        recordTimer("ms.middleware.mq.process.time", durationMs, "queue", queue);
    }

    /**
     * 记录故障自愈
     * @param component 组件
     * @param status 状态
     */
    public void recordFaultSelfHealing(String component, String status) {
        incrementCounter("ms.middleware.health.selfhealing", "component", component, "status", status);
    }

    /**
     * 记录热点key
     * @param key 键
     */
    public void recordHotKey(String key) {
        incrementCounter("ms.middleware.ai.hotkey", "key", key);
    }
}
