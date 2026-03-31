package com.ms.middleware.circuit;

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

    public Resilience4jCircuitBreaker(String name) {
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
            return circuitBreaker.executeSupplier(supplier);
        } catch (Exception e) {
            logger.error("Circuit breaker execution failed", e);
            throw e;
        }
    }

    @Override
    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (Exception e) {
            logger.warn("Circuit breaker opened, using fallback", e);
            return fallback.get();
        }
    }

    @Override
    public <T> T executeWithExceptionHandling(Supplier<T> supplier) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (Exception e) {
            logger.error("Circuit breaker execution failed, returning null", e);
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

    @Override
    public void reset() {
        circuitBreaker.reset();
        logger.info("Circuit breaker reset");
    }

}