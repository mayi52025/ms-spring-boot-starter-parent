package com.ms.middleware.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.cache.stats.CacheStats;
import com.ms.middleware.metrics.MsMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存实现
 * 使用 Caffeine 作为底层缓存实现
 */
public class LocalCache implements MsCache {

    private final Cache<String, Object> cache;
    private final MsMiddlewareProperties.LocalCacheProperties localCacheProperties;
    private final CacheStats stats;
    private final MsMetrics metrics;
    private final Map<String, Long> customExpireTimes;

    public LocalCache(MsMiddlewareProperties.LocalCacheProperties localCacheProperties, MsMetrics metrics) {
        this.localCacheProperties = localCacheProperties;
        this.stats = new CacheStats();
        this.metrics = metrics;
        this.customExpireTimes = new ConcurrentHashMap<>();
        this.cache = buildCache();
    }

    private Cache<String, Object> buildCache() {
        return Caffeine.newBuilder()
                .maximumSize(localCacheProperties.getSize())
                .expireAfter(new Expiry<String, Object>() {
                    @Override
                    public long expireAfterCreate(String key, Object value, long currentTime) {
                        Long customExpire = customExpireTimes.get(key);
                        if (customExpire != null) {
                            return customExpire;
                        }
                        return TimeUnit.SECONDS.toNanos(localCacheProperties.getTtl());
                    }

                    @Override
                    public long expireAfterUpdate(String key, Object value, long currentTime, long currentDuration) {
                        Long customExpire = customExpireTimes.get(key);
                        if (customExpire != null) {
                            return customExpire;
                        }
                        return TimeUnit.SECONDS.toNanos(localCacheProperties.getTtl());
                    }

                    @Override
                    public long expireAfterRead(String key, Object value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        T value = (T) cache.getIfPresent(key);
        if (value != null) {
            stats.recordHit();
            metrics.incrementCacheHits();
        } else {
            stats.recordMiss();
            metrics.incrementCacheMisses();
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        T value = (T) cache.getIfPresent(key);
        if (value != null) {
            stats.recordHit();
            metrics.incrementCacheHits();
        } else {
            stats.recordMiss();
            metrics.incrementCacheMisses();
        }
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
        stats.recordPut();
        metrics.incrementCachePuts();
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        customExpireTimes.put(key, timeUnit.toNanos(expire));
        cache.put(key, value);
        stats.recordPut();
        metrics.incrementCachePuts();
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
        customExpireTimes.remove(key);
        stats.recordRemove();
    }

    @Override
    public void remove(String... keys) {
        for (String key : keys) {
            cache.invalidate(key);
            customExpireTimes.remove(key);
            stats.recordRemove();
        }
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        customExpireTimes.clear();
        stats.recordClear();
    }

    @Override
    public boolean exists(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public long size() {
        long size = cache.estimatedSize();
        metrics.setCacheSize(size);
        return size;
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.LOCAL;
    }

    public CacheStats getStats() {
        return stats;
    }

    /**
     * 获取本地缓存中的所有键值对
     * @return 本地缓存中的所有键值对
     */
    public Map<String, Object> getAll() {
        Map<String, Object> result = new HashMap<>();
        cache.asMap().forEach((key, value) -> result.put(key, value));
        return result;
    }
}
