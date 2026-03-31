package com.ms.middleware.rate;

import java.util.concurrent.TimeUnit;

/**
 * 限流接口
 */
public interface RateLimiter {

    /**
     * 尝试获取令牌
     *
     * @param key 限流键
     * @param limit 限制数量
     * @param window 时间窗口
     * @param unit 时间单位
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, int limit, long window, TimeUnit unit);

    /**
     * 尝试获取令牌（令牌桶算法）
     *
     * @param key 限流键
     * @param limit 限制数量
     * @param window 时间窗口
     * @param unit 时间单位
     * @param capacity 令牌桶容量
     * @return 是否获取成功
     */
    boolean tryAcquireWithTokenBucket(String key, int limit, long window, TimeUnit unit, int capacity);

    /**
     * 尝试获取令牌（滑动窗口算法）
     *
     * @param key 限流键
     * @param limit 限制数量
     * @param window 时间窗口
     * @param unit 时间单位
     * @return 是否获取成功
     */
    boolean tryAcquireWithSlidingWindow(String key, int limit, long window, TimeUnit unit);

    /**
     * 获取当前计数
     *
     * @param key 限流键
     * @return 当前计数
     */
    long getCurrentCount(String key);

    /**
     * 重置限流计数
     *
     * @param key 限流键
     * @return 是否重置成功
     */
    boolean reset(String key);

}