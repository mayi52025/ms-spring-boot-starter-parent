package com.ms.middleware.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.cache.stats.CacheStats;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存实现
 * 使用 Caffeine 作为底层缓存实现
 */
public class LocalCache implements MsCache {

    private final Cache<String, Object> cache;
    private final MsMiddlewareProperties.LocalCacheProperties localCacheProperties;
    private final CacheStats stats;

    public LocalCache(MsMiddlewareProperties.LocalCacheProperties localCacheProperties) {
        this.localCacheProperties = localCacheProperties;
        this.stats = new CacheStats();
        this.cache = buildCache();
    }

    private Cache<String, Object> buildCache() {
        return Caffeine.newBuilder()
                .maximumSize(localCacheProperties.getSize())
                .expireAfterWrite(localCacheProperties.getTtl(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        T value = (T) cache.getIfPresent(key);
        if (value != null) {
            stats.recordHit();
        } else {
            stats.recordMiss();
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        T value = (T) cache.getIfPresent(key);
        if (value != null) {
            stats.recordHit();
        } else {
            stats.recordMiss();
        }
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
        stats.recordPut();
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        cache.put(key, value);
        stats.recordPut();
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
        stats.recordRemove();
    }

    @Override
    public void remove(String... keys) {
        for (String key : keys) {
            cache.invalidate(key);
            stats.recordRemove();
        }
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        stats.recordClear();
    }

    @Override
    public boolean exists(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.LOCAL;
    }

    public CacheStats getStats() {
        return stats;
    }
}
