package com.ms.middleware.health;

import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis恢复策略
 */
public class RedisRecoveryStrategy implements RecoveryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RedisRecoveryStrategy.class);
    private final RedissonClient redissonClient;
    private final Config config;

    public RedisRecoveryStrategy(RedissonClient redissonClient, Config config) {
        this.redissonClient = redissonClient;
        this.config = config;
    }

    @Override
    public boolean recover() {
        try {
            logger.info("Attempting to recover Redis connection...");
            // 关闭现有连接
            redissonClient.shutdown();
            // 重新创建连接
            org.redisson.Redisson.create(config);
            logger.info("Redis connection recovered successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to recover Redis connection: {}", e.getMessage());
            return false;
        }
    }
}
