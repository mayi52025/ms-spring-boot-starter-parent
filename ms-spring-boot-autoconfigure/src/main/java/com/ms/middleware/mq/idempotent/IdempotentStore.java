package com.ms.middleware.mq.idempotent;

/**
 * 幂等存储接口
 * 用于存储和管理消息幂等信息
 */
public interface IdempotentStore {

    /**
     * 获取锁
     * @param key 键
     * @param expiration 过期时间（毫秒）
     * @return 是否获取成功
     */
    boolean acquire(String key, long expiration);

    /**
     * 释放锁
     * @param key 键
     */
    void release(String key);

    /**
     * 检查是否存在
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 删除键
     * @param key 键
     */
    void delete(String key);
}
