package com.ms.middleware.cache;

import com.ms.middleware.cache.stats.CacheStats;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存实现
 * 结合本地缓存（L1）和分布式缓存（L2）
 */
public class MultiLevelCache implements MsCache {

    private final LocalCache localCache;
    private final DistributedCache distributedCache;
    private final CacheStats stats;

    public MultiLevelCache(LocalCache localCache, DistributedCache distributedCache) {
        this.localCache = localCache;
        this.distributedCache = distributedCache;
        this.stats = new CacheStats();
    }

    @Override
    public <T> T get(String key) {
        try {
            T value = localCache.get(key);
            if (value != null) {
                stats.recordHit();
                return value;
            }

            value = distributedCache.get(key);
            if (value != null) {
                localCache.put(key, value);
                stats.recordHit();
            } else {
                stats.recordMiss();
            }

            return value;
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to get value from multi-level cache", e);
        }
    }

    @Override
    public <T> T get(String key, T defaultValue) {
        T value = get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(String key, Object value) {
        try {
            localCache.put(key, value);
            distributedCache.put(key, value);
            stats.recordPut();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to put value to multi-level cache", e);
        }
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        try {
            localCache.put(key, value, expire, timeUnit);
            distributedCache.put(key, value, expire, timeUnit);
            stats.recordPut();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to put value to multi-level cache", e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            localCache.remove(key);
            distributedCache.remove(key);
            stats.recordRemove();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to remove value from multi-level cache", e);
        }
    }

    @Override
    public void remove(String... keys) {
        try {
            localCache.remove(keys);
            distributedCache.remove(keys);
            stats.recordRemove();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to remove values from multi-level cache", e);
        }
    }

    @Override
    public void clear() {
        try {
            localCache.clear();
            distributedCache.clear();
            stats.recordClear();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to clear multi-level cache", e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return localCache.exists(key) || distributedCache.exists(key);
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to check existence in multi-level cache", e);
        }
    }

    @Override
    public long size() {
        try {
            return localCache.size();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to get multi-level cache size", e);
        }
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.MULTI_LEVEL;
    }

    public CacheStats getStats() {
        return stats;
    }

    public CacheStats getLocalCacheStats() {
        return localCache.getStats();
    }

    public CacheStats getDistributedCacheStats() {
        return distributedCache.getStats();
    }
}
