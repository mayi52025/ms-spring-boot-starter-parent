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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 多级缓存测试类
 */
class MultiLevelCacheTest {

    @Mock
    private RedissonClient redissonClient;
    
    @Mock
    private RBucket<Object> mockBucket;
    
    private LocalCache localCache;
    private DistributedCache distributedCache;
    private MultiLevelCache multiLevelCache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建 MsMetrics 实例
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics metrics = new MsMetrics(meterRegistry);
        
        // 初始化本地缓存
        MsMiddlewareProperties.LocalCacheProperties localProperties = 
            new MsMiddlewareProperties.LocalCacheProperties();
        localProperties.setSize(100);
        localProperties.setTtl(60);
        localCache = new LocalCache(localProperties, metrics);

        // 初始化分布式缓存
        MsMiddlewareProperties.DistributedCacheProperties distributedProperties = 
            new MsMiddlewareProperties.DistributedCacheProperties();
        distributedProperties.setTtl(300);
        distributedProperties.setEnabled(true);

        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);
        distributedCache = new DistributedCache(redissonClient, distributedProperties, metrics);

        // 初始化多级缓存
        multiLevelCache = new MultiLevelCache(localCache, distributedCache);
    }

    @Test
    void testGetFromLocalCache() {
        String key = "test-key";
        String value = "test-value";

        // 先设置本地缓存
        localCache.put(key, value);

        // 从多级缓存获取
        String result = multiLevelCache.get(key);

        // 验证结果
        assertEquals(value, result);
        // 验证没有调用分布式缓存
        verify(mockBucket, never()).get();
    }

    @Test
    void testGetFromDistributedCache() {
        String key = "test-key";
        String value = "test-value";

        // 模拟分布式缓存返回值
        when(mockBucket.get()).thenReturn(value);

        // 从多级缓存获取
        String result = multiLevelCache.get(key);

        // 验证结果
        assertEquals(value, result);
        // 验证调用了分布式缓存
        verify(mockBucket).get();
        // 验证数据同步到了本地缓存
        assertEquals(value, localCache.get(key));
    }

    @Test
    void testPut() {
        String key = "test-key";
        String value = "test-value";

        // 放入多级缓存
        multiLevelCache.put(key, value);

        // 验证本地缓存和分布式缓存都被更新
        assertEquals(value, localCache.get(key));
        verify(mockBucket).set(value, 300, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    void testRemove() {
        String key = "test-key";
        String value = "test-value";

        // 先放入缓存
        multiLevelCache.put(key, value);
        // 然后删除
        multiLevelCache.remove(key);

        // 验证本地缓存和分布式缓存都被删除
        assertNull(localCache.get(key));
        verify(mockBucket).delete();
    }

    @Test
    void testExists() {
        String key = "test-key";

        // 测试本地缓存存在
        localCache.put(key, "value");
        assertTrue(multiLevelCache.exists(key));

        // 测试本地缓存不存在，分布式缓存存在
        localCache.remove(key);
        when(mockBucket.isExists()).thenReturn(true);
        assertTrue(multiLevelCache.exists(key));

        // 测试两者都不存在
        when(mockBucket.isExists()).thenReturn(false);
        assertFalse(multiLevelCache.exists(key));
    }

    @Test
    void testGetCacheType() {
        assertEquals(CacheType.MULTI_LEVEL, multiLevelCache.getCacheType());
    }
}
