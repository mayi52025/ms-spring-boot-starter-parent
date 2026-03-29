package com.ms.middleware.cache.consistency;

import java.util.Set;

/**
 * 缓存失效事件
 * 用于通知缓存失效
 */
public class CacheInvalidationEvent {

    /**
     * 失效类型
     */
    public enum InvalidationType {
        /**
         * 单个键失效
         */
        SINGLE,
        /**
         * 批量键失效
         */
        BATCH,
        /**
         * 全部失效
         */
        ALL
    }

    private final InvalidationType type;
    private final String key;
    private final Set<String> keys;
    private final long timestamp;

    private CacheInvalidationEvent(InvalidationType type, String key, Set<String> keys) {
        this.type = type;
        this.key = key;
        this.keys = keys;
        this.timestamp = System.currentTimeMillis();
    }

    public static CacheInvalidationEvent single(String key) {
        return new CacheInvalidationEvent(InvalidationType.SINGLE, key, null);
    }

    public static CacheInvalidationEvent batch(Set<String> keys) {
        return new CacheInvalidationEvent(InvalidationType.BATCH, null, keys);
    }

    public static CacheInvalidationEvent all() {
        return new CacheInvalidationEvent(InvalidationType.ALL, null, null);
    }

    public InvalidationType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public Set<String> getKeys() {
        return keys;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
