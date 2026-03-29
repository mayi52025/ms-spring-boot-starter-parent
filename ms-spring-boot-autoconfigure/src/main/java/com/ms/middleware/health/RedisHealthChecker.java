package com.ms.middleware.health;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis健康检查器
 */
public class RedisHealthChecker implements HealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthChecker.class);
    private final RedissonClient redissonClient;

    public RedisHealthChecker(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean checkHealth() {
        try {
            // 尝试执行一个简单的命令来检查Redis连接
            redissonClient.getKeys().count();
            return true;
        } catch (Exception e) {
            logger.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
