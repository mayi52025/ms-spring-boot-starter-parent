package com.ms.middleware.health;

import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redis恢复策略
 */
public class RedisRecoveryStrategy implements RecoveryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RedisRecoveryStrategy.class);
    private final AtomicReference<RedissonClient> redissonClientRef;
    private final Config config;

    public RedisRecoveryStrategy(AtomicReference<RedissonClient> redissonClientRef, Config config) {
        this.redissonClientRef = redissonClientRef;
        this.config = config;
    }

    @Override
    public boolean recover() {
        try {
            logger.info("Attempting to recover Redis connection...");
            
            RedissonClient redissonClient = redissonClientRef.get();
            
            // 检查客户端是否为null或已关闭
            boolean needReconnect = false;
            try {
                if (redissonClient == null) {
                    needReconnect = true;
                } else {
                    // 尝试执行一个简单的命令来检查Redis连接
                    redissonClient.getKeys().count();
                    logger.info("Redis connection is already available");
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Redis connection is not available, trying to reconnect...");
                needReconnect = true;
            }
            
            // 关闭现有连接
            if (redissonClient != null) {
                try {
                    redissonClient.shutdown();
                } catch (Exception e) {
                    logger.warn("Failed to shutdown existing Redis connection: {}", e.getMessage());
                }
            }
            
            // 等待一段时间后重新创建连接
            Thread.sleep(2000);
            
            // 重新创建连接
            RedissonClient newRedissonClient = org.redisson.Redisson.create(config);
            
            // 测试新连接
            newRedissonClient.getKeys().count();
            
            // 替换旧的客户端
            redissonClientRef.set(newRedissonClient);
            
            logger.info("Redis connection recovered successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to recover Redis connection: {}", e.getMessage());
            return false;
        }
    }
}
