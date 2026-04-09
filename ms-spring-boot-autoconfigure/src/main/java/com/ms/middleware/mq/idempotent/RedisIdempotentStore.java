package com.ms.middleware.mq.idempotent;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的幂等存储实现
 */
public class RedisIdempotentStore implements IdempotentStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisIdempotentStore.class);

    private final RedissonClient redissonClient;

    public RedisIdempotentStore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean acquire(String key, long expiration) {
        try {
            return redissonClient.getLock(key).tryLock(0, expiration, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Failed to acquire lock: {}", key, e);
            // Redis不可用时，返回true，允许消息处理
            // 这样可以避免消息被跳过，确保消息能够被处理
            return true;
        }
    }

    @Override
    public void release(String key) {
        try {
            redissonClient.getLock(key).unlock();
        } catch (Exception e) {
            logger.error("Failed to release lock: {}", key, e);
            // Redis不可用时，忽略异常
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return redissonClient.getLock(key).isLocked();
        } catch (Exception e) {
            logger.error("Failed to check lock: {}", key, e);
            // Redis不可用时，返回false
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            redissonClient.getLock(key).forceUnlock();
        } catch (Exception e) {
            logger.error("Failed to delete lock: {}", key, e);
            // Redis不可用时，忽略异常
        }
    }
}
