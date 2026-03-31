package com.ms.middleware.rate;

import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisRateLimiterTest {

    private RedissonClient redissonClient;
    private RBucket<Long> longBucket;
    private RBucket<RedisRateLimiter.TokenBucketState> tokenBucket;
    private RScoredSortedSet<String> sortedSet;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redissonClient = Mockito.mock(RedissonClient.class);
        longBucket = Mockito.mock(RBucket.class);
        tokenBucket = Mockito.mock(RBucket.class);
        sortedSet = Mockito.mock(RScoredSortedSet.class);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        
        // 使用类型转换解决泛型问题
        when(redissonClient.getBucket("rate:limiter:counter:test-key")).thenReturn((RBucket) longBucket);
        when(redissonClient.getBucket("rate:limiter:token:test-key")).thenReturn((RBucket) tokenBucket);
        when(redissonClient.getScoredSortedSet("rate:limiter:sliding:test-key")).thenReturn((RScoredSortedSet) sortedSet);
        
        MsMetrics metrics = new MsMetrics(meterRegistry);
        rateLimiter = new RedisRateLimiter(redissonClient, metrics);
    }

    @Test
    void testTryAcquireFirstTime() {
        when(longBucket.get()).thenReturn(null);
        boolean result = rateLimiter.tryAcquire("test-key", 10, 1, TimeUnit.MINUTES);
        assertTrue(result);
        verify(longBucket, times(1)).set(1L, 1, TimeUnit.MINUTES);
    }

    @Test
    void testTryAcquireWithinLimit() {
        when(longBucket.get()).thenReturn(5L);
        boolean result = rateLimiter.tryAcquire("test-key", 10, 1, TimeUnit.MINUTES);
        assertTrue(result);
        verify(longBucket, times(1)).set(6L, 1, TimeUnit.MINUTES);
    }

    @Test
    void testTryAcquireExceedLimit() {
        when(longBucket.get()).thenReturn(10L);
        boolean result = rateLimiter.tryAcquire("test-key", 10, 1, TimeUnit.MINUTES);
        assertFalse(result);
        verify(longBucket, never()).set(anyLong(), anyLong(), any());
    }

    @Test
    void testTryAcquireException() {
        when(longBucket.get()).thenThrow(new RuntimeException("Redis error"));
        boolean result = rateLimiter.tryAcquire("test-key", 10, 1, TimeUnit.MINUTES);
        assertTrue(result); // 降级策略：允许请求通过
    }

    @Test
    void testTryAcquireWithTokenBucketFirstTime() {
        when(tokenBucket.get()).thenReturn(null);
        boolean result = rateLimiter.tryAcquireWithTokenBucket("test-key", 10, 1, TimeUnit.MINUTES, 20);
        assertTrue(result);
        verify(tokenBucket, times(1)).set(any(), anyLong(), any());
    }

    @Test
    void testTryAcquireWithTokenBucketHasTokens() {
        RedisRateLimiter.TokenBucketState state = new RedisRateLimiter.TokenBucketState();
        state.setLastRefillTime(System.currentTimeMillis() - 1000);
        state.setTokens(5);
        when(tokenBucket.get()).thenReturn(state);
        
        boolean result = rateLimiter.tryAcquireWithTokenBucket("test-key", 10, 1, TimeUnit.MINUTES, 20);
        assertTrue(result);
        verify(tokenBucket, times(1)).set(any(), anyLong(), any());
    }

    @Test
    void testTryAcquireWithTokenBucketNoTokens() {
        RedisRateLimiter.TokenBucketState state = new RedisRateLimiter.TokenBucketState();
        state.setLastRefillTime(System.currentTimeMillis());
        state.setTokens(0);
        when(tokenBucket.get()).thenReturn(state);
        
        boolean result = rateLimiter.tryAcquireWithTokenBucket("test-key", 10, 1, TimeUnit.MINUTES, 20);
        assertFalse(result);
        verify(tokenBucket, never()).set(any(), anyLong(), any());
    }

    @Test
    void testTryAcquireWithSlidingWindowWithinLimit() {
        when(sortedSet.size()).thenReturn(5);
        when(sortedSet.add(anyDouble(), anyString())).thenReturn(true);
        when(sortedSet.expire(anyLong(), any())).thenReturn(true);
        
        boolean result = rateLimiter.tryAcquireWithSlidingWindow("test-key", 10, 1, TimeUnit.MINUTES);
        assertTrue(result);
        verify(sortedSet, times(1)).removeRangeByScore(anyDouble(), anyBoolean(), anyDouble(), anyBoolean());
        verify(sortedSet, times(1)).add(anyDouble(), anyString());
        verify(sortedSet, times(1)).expire(anyLong(), any());
    }

    @Test
    void testTryAcquireWithSlidingWindowExceedLimit() {
        when(sortedSet.size()).thenReturn(10);
        
        boolean result = rateLimiter.tryAcquireWithSlidingWindow("test-key", 10, 1, TimeUnit.MINUTES);
        assertFalse(result);
        verify(sortedSet, times(1)).removeRangeByScore(anyDouble(), anyBoolean(), anyDouble(), anyBoolean());
        verify(sortedSet, never()).add(anyDouble(), anyString());
    }

    @Test
    void testGetCurrentCount() {
        when(longBucket.get()).thenReturn(5L);
        long result = rateLimiter.getCurrentCount("test-key");
        assertEquals(5L, result);
    }

    @Test
    void testGetCurrentCountNotFound() {
        when(longBucket.get()).thenReturn(null);
        long result = rateLimiter.getCurrentCount("test-key");
        assertEquals(0L, result);
    }

    @Test
    void testReset() {
        when(longBucket.delete()).thenReturn(true);
        when(tokenBucket.delete()).thenReturn(true);
        when(sortedSet.delete()).thenReturn(true);
        
        boolean result = rateLimiter.reset("test-key");
        assertTrue(result);
        verify(longBucket, times(1)).delete();
        verify(tokenBucket, times(1)).delete();
        verify(sortedSet, times(1)).delete();
    }

}