package com.ms.middleware.health;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            if (client == null) {
                logger.warn("Redis client is null");
                return false;
            }
            // 尝试执行一个简单的命令来检查Redis连接
            client.getKeys().count();
            return true;
        } catch (Exception e) {
            logger.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
