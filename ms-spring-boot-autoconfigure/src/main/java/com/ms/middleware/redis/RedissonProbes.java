package com.ms.middleware.redis;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 连接探活工具。
 */
public final class RedissonProbes {

    private static final Logger logger = LoggerFactory.getLogger(RedissonProbes.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 3;

    private RedissonProbes() {
    }

    public static boolean isAvailable(RedissonClient client) {
        return isAvailable(client, DEFAULT_TIMEOUT_SECONDS);
    }

    public static boolean isAvailable(RedissonClient client, int timeoutSeconds) {
        if (client == null || client.isShutdown()) {
            return false;
        }
        try {
            client.getKeys().countAsync().get(timeoutSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.debug("Redisson probe failed: {}", e.getMessage());
            return false;
        }
    }

    public static void shutdownQuietly(RedissonClient client) {
        if (client == null) {
            return;
        }
        try {
            client.shutdown();
        } catch (Exception e) {
            logger.warn("Failed to shutdown Redisson client: {}", e.getMessage());
        }
    }
}
