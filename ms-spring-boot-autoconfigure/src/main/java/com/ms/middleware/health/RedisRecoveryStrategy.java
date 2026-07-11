package com.ms.middleware.health;

import com.ms.middleware.redis.RedissonConnectionManager;

/**
 * Redis恢复策略：委托 {@link RedissonConnectionManager} 做 single-flight 重连。
 */
public class RedisRecoveryStrategy implements RecoveryStrategy {

    private final RedissonConnectionManager connectionManager;

    public RedisRecoveryStrategy(RedissonConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public boolean recover() {
        return connectionManager.recover();
    }
}
