package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class LocalCacheManualTest {

    public static void main(String[] args) {
        // 手动测试本地缓存
        MsMiddlewareProperties.LocalCacheProperties properties = new MsMiddlewareProperties.LocalCacheProperties();
        properties.setSize(100);
        properties.setTtl(3600);
        
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics metrics = new MsMetrics(meterRegistry);
        
        LocalCache localCache = new LocalCache(properties, metrics);

        // 测试基本操作
        localCache.put("key1", "value1");
        System.out.println("Get key1: " + localCache.get("key1"));
        System.out.println("Size: " + localCache.size());

        localCache.remove("key1");
        System.out.println("Get key1 after remove: " + localCache.get("key1"));
        System.out.println("Size after remove: " + localCache.size());

        localCache.put("key2", "value2");
        localCache.put("key3", "value3");
        System.out.println("Size after adding key2 and key3: " + localCache.size());

        localCache.clear();
        System.out.println("Size after clear: " + localCache.size());
    }
}
