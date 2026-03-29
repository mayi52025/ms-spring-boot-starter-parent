package com.ms.middleware.health;

/**
 * 健康检查器接口
 * 用于检查组件的健康状态
 */
@FunctionalInterface
public interface HealthChecker {
    /**
     * 检查健康状态
     * @return 是否健康
     */
    boolean checkHealth();
}
