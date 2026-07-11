package com.ms.middleware.mq.idempotent;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于Redis的幂等存储实现
 */
public class RedisIdempotentStore implements IdempotentStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisIdempotentStore.class);

    private final AtomicReference<RedissonClient> redissonClientRef;

    public RedisIdempotentStore(AtomicReference<RedissonClient> redissonClientRef) {
        this.redissonClientRef = redissonClientRef;
    }

    private RedissonClient client() {
        return redissonClientRef.get();
    }

    @Override
    public boolean acquire(String key, long expiration) {
        try {
            return client().getLock(key).tryLock(0, expiration, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Failed to acquire lock: {}", key, e);
            return true;
        }
    }

    @Override
    public void release(String key) {
        try {
            client().getLock(key).unlock();
        } catch (Exception e) {
            logger.error("Failed to release lock: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return client().getLock(key).isLocked();
        } catch (Exception e) {
            logger.error("Failed to check lock: {}", key, e);
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            client().getLock(key).forceUnlock();
        } catch (Exception e) {
            logger.error("Failed to delete lock: {}", key, e);
        }
    }
}
