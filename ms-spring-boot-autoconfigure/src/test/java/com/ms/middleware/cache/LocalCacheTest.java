package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalCacheTest {

    private LocalCache localCache;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties.LocalCacheProperties properties = new MsMiddlewareProperties.LocalCacheProperties();
        properties.setSize(100);
        properties.setTtl(3600);
        
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics metrics = new MsMetrics(meterRegistry);
        localCache = new LocalCache(properties, metrics);
    }

    @Test
    void testPutAndGet() {
        localCache.put("key1", "value1");
        assertEquals("value1", localCache.get("key1"));
    }

    @Test
    void testRemove() {
        localCache.put("key1", "value1");
        localCache.remove("key1");
        assertNull(localCache.get("key1"));
    }

    @Test
    void testClear() {
        localCache.put("key1", "value1");
        localCache.put("key2", "value2");
        localCache.clear();
        assertNull(localCache.get("key1"));
        assertNull(localCache.get("key2"));
    }

    @Test
    void testSize() {
        localCache.put("key1", "value1");
        localCache.put("key2", "value2");
        assertEquals(2, localCache.size());
    }

    @Test
    void testExists() {
        localCache.put("key1", "value1");
        assertTrue(localCache.exists("key1"));
        assertFalse(localCache.exists("key2"));
    }
}
