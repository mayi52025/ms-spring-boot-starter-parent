package com.ms.middleware.cache;

import com.ms.middleware.cache.consistency.CacheConsistencyManager;
import com.ms.middleware.cache.consistency.MultiLevelCacheConsistencyListener;
import com.ms.middleware.cache.stats.CacheStats;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存实现
 * 组合本地缓存和分布式缓存，提供双写策略
 */
public class MultiLevelCache implements MsCache {

    private final LocalCache localCache;
    private final DistributedCache distributedCache;
    private final CacheStats stats;
    private CacheConsistencyManager consistencyManager;
    private final ThreadLocal<Boolean> invalidating = ThreadLocal.withInitial(() -> false);

    public MultiLevelCache(LocalCache localCache, DistributedCache distributedCache) {
        this.localCache = localCache;
        this.distributedCache = distributedCache;
        this.stats = new CacheStats();
    }

    public void setConsistencyManager(CacheConsistencyManager consistencyManager) {
        this.consistencyManager = consistencyManager;
        if (consistencyManager != null) {
            MultiLevelCacheConsistencyListener listener = new MultiLevelCacheConsistencyListener(this);
            consistencyManager.addListener(listener);
        }
    }

    public void invalidateFromListener(String key) {
        if (invalidating.get()) {
            return;
        }
        invalidating.set(true);
        try {
            localCache.remove(key);
            distributedCache.remove(key);
        } finally {
            invalidating.set(false);
        }
    }

    public void invalidateBatchFromListener(String... keys) {
        if (invalidating.get()) {
            return;
        }
        invalidating.set(true);
        try {
            localCache.remove(keys);
            distributedCache.remove(keys);
        } finally {
            invalidating.set(false);
        }
    }

    public void invalidateAllFromListener() {
        if (invalidating.get()) {
            return;
        }
        invalidating.set(true);
        try {
            localCache.clear();
            distributedCache.clear();
        } finally {
            invalidating.set(false);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            T value = localCache.get(key);
            if (value != null) {
                stats.recordHit();
                return value;
            }

            try {
                value = distributedCache.get(key);
                if (value != null) {
                    localCache.put(key, value);
                    stats.recordHit();
                    return value;
                }
            } catch (Exception e) {
                // 分布式缓存不可用，降级到本地缓存
                stats.recordError();
                // 只返回本地缓存的值（已经检查过为null）
            }

            stats.recordMiss();
            return null;
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to get value from multi-level cache", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        try {
            T value = get(key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to get value from multi-level cache", e);
        }
    }

    @Override
    public void put(String key, Object value) {
        try {
            localCache.put(key, value);
            try {
                distributedCache.put(key, value);
            } catch (Exception e) {
                // 分布式缓存不可用，只写入本地缓存
                stats.recordError();
            }
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
            try {
                distributedCache.put(key, value, expire, timeUnit);
            } catch (Exception e) {
                // 分布式缓存不可用，只写入本地缓存
                stats.recordError();
            }
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
            try {
                distributedCache.remove(key);
                if (consistencyManager != null && !invalidating.get()) {
                    consistencyManager.invalidate(key);
                }
            } catch (Exception e) {
                // 分布式缓存不可用，只从本地缓存删除
                stats.recordError();
            }
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
            try {
                distributedCache.remove(keys);
                if (consistencyManager != null && !invalidating.get()) {
                    consistencyManager.invalidateBatch(java.util.Set.of(keys));
                }
            } catch (Exception e) {
                // 分布式缓存不可用，只从本地缓存删除
                stats.recordError();
            }
            for (String key : keys) {
                stats.recordRemove();
            }
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to remove values from multi-level cache", e);
        }
    }

    @Override
    public void clear() {
        try {
            localCache.clear();
            try {
                distributedCache.clear();
                if (consistencyManager != null && !invalidating.get()) {
                    consistencyManager.invalidateAll();
                }
            } catch (Exception e) {
                // 分布式缓存不可用，只清除本地缓存
                stats.recordError();
            }
            stats.recordClear();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to clear multi-level cache", e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            boolean existsInLocal = localCache.exists(key);
            if (existsInLocal) {
                return true;
            }
            try {
                return distributedCache.exists(key);
            } catch (Exception e) {
                // 分布式缓存不可用，只检查本地缓存
                stats.recordError();
                return false;
            }
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

    public LocalCache getLocalCache() {
        return localCache;
    }

    public DistributedCache getDistributedCache() {
        return distributedCache;
    }
}
