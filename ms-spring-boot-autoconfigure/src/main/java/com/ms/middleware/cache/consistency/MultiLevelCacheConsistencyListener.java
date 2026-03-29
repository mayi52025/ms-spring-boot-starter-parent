package com.ms.middleware.cache.consistency;

import com.ms.middleware.cache.MultiLevelCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 多级缓存一致性监听器
 * 确保本地缓存和分布式缓存的一致性
 */
public class MultiLevelCacheConsistencyListener implements CacheInvalidationListener {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheConsistencyListener.class);

    private final MultiLevelCache multiLevelCache;

    public MultiLevelCacheConsistencyListener(MultiLevelCache multiLevelCache) {
        this.multiLevelCache = multiLevelCache;
    }

    @Override
    public void onInvalidation(CacheInvalidationEvent event) {
        switch (event.getType()) {
            case SINGLE:
                invalidateSingle(event.getKey());
                break;
            case BATCH:
                invalidateBatch(event.getKeys());
                break;
            case ALL:
                invalidateAll();
                break;
            default:
                logger.warn("Unknown cache invalidation type: {}", event.getType());
        }
    }

    private void invalidateSingle(String key) {
        try {
            multiLevelCache.invalidateFromListener(key);
        } catch (Exception e) {
            logger.error("Failed to invalidate cache for key: {}", key, e);
        }
    }

    private void invalidateBatch(Set<String> keys) {
        try {
            multiLevelCache.invalidateBatchFromListener(keys.toArray(new String[0]));
        } catch (Exception e) {
            logger.error("Failed to invalidate batch cache for keys: {}", keys, e);
        }
    }

    private void invalidateAll() {
        try {
            multiLevelCache.invalidateAllFromListener();
        } catch (Exception e) {
            logger.error("Failed to invalidate all cache", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
