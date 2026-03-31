package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.cache.stats.CacheStats;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.security.SecurityUtils;
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
    private final MsMiddlewareProperties.SecurityProperties securityProperties;
    private final CacheStats stats;
    private final MsMetrics metrics;

    public DistributedCache(RedissonClient redissonClient, 
                           MsMiddlewareProperties.DistributedCacheProperties distributedCacheProperties, 
                           MsMiddlewareProperties properties, 
                           MsMetrics metrics) {
        this.redissonClient = redissonClient;
        this.distributedCacheProperties = distributedCacheProperties;
        this.securityProperties = properties.getSecurity();
        this.stats = new CacheStats();
        this.metrics = metrics;
    }

    /**
     * 生成安全的缓存键
     * @param key 原始键
     * @return 安全的缓存键
     */
    private String generateSecureCacheKey(String key) {
        if (securityProperties.isEnabled() && securityProperties.getCache().isAccessControlEnabled()) {
            return SecurityUtils.generateSecureCacheKey(key, securityProperties.getCache().getKeyPrefix());
        }
        return key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            String secureKey = generateSecureCacheKey(key);
            RBucket<T> bucket = redissonClient.getBucket(secureKey);
            T value = bucket.get();
            if (value != null) {
                stats.recordHit();
                metrics.incrementCacheHits();
            } else {
                stats.recordMiss();
                metrics.incrementCacheMisses();
            }
            return value;
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
            throw new CacheException("Failed to get value from distributed cache", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        try {
            String secureKey = generateSecureCacheKey(key);
            RBucket<T> bucket = redissonClient.getBucket(secureKey);
            T value = bucket.get();
            if (value != null) {
                stats.recordHit();
                metrics.incrementCacheHits();
            } else {
                stats.recordMiss();
                metrics.incrementCacheMisses();
            }
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
            throw new CacheException("Failed to get value from distributed cache", e);
        }
    }

    @Override
    public void put(String key, Object value) {
        try {
            String secureKey = generateSecureCacheKey(key);
            RBucket<Object> bucket = redissonClient.getBucket(secureKey);
            bucket.set(value, distributedCacheProperties.getTtl(), TimeUnit.SECONDS);
            stats.recordPut();
            metrics.incrementCachePuts();
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
            throw new CacheException("Failed to put value to distributed cache", e);
        }
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        try {
            String secureKey = generateSecureCacheKey(key);
            RBucket<Object> bucket = redissonClient.getBucket(secureKey);
            bucket.set(value, expire, timeUnit);
            stats.recordPut();
            metrics.incrementCachePuts();
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
            throw new CacheException("Failed to put value to distributed cache", e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            String secureKey = generateSecureCacheKey(key);
            redissonClient.getBucket(secureKey).delete();
            stats.recordRemove();
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
            throw new CacheException("Failed to remove value from distributed cache", e);
        }
    }

    @Override
    public void remove(String... keys) {
        try {
            for (String key : keys) {
                String secureKey = generateSecureCacheKey(key);
                redissonClient.getBucket(secureKey).delete();
                stats.recordRemove();
            }
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
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
            metrics.incrementFailureCount();
            throw new CacheException("Failed to clear distributed cache", e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            String secureKey = generateSecureCacheKey(key);
            return redissonClient.getBucket(secureKey).isExists();
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
            throw new CacheException("Failed to check existence in distributed cache", e);
        }
    }

    @Override
    public long size() {
        try {
            long size = redissonClient.getKeys().count();
            metrics.setCacheSize(size);
            return size;
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
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
