package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地缓存测试类
 */
class LocalCacheTest {

    private LocalCache localCache;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties.LocalCacheProperties properties = 
            new MsMiddlewareProperties.LocalCacheProperties();
        properties.setSize(100);
        properties.setTtl(60);
        properties.setRefreshInterval(30);

        localCache = new LocalCache(properties);
    }

    @Test
    void testPutAndGet() {
        String key = "test-key";
        String value = "test-value";

        localCache.put(key, value);
        String result = localCache.get(key);

        assertEquals(value, result);
    }

    @Test
    void testGetWithDefaultValue() {
        String key = "non-existent-key";
        String defaultValue = "default-value";

        String result = localCache.get(key, defaultValue);

        assertEquals(defaultValue, result);
    }

    @Test
    void testRemove() {
        String key = "test-key";
        String value = "test-value";

        localCache.put(key, value);
        localCache.remove(key);

        assertNull(localCache.get(key));
    }

    @Test
    void testBatchRemove() {
        localCache.put("key1", "value1");
        localCache.put("key2", "value2");
        localCache.put("key3", "value3");

        localCache.remove("key1", "key2");

        assertNull(localCache.get("key1"));
        assertNull(localCache.get("key2"));
        assertNotNull(localCache.get("key3"));
    }

    @Test
    void testClear() {
        localCache.put("key1", "value1");
        localCache.put("key2", "value2");

        localCache.clear();

        assertEquals(0, localCache.size());
    }

    @Test
    void testExists() {
        String key = "test-key";
        String value = "test-value";

        assertFalse(localCache.exists(key));
        localCache.put(key, value);
        assertTrue(localCache.exists(key));
    }

    @Test
    void testSize() {
        assertEquals(0, localCache.size());

        localCache.put("key1", "value1");
        localCache.put("key2", "value2");

        assertEquals(2, localCache.size());
    }

    @Test
    void testGetCacheType() {
        assertEquals(CacheType.LOCAL, localCache.getCacheType());
    }
}
