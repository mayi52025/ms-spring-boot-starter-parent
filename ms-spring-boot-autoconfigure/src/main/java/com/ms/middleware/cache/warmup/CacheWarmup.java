package com.ms.middleware.cache.warmup;

import java.util.List;
import java.util.Map;

/**
 * 缓存预热接口
 * 用于在应用启动时加载热点数据到缓存
 */
public interface CacheWarmup {

    /**
     * 获取预热数据
     * @return 预热数据映射（键 -> 值）
     */
    Map<String, Object> getWarmupData();

    /**
     * 获取预热数据列表
     * @return 预热数据列表
     */
    default List<WarmupItem> getWarmupItems() {
        return List.of();
    }

    /**
     * 是否启用预热
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 预热项
     */
    record WarmupItem(String key, Object value, long expireSeconds) {
        public WarmupItem(String key, Object value) {
            this(key, value, 0);
        }
    }
}
