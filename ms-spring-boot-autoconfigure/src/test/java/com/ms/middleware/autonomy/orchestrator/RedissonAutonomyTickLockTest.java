package com.ms.middleware.autonomy.orchestrator;

import com.ms.middleware.redis.RedissonConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonAutonomyTickLockTest {

    private RedissonClient redissonClient;
    private RLock rLock;
    private RedissonAutonomyTickLock tickLock;

    @BeforeEach
    void setUp() {
        redissonClient = Mockito.mock(RedissonClient.class);
        rLock = Mockito.mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        RedissonConnectionManager connectionManager =
                new RedissonConnectionManager(new AtomicReference<>(redissonClient), new org.redisson.config.Config());
        tickLock = new RedissonAutonomyTickLock(connectionManager, 30);
    }

    @Test
    void runsActionWhenLockAcquired() throws Exception {
        when(rLock.tryLock(eq(0L), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        AtomicBoolean ran = new AtomicBoolean(false);

        tickLock.runIfLeader("order-system", () -> ran.set(true));

        assertTrue(ran.get());
        verify(rLock).unlock();
        verify(redissonClient).getLock(RedissonAutonomyTickLock.KEY_PREFIX + "order-system");
    }

    @Test
    void skipsActionWhenLockNotAcquired() throws Exception {
        when(rLock.tryLock(eq(0L), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean(false);

        tickLock.runIfLeader("order-system", () -> ran.set(true));

        assertFalse(ran.get());
        verify(rLock, never()).unlock();
    }

    @Test
    void normalizesBlankTenantToDefault() {
        assertEquals("default", RedissonAutonomyTickLock.normalizeTenant("  "));
    }
}
