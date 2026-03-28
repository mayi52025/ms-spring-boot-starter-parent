package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存实现
 * 结合本地缓存（L1）和分布式缓存（L2）
 */
public class MultiLevelCache implements MsCache {

    private final LocalCache localCache;
    private final DistributedCache distributedCache;

    public MultiLevelCache(LocalCache localCache, DistributedCache distributedCache) {
        this.localCache = localCache;
        this.distributedCache = distributedCache;
    }

    @Override
    public <T> T get(String key) {
        // 1. 先从本地缓存（L1）获取
        T value = localCache.get(key);
        if (value != null) {
            return value;
        }

        // 2. 本地缓存未命中，从分布式缓存（L2）获取
        value = distributedCache.get(key);
        if (value != null) {
            // 3. 将分布式缓存的数据同步到本地缓存
            localCache.put(key, value);
        }

        return value;
    }

    @Override
    public <T> T get(String key, T defaultValue) {
        T value = get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(String key, Object value) {
        // 双写策略：同时更新本地缓存和分布式缓存
        localCache.put(key, value);
        distributedCache.put(key, value);
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        // 双写策略：同时更新本地缓存和分布式缓存
        localCache.put(key, value, expire, timeUnit);
        distributedCache.put(key, value, expire, timeUnit);
    }

    @Override
    public void remove(String key) {
        // 双删策略：同时删除本地缓存和分布式缓存
        localCache.remove(key);
        distributedCache.remove(key);
    }

    @Override
    public void remove(String... keys) {
        // 双删策略：同时删除本地缓存和分布式缓存
        localCache.remove(keys);
        distributedCache.remove(keys);
    }

    @Override
    public void clear() {
        // 同时清空本地缓存和分布式缓存
        localCache.clear();
        distributedCache.clear();
    }

    @Override
    public boolean exists(String key) {
        // 先检查本地缓存，再检查分布式缓存
        return localCache.exists(key) || distributedCache.exists(key);
    }

    @Override
    public long size() {
        // 只返回本地缓存大小，因为分布式缓存大小获取成本高
        return localCache.size();
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.MULTI_LEVEL;
    }
}
