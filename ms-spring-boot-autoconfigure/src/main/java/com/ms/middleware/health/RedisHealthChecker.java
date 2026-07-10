package com.ms.middleware.health;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redis健康检查器
 */
public class RedisHealthChecker implements HealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthChecker.class);
    private final AtomicReference<RedissonClient> redissonClientRef;

    public RedisHealthChecker(AtomicReference<RedissonClient> redissonClientRef) {
        this.redissonClientRef = redissonClientRef;
    }

    @Override
    public boolean checkHealth() {
        try {
            RedissonClient client = redissonClientRef.get();
            if (client == null || client.isShutdown()) {
                logger.warn("Redis client is null or shutdown");
                return false;
            }
            // 短超时探活，避免 Redis 宕机时阻塞自治扫描线程
            client.getKeys().countAsync().get(3, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
