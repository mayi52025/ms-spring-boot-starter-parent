package com.ms.middleware.autonomy.act;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.ai.HotKeyManager;
import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.health.FaultSelfHealing;
import com.ms.middleware.mq.MsMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 自治动作执行器：把 {@link AutonomyActionType} 映射到现有中间件能力。
 *
 * <p>不重复实现缓存/MQ 逻辑，而是桥接 {@link FaultSelfHealing}、{@link HotKeyManager}、
 * {@link MqConsumerThrottle}、{@link MqDelayedRetryExecutor} 等组件。</p>
 */
public class AutonomyActuator {

    private static final Logger logger = LoggerFactory.getLogger(AutonomyActuator.class);

    private final FaultSelfHealing faultSelfHealing;
    private final ObjectProvider<HotKeyManager> hotKeyManagerProvider;
    private final ObjectProvider<MqConsumerThrottle> consumerThrottleProvider;
    private final ObjectProvider<MqDelayedRetryExecutor> delayedRetryExecutorProvider;
    private final MsMiddlewareProperties properties;

    public AutonomyActuator(FaultSelfHealing faultSelfHealing,
                            ObjectProvider<HotKeyManager> hotKeyManagerProvider,
                            ObjectProvider<MqConsumerThrottle> consumerThrottleProvider,
                            ObjectProvider<MqDelayedRetryExecutor> delayedRetryExecutorProvider,
                            MsMiddlewareProperties properties) {
        this.faultSelfHealing = faultSelfHealing;
        this.hotKeyManagerProvider = hotKeyManagerProvider;
        this.consumerThrottleProvider = consumerThrottleProvider;
        this.delayedRetryExecutorProvider = delayedRetryExecutorProvider;
        this.properties = properties;
    }

    /**
     * 执行单个计划动作，结果写回 action.executionStatus / executionDetail。
     */
    public void execute(PlannedAction action) {
        AutonomyActionType type = action.getActionType();
        try {
            switch (type) {
                case ENSURE_L1_DEGRADE -> {
                    action.setExecutionStatus("SUCCESS");
                    action.setExecutionDetail("已依赖 MultiLevelCache 请求路径自动降级至 L1，无需额外调用");
                }
                case WARMUP_HOT_KEYS -> executeWarmup(action);
                case TRIGGER_REDIS_RECOVERY -> executeRedisRecovery(action);
                case TRIGGER_RABBITMQ_RECOVERY -> executeRabbitRecovery(action);
                case THROTTLE_CONSUMER -> executeThrottle(action);
                case DELAYED_RETRY_BATCH -> executeDelayedRetry(action);
                default -> {
                    action.setExecutionStatus("SKIPPED");
                    action.setExecutionDetail("未知动作");
                }
            }
        } catch (Exception e) {
            logger.error("Autonomy action failed: {}", type, e);
            action.setExecutionStatus("FAILED");
            action.setExecutionDetail(e.getMessage());
        }
    }

    /** MQ 故障恢复后关闭消费限流 */
    public void clearMqThrottle() {
        MqConsumerThrottle throttle = consumerThrottleProvider.getIfAvailable();
        if (throttle != null) {
            throttle.disable();
        }
    }

    private void executeWarmup(PlannedAction action) {
        HotKeyManager manager = hotKeyManagerProvider.getIfAvailable();
        if (manager == null) {
            action.setExecutionStatus("SKIPPED");
            action.setExecutionDetail("HotKeyManager 未启用，跳过预热");
        } else {
            int count = manager.getHotKeys().size();
            action.setExecutionStatus("SUCCESS");
            action.setExecutionDetail("已扫描 " + count + " 个热点 Key（自动预热任务由 HotKeyManager 负责）");
        }
    }

    private void executeRedisRecovery(PlannedAction action) {
        boolean ok = faultSelfHealing.triggerRecovery("Redis");
        action.setExecutionStatus(ok ? "SUCCESS" : "FAILED");
        action.setExecutionDetail(ok ? "Redis 自愈已触发/连接正常" : "Redis 自愈未成功，请人工检查");
    }

    private void executeRabbitRecovery(PlannedAction action) {
        boolean ok = faultSelfHealing.triggerRecovery("RabbitMQ");
        action.setExecutionStatus(ok ? "SUCCESS" : "FAILED");
        action.setExecutionDetail(ok ? "RabbitMQ 自愈已触发/连接正常" : "RabbitMQ 自愈未成功");
    }

    /** 启用 MQ 消费限流，在 RabbitMessageQueue 消费路径生效 */
    private void executeThrottle(PlannedAction action) {
        MqConsumerThrottle throttle = consumerThrottleProvider.getIfAvailable();
        if (throttle == null) {
            action.setExecutionStatus("SKIPPED");
            action.setExecutionDetail("MqConsumerThrottle 未就绪（需开启 autonomy + mq + rateLimiter）");
            return;
        }
        MsMiddlewareProperties.MqActuatorProperties mq = properties.getAutonomy().getMq();
        throttle.enable(mq.getThrottleLimit(), mq.getThrottleWindowSeconds());
        action.setExecutionStatus("SUCCESS");
        action.setExecutionDetail(String.format(
                "已启用消费限流 %d 次/%d 秒，消费路径将施加背压",
                throttle.getLimit(), throttle.getWindowSeconds()));
    }

    /** 批量延迟重试失败消息（MEDIUM，通常经 ADVISE/人工采纳后执行） */
    private void executeDelayedRetry(PlannedAction action) {
        MqDelayedRetryExecutor executor = delayedRetryExecutorProvider.getIfAvailable();
        if (executor == null) {
            action.setExecutionStatus("SKIPPED");
            action.setExecutionDetail("MqDelayedRetryExecutor 未就绪（需开启 autonomy + mq）");
            return;
        }
        int count = executor.retryFailedBatch();
        if (count > 0) {
            action.setExecutionStatus("SUCCESS");
            action.setExecutionDetail("已调度 " + count + " 条失败消息延迟重试");
        } else {
            action.setExecutionStatus("SKIPPED");
            action.setExecutionDetail("无带 retryPayload 的失败消息可重试");
        }
    }
}
