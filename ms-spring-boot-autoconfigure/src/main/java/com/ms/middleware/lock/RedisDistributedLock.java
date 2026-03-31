package com.ms.middleware.lock;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.security.SecurityUtils;
import io.micrometer.core.instrument.Timer;
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
    private final MsMetrics metrics;
    private final MsMiddlewareProperties.SecurityProperties securityProperties;

    public RedisDistributedLock(RedissonClient redissonClient, MsMetrics metrics, MsMiddlewareProperties properties) {
        this.redissonClient = redissonClient;
        this.metrics = metrics;
        this.securityProperties = properties.getSecurity();
    }

    /**
     * 生成安全的锁键
     * @param key 原始键
     * @return 安全的锁键
     */
    private String generateSecureLockKey(String key) {
        if (securityProperties.isEnabled() && securityProperties.getLock().isAccessControlEnabled()) {
            return SecurityUtils.generateSecureLockKey(key, securityProperties.getLock().getKeyPrefix());
        }
        return key;
    }

    @Override
    public boolean lock(String key) {
        try {
            String secureKey = generateSecureLockKey(key);
            Timer.Sample sample = metrics.startLockAcquisition();
            RLock lock = redissonClient.getLock(secureKey);
            lock.lock();
            metrics.stopLockAcquisition(sample);
            logger.debug("Acquired lock: {}", key);
            metrics.incrementLockAcquired();
            return true;
        } catch (Exception e) {
            logger.error("Failed to acquire lock: {}", key, e);
            metrics.incrementLockFailed();
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public boolean lock(String key, long timeout, TimeUnit unit) {
        try {
            String secureKey = generateSecureLockKey(key);
            Timer.Sample sample = metrics.startLockAcquisition();
            RLock lock = redissonClient.getLock(secureKey);
            lock.lock(timeout, unit);
            metrics.stopLockAcquisition(sample);
            logger.debug("Acquired lock: {} with timeout: {} {}", key, timeout, unit);
            metrics.incrementLockAcquired();
            return true;
        } catch (Exception e) {
            logger.error("Failed to acquire lock: {}", key, e);
            metrics.incrementLockFailed();
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public boolean tryLock(String key, long waitTime, long timeout, TimeUnit unit) {
        try {
            String secureKey = generateSecureLockKey(key);
            Timer.Sample sample = metrics.startLockAcquisition();
            RLock lock = redissonClient.getLock(secureKey);
            boolean result = lock.tryLock(waitTime, timeout, unit);
            metrics.stopLockAcquisition(sample);
            if (result) {
                logger.debug("Acquired lock: {} with waitTime: {} and timeout: {} {}", key, waitTime, timeout, unit);
                metrics.incrementLockAcquired();
            } else {
                metrics.incrementLockFailed();
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to try lock: {}", key, e);
            metrics.incrementLockFailed();
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public boolean unlock(String key) {
        try {
            String secureKey = generateSecureLockKey(key);
            RLock lock = redissonClient.getLock(secureKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.debug("Released lock: {}", key);
                return true;
            }
            logger.debug("Lock: {} is not held by current thread", key);
            return false;
        } catch (Exception e) {
            logger.error("Failed to release lock: {}", key, e);
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            String secureKey = generateSecureLockKey(key);
            RLock lock = redissonClient.getLock(secureKey);
            return lock.isLocked();
        } catch (Exception e) {
            logger.error("Failed to check lock existence: {}", key, e);
            return false;
        }
    }

    @Override
    public long getRemainingTime(String key, TimeUnit unit) {
        try {
            String secureKey = generateSecureLockKey(key);
            RLock lock = redissonClient.getLock(secureKey);
            return lock.remainTimeToLive();
        } catch (Exception e) {
            logger.error("Failed to get remaining time: {}", key, e);
            return -1;
        }
    }

}