package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 分布式缓存测试类
 * 使用 Mockito 模拟 Redisson 客户端
 */
class DistributedCacheTest {

    @Mock
    private RedissonClient redissonClient;
    
    @Mock
    private RBucket<Object> mockBucket;
    
    private DistributedCache distributedCache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        MsMiddlewareProperties.DistributedCacheProperties distributedProperties = 
            new MsMiddlewareProperties.DistributedCacheProperties();
        distributedProperties.setTtl(300);
        distributedProperties.setEnabled(true);
        properties.getCache().setDistributed(distributedProperties);

        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics metrics = new MsMetrics(meterRegistry);
        AtomicReference<RedissonClient> redissonClientRef = new AtomicReference<>(redissonClient);
        distributedCache = new DistributedCache(redissonClientRef, distributedProperties, properties, metrics);
    }

    @Test
    void testPutAndGet() {
        String key = "test-key";
        String value = "test-value";

        when(mockBucket.get()).thenReturn(value);

        distributedCache.put(key, value);
        String result = distributedCache.get(key);

        assertEquals(value, result);
        verify(mockBucket).set(value, 300, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    void testGetWithDefaultValue() {
        String key = "non-existent-key";
        String defaultValue = "default-value";

        when(mockBucket.get()).thenReturn(null);

        String result = distributedCache.get(key, defaultValue);

        assertEquals(defaultValue, result);
    }

    @Test
    void testRemove() {
        String key = "test-key";

        distributedCache.remove(key);

        verify(mockBucket).delete();
    }

    @Test
    void testExists() {
        String key = "test-key";

        when(mockBucket.isExists()).thenReturn(true);

        boolean exists = distributedCache.exists(key);

        assertTrue(exists);
        verify(mockBucket).isExists();
    }

    @Test
    void testGetCacheType() {
        assertEquals(CacheType.DISTRIBUTED, distributedCache.getCacheType());
    }
}
