package com.ms.middleware.circuit;

import com.ms.middleware.metrics.MsMetrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 基于 Resilience4j 的熔断实现
 */
public class Resilience4jCircuitBreaker implements com.ms.middleware.circuit.CircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(Resilience4jCircuitBreaker.class);

    private final CircuitBreaker circuitBreaker;
    private final MsMetrics metrics;

    public Resilience4jCircuitBreaker(String name, MsMetrics metrics) {
        this.metrics = metrics;
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 失败率阈值，超过此值则熔断
                .waitDurationInOpenState(Duration.ofSeconds(60)) // 熔断后等待时间
                .slidingWindowSize(100) // 滑动窗口大小
                .minimumNumberOfCalls(10) // 最小调用次数
                .permittedNumberOfCallsInHalfOpenState(5) // 半开状态允许的调用次数
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.circuitBreaker = registry.circuitBreaker(name);
    }

    @Override
    public <T> T execute(Supplier<T> supplier) throws Exception {
        try {
            T result = circuitBreaker.executeSupplier(supplier);
            // 检查并记录熔断状态
            recordCircuitState();
            return result;
        } catch (Exception e) {
            logger.error("Circuit breaker execution failed", e);
            metrics.incrementCircuitBreakerFailed();
            metrics.incrementFailureCount();
            // 检查并记录熔断状态
            recordCircuitState();
            throw e;
        }
    }

    @Override
    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        try {
            T result = circuitBreaker.executeSupplier(supplier);
            // 检查并记录熔断状态
            recordCircuitState();
            return result;
        } catch (Exception e) {
            logger.warn("Circuit breaker opened, using fallback", e);
            metrics.incrementCircuitBreakerFailed();
            // 检查并记录熔断状态
            recordCircuitState();
            return fallback.get();
        }
    }

    @Override
    public <T> T executeWithExceptionHandling(Supplier<T> supplier) {
        try {
            T result = circuitBreaker.executeSupplier(supplier);
            // 检查并记录熔断状态
            recordCircuitState();
            return result;
        } catch (Exception e) {
            logger.error("Circuit breaker execution failed, returning null", e);
            metrics.incrementCircuitBreakerFailed();
            metrics.incrementFailureCount();
            // 检查并记录熔断状态
            recordCircuitState();
            return null;
        }
    }

    @Override
    public CircuitState getState() {
        CircuitBreaker.State state = circuitBreaker.getState();
        switch (state) {
            case CLOSED:
                return CircuitState.CLOSED;
            case OPEN:
                return CircuitState.OPEN;
            case HALF_OPEN:
                return CircuitState.HALF_OPEN;
            default:
                return CircuitState.CLOSED;
        }
    }

    /**
     * 记录熔断状态
     */
    private void recordCircuitState() {
        CircuitBreaker.State state = circuitBreaker.getState();
        switch (state) {
            case CLOSED:
                metrics.incrementCircuitBreakerClosed();
                break;
            case OPEN:
                metrics.incrementCircuitBreakerOpen();
                break;
            case HALF_OPEN:
                metrics.incrementCircuitBreakerHalfOpen();
                break;
        }
    }

    @Override
    public void reset() {
        circuitBreaker.reset();
        logger.info("Circuit breaker reset");
    }

}