package com.ms.middleware.lock;

import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisDistributedLockTest {

    private RedissonClient redissonClient;
    private RLock rLock;
    private DistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        redissonClient = Mockito.mock(RedissonClient.class);
        rLock = Mockito.mock(RLock.class);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        MsMetrics metrics = new MsMetrics(meterRegistry);
        distributedLock = new RedisDistributedLock(redissonClient, metrics);
    }

    @Test
    void testLock() {
        doNothing().when(rLock).lock();
        boolean result = distributedLock.lock("test-key");
        assertTrue(result);
        verify(rLock, times(1)).lock();
    }

    @Test
    void testLockWithTimeout() {
        doNothing().when(rLock).lock(10, TimeUnit.SECONDS);
        boolean result = distributedLock.lock("test-key", 10, TimeUnit.SECONDS);
        assertTrue(result);
        verify(rLock, times(1)).lock(10, TimeUnit.SECONDS);
    }

    @Test
    void testTryLock() throws InterruptedException {
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        boolean result = distributedLock.tryLock("test-key", 5, 10, TimeUnit.SECONDS);
        assertTrue(result);
        verify(rLock, times(1)).tryLock(5, 10, TimeUnit.SECONDS);
    }

    @Test
    void testUnlock() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        boolean result = distributedLock.unlock("test-key");
        assertTrue(result);
        verify(rLock, times(1)).isHeldByCurrentThread();
        verify(rLock, times(1)).unlock();
    }

    @Test
    void testUnlockNotHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(false);
        boolean result = distributedLock.unlock("test-key");
        assertFalse(result);
        verify(rLock, times(1)).isHeldByCurrentThread();
        verify(rLock, never()).unlock();
    }

    @Test
    void testExists() {
        when(rLock.isLocked()).thenReturn(true);
        boolean result = distributedLock.exists("test-key");
        assertTrue(result);
        verify(rLock, times(1)).isLocked();
    }

    @Test
    void testGetRemainingTime() {
        when(rLock.remainTimeToLive()).thenReturn(5000L);
        long result = distributedLock.getRemainingTime("test-key", TimeUnit.MILLISECONDS);
        assertEquals(5000L, result);
        verify(rLock, times(1)).remainTimeToLive();
    }

    @Test
    void testLockException() {
        doThrow(new RuntimeException("Lock error")).when(rLock).lock();
        boolean result = distributedLock.lock("test-key");
        assertFalse(result);
    }

    @Test
    void testUnlockException() {
        when(rLock.isHeldByCurrentThread()).thenThrow(new RuntimeException("Unlock error"));
        boolean result = distributedLock.unlock("test-key");
        assertFalse(result);
    }

    @Test
    void testExistsException() {
        when(rLock.isLocked()).thenThrow(new RuntimeException("Exists error"));
        boolean result = distributedLock.exists("test-key");
        assertFalse(result);
    }

    @Test
    void testGetRemainingTimeException() {
        when(rLock.remainTimeToLive()).thenThrow(new RuntimeException("Get remaining time error"));
        long result = distributedLock.getRemainingTime("test-key", TimeUnit.MILLISECONDS);
        assertEquals(-1L, result);
    }

}