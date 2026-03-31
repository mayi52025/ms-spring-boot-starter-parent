package com.ms.middleware.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控指标收集类
 */
@Component
public class MsMetrics {

    // 缓存指标
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cachePuts;
    private final Counter cacheEvictions;
    private final Gauge cacheSize;

    // 消息队列指标
    private final Counter messagePublished;
    private final Counter messageConsumed;
    private final Counter messageFailed;
    private final Timer messageProcessingTime;

    // 分布式锁指标
    private final Counter lockAcquired;
    private final Counter lockFailed;
    private final Timer lockAcquisitionTime;

    // 限流指标
    private final Counter rateLimitPassed;
    private final Counter rateLimitRejected;

    // 熔断指标
    private final Counter circuitBreakerOpen;
    private final Counter circuitBreakerClosed;
    private final Counter circuitBreakerHalfOpen;
    private final Counter circuitBreakerFailed;

    // 故障次数
    private final AtomicLong failureCount = new AtomicLong(0);

    @Autowired
    public MsMetrics(MeterRegistry meterRegistry) {
        // 缓存指标
        cacheHits = Counter.builder("ms.cache.hits")
                .description("Cache hit count")
                .register(meterRegistry);

        cacheMisses = Counter.builder("ms.cache.misses")
                .description("Cache miss count")
                .register(meterRegistry);

        cachePuts = Counter.builder("ms.cache.puts")
                .description("Cache put count")
                .register(meterRegistry);

        cacheEvictions = Counter.builder("ms.cache.evictions")
                .description("Cache eviction count")
                .register(meterRegistry);

        cacheSize = Gauge.builder("ms.cache.size", () -> 0)
                .description("Cache size")
                .register(meterRegistry);

        // 消息队列指标
        messagePublished = Counter.builder("ms.mq.published")
                .description("Message published count")
                .register(meterRegistry);

        messageConsumed = Counter.builder("ms.mq.consumed")
                .description("Message consumed count")
                .register(meterRegistry);

        messageFailed = Counter.builder("ms.mq.failed")
                .description("Message failed count")
                .register(meterRegistry);

        messageProcessingTime = Timer.builder("ms.mq.processing.time")
                .description("Message processing time")
                .register(meterRegistry);

        // 分布式锁指标
        lockAcquired = Counter.builder("ms.lock.acquired")
                .description("Lock acquired count")
                .register(meterRegistry);

        lockFailed = Counter.builder("ms.lock.failed")
                .description("Lock failed count")
                .register(meterRegistry);

        lockAcquisitionTime = Timer.builder("ms.lock.acquisition.time")
                .description("Lock acquisition time")
                .register(meterRegistry);

        // 限流指标
        rateLimitPassed = Counter.builder("ms.rate.limit.passed")
                .description("Rate limit passed count")
                .register(meterRegistry);

        rateLimitRejected = Counter.builder("ms.rate.limit.rejected")
                .description("Rate limit rejected count")
                .register(meterRegistry);

        // 熔断指标
        circuitBreakerOpen = Counter.builder("ms.circuit.breaker.open")
                .description("Circuit breaker open count")
                .register(meterRegistry);

        circuitBreakerClosed = Counter.builder("ms.circuit.breaker.closed")
                .description("Circuit breaker closed count")
                .register(meterRegistry);

        circuitBreakerHalfOpen = Counter.builder("ms.circuit.breaker.half.open")
                .description("Circuit breaker half open count")
                .register(meterRegistry);

        circuitBreakerFailed = Counter.builder("ms.circuit.breaker.failed")
                .description("Circuit breaker failed count")
                .register(meterRegistry);

        // 故障次数指标
        Gauge.builder("ms.failure.count", failureCount, AtomicLong::get)
                .description("Failure count")
                .register(meterRegistry);
    }

    // 缓存指标方法
    public void incrementCacheHits() {
        cacheHits.increment();
    }

    public void incrementCacheMisses() {
        cacheMisses.increment();
    }

    public void incrementCachePuts() {
        cachePuts.increment();
    }

    public void incrementCacheEvictions() {
        cacheEvictions.increment();
    }

    public void setCacheSize(long size) {
        // 这里需要实现动态更新缓存大小的逻辑
    }

    // 消息队列指标方法
    public void incrementMessagePublished() {
        messagePublished.increment();
    }

    public void incrementMessageConsumed() {
        messageConsumed.increment();
    }

    public void incrementMessageFailed() {
        messageFailed.increment();
    }

    public Timer.Sample startMessageProcessing() {
        return Timer.start();
    }

    public void stopMessageProcessing(Timer.Sample sample) {
        sample.stop(messageProcessingTime);
    }

    // 分布式锁指标方法
    public void incrementLockAcquired() {
        lockAcquired.increment();
    }

    public void incrementLockFailed() {
        lockFailed.increment();
    }

    public Timer.Sample startLockAcquisition() {
        return Timer.start();
    }

    public void stopLockAcquisition(Timer.Sample sample) {
        sample.stop(lockAcquisitionTime);
    }

    // 限流指标方法
    public void incrementRateLimitPassed() {
        rateLimitPassed.increment();
    }

    public void incrementRateLimitRejected() {
        rateLimitRejected.increment();
    }

    // 熔断指标方法
    public void incrementCircuitBreakerOpen() {
        circuitBreakerOpen.increment();
    }

    public void incrementCircuitBreakerClosed() {
        circuitBreakerClosed.increment();
    }

    public void incrementCircuitBreakerHalfOpen() {
        circuitBreakerHalfOpen.increment();
    }

    public void incrementCircuitBreakerFailed() {
        circuitBreakerFailed.increment();
    }

    // 故障次数方法
    public void incrementFailureCount() {
        failureCount.incrementAndGet();
    }

    public long getFailureCount() {
        return failureCount.get();
    }

}