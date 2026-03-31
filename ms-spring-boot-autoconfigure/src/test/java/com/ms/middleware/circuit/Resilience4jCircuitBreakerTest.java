package com.ms.middleware.circuit;

import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class Resilience4jCircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics metrics = new MsMetrics(meterRegistry);
        circuitBreaker = new Resilience4jCircuitBreaker("test-circuit-breaker", metrics);
    }

    @Test
    void testExecuteSuccess() throws Exception {
        Supplier<String> supplier = () -> "Success";
        String result = circuitBreaker.execute(supplier);
        assertEquals("Success", result);
        assertEquals(CircuitBreaker.CircuitState.CLOSED, circuitBreaker.getState());
    }

    @Test
    void testExecuteFailure() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("Failure");
        };
        assertThrows(Exception.class, () -> circuitBreaker.execute(supplier));
        assertEquals(CircuitBreaker.CircuitState.CLOSED, circuitBreaker.getState());
    }

    @Test
    void testExecuteWithFallbackSuccess() {
        Supplier<String> supplier = () -> "Success";
        Supplier<String> fallback = () -> "Fallback";
        String result = circuitBreaker.execute(supplier, fallback);
        assertEquals("Success", result);
    }

    @Test
    void testExecuteWithFallbackFailure() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("Failure");
        };
        Supplier<String> fallback = () -> "Fallback";
        String result = circuitBreaker.execute(supplier, fallback);
        assertEquals("Fallback", result);
    }

    @Test
    void testExecuteWithExceptionHandlingSuccess() {
        Supplier<String> supplier = () -> "Success";
        String result = circuitBreaker.executeWithExceptionHandling(supplier);
        assertEquals("Success", result);
    }

    @Test
    void testExecuteWithExceptionHandlingFailure() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("Failure");
        };
        String result = circuitBreaker.executeWithExceptionHandling(supplier);
        assertNull(result);
    }

    @Test
    void testGetState() {
        CircuitBreaker.CircuitState state = circuitBreaker.getState();
        assertEquals(CircuitBreaker.CircuitState.CLOSED, state);
    }

    @Test
    void testReset() {
        circuitBreaker.reset();
        CircuitBreaker.CircuitState state = circuitBreaker.getState();
        assertEquals(CircuitBreaker.CircuitState.CLOSED, state);
    }

}