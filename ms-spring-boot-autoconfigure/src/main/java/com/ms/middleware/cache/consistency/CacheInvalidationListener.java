package com.ms.middleware.cache.consistency;

/**
 * 缓存失效监听器
 * 用于监听缓存失效事件
 */
public interface CacheInvalidationListener {

    /**
     * 处理缓存失效事件
     * @param event 缓存失效事件
     */
    void onInvalidation(CacheInvalidationEvent event);

    /**
     * 是否启用
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}
