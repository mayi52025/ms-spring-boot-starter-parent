package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.cache.stats.CacheStats;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存实现
 * 使用 Redisson 作为底层缓存实现
 */
public class DistributedCache implements MsCache {

    private final RedissonClient redissonClient;
    private final MsMiddlewareProperties.DistributedCacheProperties distributedCacheProperties;
    private final CacheStats stats;

    public DistributedCache(RedissonClient redissonClient, 
                           MsMiddlewareProperties.DistributedCacheProperties distributedCacheProperties) {
        this.redissonClient = redissonClient;
        this.distributedCacheProperties = distributedCacheProperties;
        this.stats = new CacheStats();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            T value = bucket.get();
            if (value != null) {
                stats.recordHit();
            } else {
                stats.recordMiss();
            }
            return value;
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to get value from distributed cache", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            T value = bucket.get();
            if (value != null) {
                stats.recordHit();
            } else {
                stats.recordMiss();
            }
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to get value from distributed cache", e);
        }
    }

    @Override
    public void put(String key, Object value) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            bucket.set(value, distributedCacheProperties.getTtl(), TimeUnit.SECONDS);
            stats.recordPut();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to put value to distributed cache", e);
        }
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            bucket.set(value, expire, timeUnit);
            stats.recordPut();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to put value to distributed cache", e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            redissonClient.getBucket(key).delete();
            stats.recordRemove();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to remove value from distributed cache", e);
        }
    }

    @Override
    public void remove(String... keys) {
        try {
            for (String key : keys) {
                redissonClient.getBucket(key).delete();
                stats.recordRemove();
            }
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to remove values from distributed cache", e);
        }
    }

    @Override
    public void clear() {
        try {
            redissonClient.getKeys().deleteByPattern("*");
            stats.recordClear();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to clear distributed cache", e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return redissonClient.getBucket(key).isExists();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to check existence in distributed cache", e);
        }
    }

    @Override
    public long size() {
        try {
            return redissonClient.getKeys().count();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to get distributed cache size", e);
        }
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.DISTRIBUTED;
    }

    public CacheStats getStats() {
        return stats;
    }
}
