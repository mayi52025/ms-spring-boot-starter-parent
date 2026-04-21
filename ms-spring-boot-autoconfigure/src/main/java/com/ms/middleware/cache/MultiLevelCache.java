package com.ms.middleware.cache;

import com.ms.middleware.cache.consistency.CacheConsistencyManager;
import com.ms.middleware.cache.consistency.MultiLevelCacheConsistencyListener;
import com.ms.middleware.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 多级缓存实现
 * 组合本地缓存和分布式缓存，提供双写策略
 */
public class MultiLevelCache implements MsCache {
    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCache.class);

    private final LocalCache localCache;
    private final DistributedCache distributedCache;
    private final CacheStats stats;
    private CacheConsistencyManager consistencyManager;
    private final ThreadLocal<Boolean> invalidating = ThreadLocal.withInitial(() -> false);
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final Set<String> pendingSyncKeys = ConcurrentHashMap.newKeySet();
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

            // 只有当Redis可用时才尝试从分布式缓存获取
            if (redisAvailable.get()) {
                try {
                    value = distributedCache.get(key);
                    if (value != null) {
                        localCache.put(key, value);
                        stats.recordHit();
                        return value;
                    }
                } catch (Exception e) {
                    // 分布式缓存不可用，降级到本地缓存
                    redisAvailable.set(false);
                    stats.recordError();
                    logger.warn("Redis get failed, degrade to local cache. key={}", key, e);
                }
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
            boolean wasUnavailable = !redisAvailable.get();
            
            // 只有当Redis可用时才尝试写入分布式缓存
            if (redisAvailable.get()) {
                try {
                    distributedCache.put(key, value);
                    pendingSyncKeys.remove(key);
                    if (wasUnavailable) {
                        checkRedisRecovery();
                    }
                } catch (Exception e) {
                    // 分布式缓存不可用，只写入本地缓存
                    redisAvailable.set(false);
                    pendingSyncKeys.add(key);
                    stats.recordError();
                    logger.warn("Redis put failed, degrade to local cache. key={}", key, e);
                }
            } else {
                // Redis不可用，只写入本地缓存
                pendingSyncKeys.add(key);
            }
            
            stats.recordPut();
            // 检查Redis是否从不可用恢复
            checkRedisRecovery();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to put value to multi-level cache", e);
        }
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        try {
            localCache.put(key, value, expire, timeUnit);
            boolean wasUnavailable = !redisAvailable.get();
            
            // 只有当Redis可用时才尝试写入分布式缓存
            if (redisAvailable.get()) {
                try {
                    distributedCache.put(key, value, expire, timeUnit);
                    pendingSyncKeys.remove(key);
                    if (wasUnavailable) {
                        checkRedisRecovery();
                    }
                } catch (Exception e) {
                    // 分布式缓存不可用，只写入本地缓存
                    redisAvailable.set(false);
                    pendingSyncKeys.add(key);
                    stats.recordError();
                    logger.warn("Redis put with ttl failed, degrade to local cache. key={}", key, e);
                }
            } else {
                // Redis不可用，只写入本地缓存
                pendingSyncKeys.add(key);
            }
            
            stats.recordPut();
            // 检查Redis是否从不可用恢复
            checkRedisRecovery();
        } catch (Exception e) {
            stats.recordError();
            throw new CacheException("Failed to put value to multi-level cache", e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            pendingSyncKeys.remove(key);
            localCache.remove(key);
            
            // 只有当Redis可用时才尝试从分布式缓存删除
            if (redisAvailable.get()) {
                try {
                    distributedCache.remove(key);
                    if (consistencyManager != null && !invalidating.get()) {
                        consistencyManager.invalidate(key);
                    }
                } catch (Exception e) {
                    // 分布式缓存不可用，只从本地缓存删除
                    redisAvailable.set(false);
                    stats.recordError();
                    logger.warn("Redis remove failed, degrade to local cache. key={}", key, e);
                }
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
            for (String key : keys) {
                pendingSyncKeys.remove(key);
            }
            localCache.remove(keys);
            
            // 只有当Redis可用时才尝试从分布式缓存删除
            if (redisAvailable.get()) {
                try {
                    distributedCache.remove(keys);
                    if (consistencyManager != null && !invalidating.get()) {
                        consistencyManager.invalidateBatch(java.util.Set.of(keys));
                    }
                } catch (Exception e) {
                    // 分布式缓存不可用，只从本地缓存删除
                    redisAvailable.set(false);
                    stats.recordError();
                    logger.warn("Redis remove batch failed, degrade to local cache.", e);
                }
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
            pendingSyncKeys.clear();
            localCache.clear();
            
            // 只有当Redis可用时才尝试清除分布式缓存
            if (redisAvailable.get()) {
                try {
                    distributedCache.clear();
                    if (consistencyManager != null && !invalidating.get()) {
                        consistencyManager.invalidateAll();
                    }
                } catch (Exception e) {
                    // 分布式缓存不可用，只清除本地缓存
                    redisAvailable.set(false);
                    stats.recordError();
                    logger.warn("Redis clear failed, degrade to local cache.", e);
                }
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
            
            // 只有当Redis可用时才尝试检查分布式缓存
            if (redisAvailable.get()) {
                try {
                    return distributedCache.exists(key);
                } catch (Exception e) {
                    // 分布式缓存不可用，只检查本地缓存
                    redisAvailable.set(false);
                    stats.recordError();
                    logger.warn("Redis exists check failed, degrade to local cache. key={}", key, e);
                }
            }
            
            return false;
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
                        // 测试环境中跳过Redis恢复检查
                        if (isTestEnvironment()) {
                            redisAvailable.set(true);
                            return;
                        }
                        
                        // 尝试执行一个简单的Redis操作，检查Redis是否恢复
                        boolean redisRecovered = false;
                        try {
                            distributedCache.exists("__redis_recovery_check__");
                            redisRecovered = true;
                        } catch (Exception e) {
                            // 第一次尝试失败，等待一段时间后重试
                            Thread.sleep(1000);
                            try {
                                distributedCache.exists("__redis_recovery_check__");
                                redisRecovered = true;
                            } catch (Exception ex) {
                                // 第二次尝试失败，Redis仍然不可用
                                redisRecovered = false;
                            }
                        }
                        
                        if (redisRecovered) {
                            // Redis恢复，同步本地缓存数据
                            System.out.println("[ms-middleware] Redis recovered, syncing local cache to Redis...");
                            syncPendingKeysToRedis();
                            // 更新Redis可用状态
                            redisAvailable.set(true);
                        } else {
                            // Redis仍然不可用
                            redisAvailable.set(false);
                            System.err.println("[ms-middleware] Redis is still unavailable");
                        }
                    } catch (Exception e) {
                        // Redis仍然不可用
                        redisAvailable.set(false);
                        System.err.println("[ms-middleware] Redis recovery check failed: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 检查是否是测试环境
     */
    private boolean isTestEnvironment() {
        try {
            // 检查是否有测试相关的类加载
            Class.forName("org.junit.jupiter.api.Test");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 同步Redis不可用期间写入失败的键到Redis
     */
    private void syncPendingKeysToRedis() {
        if (pendingSyncKeys.isEmpty()) {
            return;
        }
        if (!(localCache instanceof com.ms.middleware.cache.LocalCache)) {
            return;
        }

        Map<String, Object> localData = ((com.ms.middleware.cache.LocalCache) localCache).getAll();
        Set<String> keysToSync = new HashSet<>(pendingSyncKeys);
        int syncedCount = 0;
        int failedCount = 0;

        for (String key : keysToSync) {
            Object value = localData.get(key);
            // 本地已不存在则无需补偿
            if (value == null) {
                pendingSyncKeys.remove(key);
                continue;
            }
            try {
                distributedCache.put(key, value);
                pendingSyncKeys.remove(key);
                syncedCount++;
            } catch (Exception e) {
                failedCount++;
                System.err.println("[ms-middleware] Failed to sync key: " + key + ", error: " + e.getMessage());
            }
        }
        System.out.println("[ms-middleware] Pending cache sync completed: " + syncedCount + " keys synced, " + failedCount + " keys failed");
    }
}
