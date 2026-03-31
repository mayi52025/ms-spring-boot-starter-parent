package com.ms.middleware.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁实现
 */
public class RedisDistributedLock implements DistributedLock {

    private static final Logger logger = LoggerFactory.getLogger(RedisDistributedLock.class);

    private final RedissonClient redissonClient;

    public RedisDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean lock(String key) {
        try {
            RLock lock = redissonClient.getLock(key);
            lock.lock();
            logger.debug("Acquired lock: {}", key);
            return true;
        } catch (Exception e) {
            logger.error("Failed to acquire lock: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean lock(String key, long timeout, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(key);
            lock.lock(timeout, unit);
            logger.debug("Acquired lock: {} with timeout: {} {}", key, timeout, unit);
            return true;
        } catch (Exception e) {
            logger.error("Failed to acquire lock: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean tryLock(String key, long waitTime, long timeout, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(key);
            boolean result = lock.tryLock(waitTime, timeout, unit);
            if (result) {
                logger.debug("Acquired lock: {} with waitTime: {} and timeout: {} {}", key, waitTime, timeout, unit);
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to try lock: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean unlock(String key) {
        try {
            RLock lock = redissonClient.getLock(key);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.debug("Released lock: {}", key);
                return true;
            }
            logger.debug("Lock: {} is not held by current thread", key);
            return false;
        } catch (Exception e) {
            logger.error("Failed to release lock: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            RLock lock = redissonClient.getLock(key);
            return lock.isLocked();
        } catch (Exception e) {
            logger.error("Failed to check lock existence: {}", key, e);
            return false;
        }
    }

    @Override
    public long getRemainingTime(String key, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(key);
            return lock.remainTimeToLive();
        } catch (Exception e) {
            logger.error("Failed to get remaining time: {}", key, e);
            return -1;
        }
    }

}