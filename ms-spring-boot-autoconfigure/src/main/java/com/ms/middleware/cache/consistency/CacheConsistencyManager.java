package com.ms.middleware.cache.consistency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 缓存一致性管理器
 * 用于管理缓存失效监听器和处理缓存失效事件
 */
public class CacheConsistencyManager {

    private static final Logger logger = LoggerFactory.getLogger(CacheConsistencyManager.class);

    private final List<CacheInvalidationListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(CacheInvalidationListener listener) {
        if (listener != null && listener.isEnabled()) {
            listeners.add(listener);
            logger.debug("Added cache invalidation listener: {}", listener.getClass().getSimpleName());
        }
    }

    public void removeListener(CacheInvalidationListener listener) {
        listeners.remove(listener);
        logger.debug("Removed cache invalidation listener: {}", listener.getClass().getSimpleName());
    }

    public void invalidate(String key) {
        CacheInvalidationEvent event = CacheInvalidationEvent.single(key);
        notifyListeners(event);
    }

    public void invalidateBatch(Set<String> keys) {
        CacheInvalidationEvent event = CacheInvalidationEvent.batch(keys);
        notifyListeners(event);
    }

    public void invalidateAll() {
        CacheInvalidationEvent event = CacheInvalidationEvent.all();
        notifyListeners(event);
    }

    private void notifyListeners(CacheInvalidationEvent event) {
        logger.debug("Notifying listeners for cache invalidation event: {}", event.getType());

        for (CacheInvalidationListener listener : listeners) {
            try {
                listener.onInvalidation(event);
            } catch (Exception e) {
                logger.error("Error notifying cache invalidation listener: {}", listener.getClass().getSimpleName(), e);
            }
        }
    }

    public int getListenerCount() {
        return listeners.size();
    }
}
