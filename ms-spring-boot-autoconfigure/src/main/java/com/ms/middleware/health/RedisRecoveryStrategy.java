package com.ms.middleware.health;

import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redis恢复策略
 */
public class RedisRecoveryStrategy implements RecoveryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RedisRecoveryStrategy.class);
    private static final int PROBE_TIMEOUT_SECONDS = 3;
    private static final int MAX_ATTEMPTS = 3;

    private final AtomicReference<RedissonClient> redissonClientRef;
    private final Config config;

    public RedisRecoveryStrategy(AtomicReference<RedissonClient> redissonClientRef, Config config) {
        this.redissonClientRef = redissonClientRef;
        this.config = config;
    }

    @Override
    public boolean recover() {
        logger.info("Attempting to recover Redis connection...");
        RedissonClient existing = redissonClientRef.get();
        if (probe(existing)) {
            logger.info("Redis connection is already available");
            return true;
        }
        shutdownQuietly(existing);
        redissonClientRef.set(null);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                if (attempt > 1) {
                    Thread.sleep(1000L * attempt);
                }
                Config reconnectConfig = copyConfigForReconnect();
                RedissonClient newClient = org.redisson.Redisson.create(reconnectConfig);
                if (!probe(newClient)) {
                    shutdownQuietly(newClient);
                    continue;
                }
                redissonClientRef.set(newClient);
                logger.info("Redis connection recovered successfully on attempt {}", attempt);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                logger.warn("Redis recovery attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        logger.error("Failed to recover Redis connection after {} attempts", MAX_ATTEMPTS);
        return false;
    }

    private boolean probe(RedissonClient client) {
        if (client == null || client.isShutdown()) {
            return false;
        }
        try {
            client.getKeys().countAsync().get(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.debug("Redis probe failed: {}", e.getMessage());
            return false;
        }
    }

    private void shutdownQuietly(RedissonClient client) {
        if (client == null) {
            return;
        }
        try {
            client.shutdown();
        } catch (Exception e) {
            logger.warn("Failed to shutdown existing Redis connection: {}", e.getMessage());
        }
    }

    /** 每次重连使用独立 Config，避免污染共享实例 */
    private Config copyConfigForReconnect() {
        Config reconnect = new Config();
        reconnect.setLazyInitialization(false);
        if (config.isClusterConfig()) {
            reconnect.useClusterServers().setNodeAddresses(config.useClusterServers().getNodeAddresses());
        } else if (config.isSentinelConfig()) {
            var src = config.useSentinelServers();
            reconnect.useSentinelServers()
                    .setMasterName(src.getMasterName())
                    .setSentinelAddresses(src.getSentinelAddresses());
        } else {
            var src = config.useSingleServer();
            var dst = reconnect.useSingleServer()
                    .setAddress(src.getAddress())
                    .setDatabase(src.getDatabase())
                    .setConnectTimeout(src.getConnectTimeout())
                    .setTimeout(src.getTimeout())
                    .setRetryAttempts(src.getRetryAttempts())
                    .setRetryInterval(src.getRetryInterval());
            if (src.getPassword() != null) {
                dst.setPassword(src.getPassword());
            }
        }
        return reconnect;
    }
}
