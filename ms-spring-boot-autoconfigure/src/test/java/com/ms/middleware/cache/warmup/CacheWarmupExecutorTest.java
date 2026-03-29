package com.ms.middleware.cache.warmup;

import com.ms.middleware.cache.LocalCache;
import com.ms.middleware.cache.MsCache;
import com.ms.middleware.MsMiddlewareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存预热执行器测试类
 */
class CacheWarmupExecutorTest {

    private MsCache cache;
    private CacheWarmup warmupProvider;
    private CacheWarmupExecutor warmupExecutor;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties.LocalCacheProperties properties = new MsMiddlewareProperties.LocalCacheProperties();
        properties.setSize(100);
        properties.setTtl(60);
        cache = new LocalCache(properties);

        warmupProvider = new CacheWarmup() {
            @Override
            public Map<String, Object> getWarmupData() {
                Map<String, Object> data = new HashMap<>();
                data.put("key1", "value1");
                data.put("key2", "value2");
                data.put("key3", "value3");
                return data;
            }

            @Override
            public List<WarmupItem> getWarmupItems() {
                return List.of(
                    new WarmupItem("key4", "value4", 300),
                    new WarmupItem("key5", "value5", 600)
                );
            }
        };

        warmupExecutor = new CacheWarmupExecutor(cache, warmupProvider);
    }

    @Test
    void testExecuteWarmup() {
        warmupExecutor.executeWarmup();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        assertNotNull(cache.get("key3"));
        assertNotNull(cache.get("key4"));
        assertNotNull(cache.get("key5"));

        assertEquals("value1", cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
        assertEquals("value3", cache.get("key3"));
        assertEquals("value4", cache.get("key4"));
        assertEquals("value5", cache.get("key5"));
    }

    @Test
    void testDisabledWarmup() {
        CacheWarmup disabledProvider = new CacheWarmup() {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public Map<String, Object> getWarmupData() {
                return Map.of();
            }
        };

        CacheWarmupExecutor disabledExecutor = new CacheWarmupExecutor(cache, disabledProvider);
        disabledExecutor.executeWarmup();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
        assertNull(cache.get("key3"));
    }

    @Test
    void testShutdown() {
        warmupExecutor.executeWarmup();
        warmupExecutor.shutdown();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        assertNotNull(cache.get("key3"));
    }
}
