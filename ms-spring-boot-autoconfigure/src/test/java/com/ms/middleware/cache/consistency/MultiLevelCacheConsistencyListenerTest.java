package com.ms.middleware.cache.consistency;

import com.ms.middleware.cache.DistributedCache;
import com.ms.middleware.cache.LocalCache;
import com.ms.middleware.cache.MultiLevelCache;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多级缓存一致性监听器测试类
 */
class MultiLevelCacheConsistencyListenerTest {

    private LocalCache localCache;
    private DistributedCache distributedCache;
    private MultiLevelCache multiLevelCache;
    private MultiLevelCacheConsistencyListener listener;

    @BeforeEach
    void setUp() {
        // 创建 MeterRegistry 和 MsMetrics 实例
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics metrics = new MsMetrics(meterRegistry);
        
        MsMiddlewareProperties.LocalCacheProperties localProperties = new MsMiddlewareProperties.LocalCacheProperties();
        localProperties.setSize(100);
        localProperties.setTtl(60);
        localCache = new LocalCache(localProperties, metrics);

        MsMiddlewareProperties.DistributedCacheProperties distributedProperties = new MsMiddlewareProperties.DistributedCacheProperties();
        distributedProperties.setTtl(300);
        distributedProperties.setEnabled(false);
        distributedCache = new TestDistributedCache(distributedProperties, metrics);

        multiLevelCache = new MultiLevelCache(localCache, distributedCache);
        listener = new MultiLevelCacheConsistencyListener(multiLevelCache);
    }

    @Test
    void testOnInvalidationSingle() {
        multiLevelCache.put("key1", "value1");

        assertNotNull(localCache.get("key1"));
        assertNotNull(distributedCache.get("key1"));

        CacheInvalidationEvent event = CacheInvalidationEvent.single("key1");
        listener.onInvalidation(event);

        assertNull(localCache.get("key1"));
        assertNull(distributedCache.get("key1"));
    }

    @Test
    void testOnInvalidationBatch() {
        multiLevelCache.put("key1", "value1");
        multiLevelCache.put("key2", "value2");
        multiLevelCache.put("key3", "value3");

        assertNotNull(localCache.get("key1"));
        assertNotNull(localCache.get("key2"));
        assertNotNull(localCache.get("key3"));

        Set<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        keys.add("key3");

        CacheInvalidationEvent event = CacheInvalidationEvent.batch(keys);
        listener.onInvalidation(event);

        assertNull(localCache.get("key1"));
        assertNull(localCache.get("key2"));
        assertNull(localCache.get("key3"));
        assertNull(distributedCache.get("key1"));
        assertNull(distributedCache.get("key2"));
        assertNull(distributedCache.get("key3"));
    }

    @Test
    void testOnInvalidationAll() {
        multiLevelCache.put("key1", "value1");
        multiLevelCache.put("key2", "value2");

        assertNotNull(localCache.get("key1"));
        assertNotNull(localCache.get("key2"));

        CacheInvalidationEvent event = CacheInvalidationEvent.all();
        listener.onInvalidation(event);

        assertNull(localCache.get("key1"));
        assertNull(localCache.get("key2"));
        assertNull(distributedCache.get("key1"));
        assertNull(distributedCache.get("key2"));
    }

    @Test
    void testPartialInvalidation() {
        multiLevelCache.put("key1", "value1");
        multiLevelCache.put("key2", "value2");
        multiLevelCache.put("key3", "value3");

        assertNotNull(localCache.get("key1"));
        assertNotNull(localCache.get("key2"));
        assertNotNull(localCache.get("key3"));

        CacheInvalidationEvent event = CacheInvalidationEvent.single("key2");
        listener.onInvalidation(event);

        assertNull(localCache.get("key2"));
        assertNull(distributedCache.get("key2"));

        assertNotNull(localCache.get("key1"));
        assertNotNull(localCache.get("key3"));
        assertNotNull(distributedCache.get("key1"));
        assertNotNull(distributedCache.get("key3"));
    }

    static class TestDistributedCache extends DistributedCache {
        private final java.util.Map<String, Object> cache = new java.util.HashMap<>();

        public TestDistributedCache(MsMiddlewareProperties.DistributedCacheProperties properties, MsMetrics metrics) {
            super(null, properties, metrics);
        }

        @Override
        public <T> T get(String key) {
            return (T) cache.get(key);
        }

        @Override
        public void put(String key, Object value) {
            cache.put(key, value);
        }

        @Override
        public void remove(String key) {
            cache.remove(key);
        }

        @Override
        public void remove(String... keys) {
            for (String key : keys) {
                cache.remove(key);
            }
        }

        @Override
        public void clear() {
            cache.clear();
        }
    }
}
