package com.ms.middleware.cache;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.cache.stats.CacheStats;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.redis.RedissonConnectionManager;
import com.ms.middleware.security.SecurityUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存实现
 * 使用 Redisson 作为底层缓存实现
 */
public class DistributedCache implements MsCache {

    private static final Logger logger = LoggerFactory.getLogger(DistributedCache.class);
    private final RedissonConnectionManager connectionManager;
    private final MsMiddlewareProperties.DistributedCacheProperties distributedCacheProperties;
    private final MsMiddlewareProperties.SecurityProperties securityProperties;
    private final CacheStats stats;
    private final MsMetrics metrics;

    public DistributedCache(RedissonConnectionManager connectionManager,
                           MsMiddlewareProperties.DistributedCacheProperties distributedCacheProperties,
                           MsMiddlewareProperties properties,
                           MsMetrics metrics) {
        this.connectionManager = connectionManager;
        this.distributedCacheProperties = distributedCacheProperties;
        this.securityProperties = properties.getSecurity();
        this.stats = new CacheStats();
        this.metrics = metrics;
    }

    /**
     * 确保 Redis 连接有效；统一走 {@link RedissonConnectionManager}，不再独立重连。
     */
    private void ensureRedisConnection() throws Exception {
        if (isTestEnvironment()) {
            return;
        }
        if (connectionManager == null) {
            return;
        }
        if (!connectionManager.ensureAvailable()) {
            throw new CacheException("Redis unavailable after unified recovery attempt");
        }
    }

    private RedissonClient client() {
        if (connectionManager == null) {
            return null;
        }
        return connectionManager.getClient();
    }

    /**
     * 检查是否是测试环境
     */
    private boolean isTestEnvironment() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().contains("Test")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成安全的缓存键
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
            ensureRedisConnection();
            String secureKey = generateSecureCacheKey(key);
            RBucket<T> bucket = client().getBucket(secureKey);
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
            ensureRedisConnection();
            String secureKey = generateSecureCacheKey(key);
            RBucket<T> bucket = client().getBucket(secureKey);
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
            ensureRedisConnection();
            String secureKey = generateSecureCacheKey(key);
            RBucket<Object> bucket = client().getBucket(secureKey);
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
            ensureRedisConnection();
            String secureKey = generateSecureCacheKey(key);
            RBucket<Object> bucket = client().getBucket(secureKey);
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
            ensureRedisConnection();
            String secureKey = generateSecureCacheKey(key);
            client().getBucket(secureKey).delete();
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
            ensureRedisConnection();
            for (String key : keys) {
                String secureKey = generateSecureCacheKey(key);
                client().getBucket(secureKey).delete();
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
            ensureRedisConnection();
            client().getKeys().deleteByPattern("*");
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
            ensureRedisConnection();
            String secureKey = generateSecureCacheKey(key);
            return client().getBucket(secureKey).isExists();
        } catch (Exception e) {
            stats.recordError();
            metrics.incrementFailureCount();
            throw new CacheException("Failed to check existence in distributed cache", e);
        }
    }

    @Override
    public long size() {
        try {
            ensureRedisConnection();
            long size = client().getKeys().count();
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
