package com.ms.middleware.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口
 */
public interface DistributedLock {

    /**
     * 获取锁
     *
     * @param key 锁的键
     * @return 是否获取成功
     */
    boolean lock(String key);

    /**
     * 获取锁，带过期时间
     *
     * @param key     锁的键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    boolean lock(String key, long timeout, TimeUnit unit);

    /**
     * 尝试获取锁
     *
     * @param key      锁的键
     * @param waitTime 等待时间
     * @param timeout  过期时间
     * @param unit     时间单位
     * @return 是否获取成功
     */
    boolean tryLock(String key, long waitTime, long timeout, TimeUnit unit);

    /**
     * 释放锁
     *
     * @param key 锁的键
     * @return 是否释放成功
     */
    boolean unlock(String key);

    /**
     * 检查锁是否存在
     *
     * @param key 锁的键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 获取锁的剩余过期时间
     *
     * @param key 锁的键
     * @param unit 时间单位
     * @return 剩余过期时间
     */
    long getRemainingTime(String key, TimeUnit unit);

}