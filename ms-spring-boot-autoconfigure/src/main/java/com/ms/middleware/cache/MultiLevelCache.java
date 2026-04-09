package com.ms.middleware.cache;

import com.ms.middleware.cache.consistency.CacheConsistencyManager;
import com.ms.middleware.cache.consistency.MultiLevelCacheConsistencyListener;
import com.ms.middleware.cache.stats.CacheStats;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final Object syncLock = new Object();

    public MultiLevelCache(LocalCache localCache, DistributedCache distributedCache) {
        this.localCache = localCache;
        this.distributedCache = distributedCache;
        this.stats = new CacheStats();
        // 初始化时检查Redis可用性
        checkRedisAvailability();
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
                // 检查Redis是否从不可用恢复
                checkRedisRecovery();
                return value;
            }

            try {
                value = distributedCache.get(key);
                if (value != null) {
                    localCache.put(key, value);
                    stats.recordHit();
                    // Redis可用，更新状态
                    redisAvailable.set(true);
                    return value;
                }
            } catch (Exception e) {
                // 分布式缓存不可用，降级到本地缓存
                redisAvailable.set(false);
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
                // Redis可用，更新状态
                redisAvailable.set(true);
            } catch (Exception e) {
                // 分布式缓存不可用，只写入本地缓存
                redisAvailable.set(false);
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
                // Redis可用，更新状态
                redisAvailable.set(true);
            } catch (Exception e) {
                // 分布式缓存不可用，只写入本地缓存
                redisAvailable.set(false);
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

    /**
     * 检查Redis可用性
     */
    private void checkRedisAvailability() {
        try {
            // 尝试执行一个简单的Redis操作
            distributedCache.exists("__redis_availability_check__");
            redisAvailable.set(true);
        } catch (Exception e) {
            redisAvailable.set(false);
        }
    }

    /**
     * 检查Redis是否从不可用恢复
     * 如果Redis恢复，同步本地缓存数据到Redis
     */
    private void checkRedisRecovery() {
        if (!redisAvailable.get()) {
            synchronized (syncLock) {
                if (!redisAvailable.get()) {
                    try {
                        // 尝试执行一个简单的Redis操作
                        distributedCache.exists("__redis_recovery_check__");
                        
                        // Redis恢复，同步本地缓存数据
                        System.out.println("[ms-middleware] Redis recovered, syncing local cache to Redis...");
                        
                        // 获取本地缓存中的所有键值对
                        Map<String, Object> localData = null;
                        if (localCache instanceof com.ms.middleware.cache.LocalCache) {
                            localData = ((com.ms.middleware.cache.LocalCache) localCache).getAll();
                        }
                        if (localData != null && !localData.isEmpty()) {
                            for (Map.Entry<String, Object> entry : localData.entrySet()) {
                                try {
                                    distributedCache.put(entry.getKey(), entry.getValue());
                                    System.out.println("[ms-middleware] Synced key: " + entry.getKey());
                                } catch (Exception e) {
                                    // 同步单个键失败，继续同步其他键
                                    System.err.println("[ms-middleware] Failed to sync key: " + entry.getKey() + ", error: " + e.getMessage());
                                }
                            }
                            System.out.println("[ms-middleware] Local cache sync to Redis completed: " + localData.size() + " keys synced");
                        }
                        
                        // 更新Redis可用状态
                        redisAvailable.set(true);
                    } catch (Exception e) {
                        // Redis仍然不可用
                        redisAvailable.set(false);
                    }
                }
            }
        }
    }
}
