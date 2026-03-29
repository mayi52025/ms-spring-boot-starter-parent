package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;

public class LocalCacheManualTest {

    public static void main(String[] args) {
        // 手动测试本地缓存
        MsMiddlewareProperties.LocalCacheProperties properties = new MsMiddlewareProperties.LocalCacheProperties();
        properties.setSize(100);
        properties.setTtl(3600);
        LocalCache localCache = new LocalCache(properties);

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
