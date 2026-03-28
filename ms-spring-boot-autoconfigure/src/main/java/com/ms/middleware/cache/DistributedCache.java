package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
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

    public DistributedCache(RedissonClient redissonClient, 
                           MsMiddlewareProperties.DistributedCacheProperties distributedCacheProperties) {
        this.redissonClient = redissonClient;
        this.distributedCacheProperties = distributedCacheProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        T value = bucket.get();
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(String key, Object value) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value, distributedCacheProperties.getTtl(), TimeUnit.SECONDS);
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value, expire, timeUnit);
    }

    @Override
    public void remove(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public void remove(String... keys) {
        for (String key : keys) {
            redissonClient.getBucket(key).delete();
        }
    }

    @Override
    public void clear() {
        redissonClient.getKeys().deleteByPattern("*");
    }

    @Override
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    @Override
    public long size() {
        // 注意：这个方法会遍历所有键，可能影响性能
        return redissonClient.getKeys().count();
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.DISTRIBUTED;
    }
}
