package com.ms.middleware.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 故障自愈机制
 * 用于检测和自动恢复系统故障
 */
public class FaultSelfHealing {

    private static final Logger logger = LoggerFactory.getLogger(FaultSelfHealing.class);
    private static final FaultSelfHealing instance = new FaultSelfHealing();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
    private final Map<String, ComponentHealth> components = new ConcurrentHashMap<>();
    private final Map<String, RecoveryStrategy> recoveryStrategies = new ConcurrentHashMap<>();

    private FaultSelfHealing() {
        // 启动健康检查任务
        executorService.scheduleAtFixedRate(this::checkComponents, 10, 30, TimeUnit.SECONDS);
    }

    public static FaultSelfHealing getInstance() {
        return instance;
    }

    /**
     * 注册组件
     * @param componentName 组件名称
     * @param healthChecker 健康检查器
     * @param recoveryStrategy 恢复策略
     */
    public void registerComponent(String componentName, HealthChecker healthChecker, RecoveryStrategy recoveryStrategy) {
        components.put(componentName, new ComponentHealth(healthChecker, false));
        recoveryStrategies.put(componentName, recoveryStrategy);
        logger.info("Component registered: {}", componentName);
    }

    /**
     * 检查组件健康状态
     */
    private void checkComponents() {
        for (Map.Entry<String, ComponentHealth> entry : components.entrySet()) {
            String componentName = entry.getKey();
            ComponentHealth health = entry.getValue();

            try {
                boolean isHealthy = health.getHealthChecker().checkHealth();
                if (!isHealthy && !health.isRecovering()) {
                    logger.warn("Component {} is unhealthy, starting recovery...", componentName);
                    health.setRecovering(true);
                    recoverComponent(componentName);
                } else if (isHealthy && health.isRecovering()) {
                    logger.info("Component {} recovered successfully", componentName);
                    health.setRecovering(false);
                }
            } catch (Exception e) {
                logger.error("Error checking health for component {}", componentName, e);
            }
        }
    }

    /**
     * 恢复组件
     * @param componentName 组件名称
     */
    private void recoverComponent(String componentName) {
        RecoveryStrategy strategy = recoveryStrategies.get(componentName);
        if (strategy != null) {
            executorService.submit(() -> {
                try {
                    boolean recovered = strategy.recover();
                    if (recovered) {
                        logger.info("Component {} recovered successfully", componentName);
                        components.get(componentName).setRecovering(false);
                    } else {
                        logger.warn("Failed to recover component {}", componentName);
                        // 可以添加重试逻辑
                    }
                } catch (Exception e) {
                    logger.error("Error recovering component {}", componentName, e);
                    components.get(componentName).setRecovering(false);
                }
            });
        }
    }

    /**
     * 获取组件健康状态
     * @param componentName 组件名称
     * @return 健康状态
     */
    public boolean getComponentHealth(String componentName) {
        ComponentHealth health = components.get(componentName);
        if (health != null) {
            try {
                return health.getHealthChecker().checkHealth();
            } catch (Exception e) {
                logger.error("Error checking health for component {}", componentName, e);
                return false;
            }
        }
        return false;
    }

    /**
     * 关闭自愈服务
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 组件健康状态
     */
    private static class ComponentHealth {
        private final HealthChecker healthChecker;
        private boolean recovering;

        public ComponentHealth(HealthChecker healthChecker, boolean recovering) {
            this.healthChecker = healthChecker;
            this.recovering = recovering;
        }

        public HealthChecker getHealthChecker() {
            return healthChecker;
        }

        public boolean isRecovering() {
            return recovering;
        }

        public void setRecovering(boolean recovering) {
            this.recovering = recovering;
        }
    }
}
