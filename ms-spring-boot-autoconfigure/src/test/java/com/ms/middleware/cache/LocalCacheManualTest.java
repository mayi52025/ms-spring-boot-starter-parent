package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;

/**
 * 本地缓存手动测试类
 * 可以直接运行 main 方法进行手动测试
 */
public class LocalCacheManualTest {

    public static void main(String[] args) {
        MsMiddlewareProperties.LocalCacheProperties properties = 
            new MsMiddlewareProperties.LocalCacheProperties();
        properties.setSize(100);
        properties.setTtl(60);
        properties.setRefreshInterval(30);

        LocalCache localCache = new LocalCache(properties);

        System.out.println("=== 本地缓存手动测试 ===");

        testBasicOperations(localCache);
        testBatchOperations(localCache);
        testCacheStats(localCache);
    }

    private static void testBasicOperations(LocalCache cache) {
        System.out.println("\n--- 基本操作测试 ---");

        cache.put("user:1", "张三");
        cache.put("user:2", "李四");
        cache.put("user:3", "王五");

        System.out.println("添加 3 个用户到缓存");

        String user1 = cache.get("user:1");
        System.out.println("获取 user:1 = " + user1);

        String user4 = cache.get("user:4", "默认用户");
        System.out.println("获取不存在的 user:4 = " + user4);

        boolean exists = cache.exists("user:1");
        System.out.println("user:1 是否存在 = " + exists);

        long size = cache.size();
        System.out.println("当前缓存大小 = " + size);

        cache.remove("user:1");
        System.out.println("删除 user:1");

        exists = cache.exists("user:1");
        System.out.println("user:1 是否存在 = " + exists);
    }

    private static void testBatchOperations(LocalCache cache) {
        System.out.println("\n--- 批量操作测试 ---");

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");
        cache.put("key5", "value5");

        System.out.println("添加 5 个键值对到缓存");
        System.out.println("当前缓存大小 = " + cache.size());

        cache.remove("key1", "key2", "key3");
        System.out.println("批量删除 key1, key2, key3");
        System.out.println("当前缓存大小 = " + cache.size());

        cache.clear();
        System.out.println("清空所有缓存");
        System.out.println("当前缓存大小 = " + cache.size());
    }

    private static void testCacheStats(LocalCache cache) {
        System.out.println("\n--- 缓存统计测试 ---");

        cache.put("stat:1", 100);
        cache.put("stat:2", 200);
        cache.put("stat:3", 300);

        System.out.println("缓存类型 = " + cache.getCacheType());
        System.out.println("当前缓存大小 = " + cache.size());
        System.out.println("stat:1 是否存在 = " + cache.exists("stat:1"));
        System.out.println("stat:4 是否存在 = " + cache.exists("stat:4"));

        System.out.println("\n=== 测试完成 ===");
    }
}
