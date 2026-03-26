package com.ms.middleware.cache;

import java.util.concurrent.TimeUnit;

/**
 * 缓存接口
 */
public interface MsCache {
    
    /**
     * 获取缓存
     * @param key 缓存键
     * @param <T> 缓存值类型
     * @return 缓存值
     */
    <T> T get(String key);
    
    /**
     * 获取缓存，若不存在则返回默认值
     * @param key 缓存键
     * @param defaultValue 默认值
     * @param <T> 缓存值类型
     * @return 缓存值或默认值
     */
    <T> T get(String key, T defaultValue);
    
    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);
    
    /**
     * 设置缓存，带过期时间
     * @param key 缓存键
     * @param value 缓存值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     */
    void put(String key, Object value, long expire, TimeUnit timeUnit);
    
    /**
     * 删除缓存
     * @param key 缓存键
     */
    void remove(String key);
    
    /**
     * 批量删除缓存
     * @param keys 缓存键集合
     */
    void remove(String... keys);
    
    /**
     * 清除所有缓存
     */
    void clear();
    
    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 获取缓存大小
     * @return 缓存大小
     */
    long size();
    
    /**
     * 获取缓存类型
     * @return 缓存类型
     */
    CacheType getCacheType();
}
