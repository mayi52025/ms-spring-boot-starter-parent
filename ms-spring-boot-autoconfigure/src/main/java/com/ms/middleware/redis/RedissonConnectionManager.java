package com.ms.middleware.redis;

import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redisson 连接统一管理：single-flight 恢复，避免缓存/自愈/自治多层并发重建客户端。
 */
public class RedissonConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(RedissonConnectionManager.class);
    private static final int MAX_ATTEMPTS = 3;

    private final AtomicReference<RedissonClient> clientRef;
    private final Config templateConfig;
    private final Object recoveryMonitor = new Object();
    private final List<Runnable> afterRecoverCallbacks = new CopyOnWriteArrayList<>();

    public RedissonConnectionManager(AtomicReference<RedissonClient> clientRef, Config templateConfig) {
        this.clientRef = clientRef;
        this.templateConfig = templateConfig;
    }

    public AtomicReference<RedissonClient> getClientRef() {
        return clientRef;
    }

    public RedissonClient getClient() {
        return clientRef.get();
    }

    public void addAfterRecoverCallback(Runnable callback) {
        if (callback != null) {
            afterRecoverCallbacks.add(callback);
        }
    }

    /** 探活通过则直接返回；否则触发 single-flight 恢复 */
    public boolean ensureAvailable() {
        if (RedissonProbes.isAvailable(clientRef.get())) {
            return true;
        }
        return recover();
    }

    /**
     * 全局唯一恢复入口（synchronized single-flight）。
     */
    public boolean recover() {
        synchronized (recoveryMonitor) {
            RedissonClient existing = clientRef.get();
            if (RedissonProbes.isAvailable(existing)) {
                return true;
            }
            RedissonProbes.shutdownQuietly(existing);
            clientRef.set(null);

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                RedissonClient candidate = null;
                try {
                    if (attempt > 1) {
                        Thread.sleep(1000L * attempt);
                    }
                    candidate = org.redisson.Redisson.create(copyConfigForReconnect());
                    if (!RedissonProbes.isAvailable(candidate)) {
                        RedissonProbes.shutdownQuietly(candidate);
                        continue;
                    }
                    clientRef.set(candidate);
                    logger.info("Redisson connection recovered on attempt {}", attempt);
                    notifyAfterRecover();
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    RedissonProbes.shutdownQuietly(candidate);
                    return false;
                } catch (Exception e) {
                    RedissonProbes.shutdownQuietly(candidate);
                    logger.warn("Redisson recovery attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                }
            }
            logger.error("Redisson recovery failed after {} attempts", MAX_ATTEMPTS);
            return false;
        }
    }

    private void notifyAfterRecover() {
        for (Runnable callback : afterRecoverCallbacks) {
            try {
                callback.run();
            } catch (Exception e) {
                logger.warn("After-recover callback failed: {}", e.getMessage());
            }
        }
    }

    private Config copyConfigForReconnect() {
        Config reconnect = new Config();
        reconnect.setLazyInitialization(false);
        if (templateConfig.isClusterConfig()) {
            reconnect.useClusterServers().setNodeAddresses(templateConfig.useClusterServers().getNodeAddresses());
        } else if (templateConfig.isSentinelConfig()) {
            var src = templateConfig.useSentinelServers();
            reconnect.useSentinelServers()
                    .setMasterName(src.getMasterName())
                    .setSentinelAddresses(src.getSentinelAddresses());
        } else {
            var src = templateConfig.useSingleServer();
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
