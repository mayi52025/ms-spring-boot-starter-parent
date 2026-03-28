package com.ms.middleware.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ms.middleware.MsMiddlewareProperties;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存实现
 * 使用 Caffeine 作为底层缓存实现
 */
public class LocalCache implements MsCache {

    private final Cache<String, Object> cache;
    private final MsMiddlewareProperties.LocalCacheProperties localCacheProperties;

    public LocalCache(MsMiddlewareProperties.LocalCacheProperties localCacheProperties) {
        this.localCacheProperties = localCacheProperties;
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
        return (T) cache.getIfPresent(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        T value = (T) cache.getIfPresent(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        cache.put(key, value);
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }

    @Override
    public void remove(String... keys) {
        for (String key : keys) {
            cache.invalidate(key);
        }
    }

    @Override
    public void clear() {
        cache.invalidateAll();
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
}
