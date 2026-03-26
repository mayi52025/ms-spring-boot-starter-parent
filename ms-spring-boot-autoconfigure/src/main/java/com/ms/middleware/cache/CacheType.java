package com.ms.middleware.cache;

/**
 * 缓存类型枚举
 */
public enum CacheType {
    /**
     * 本地缓存
     */
    LOCAL,
    
    /**
     * 分布式缓存
     */
    DISTRIBUTED,
    
    /**
     * 多级缓存（本地 + 分布式）
     */
    MULTI_LEVEL
}
