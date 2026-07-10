package com.ms.middleware.autonomy.act;

import com.ms.middleware.ai.HotKeyManager;
import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.health.FaultSelfHealing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 自治动作执行器：把 {@link com.ms.middleware.autonomy.AutonomyActionType} 映射到现有中间件能力。
 *
 * <p>不重复实现缓存/MQ 逻辑，而是调用 {@link FaultSelfHealing}、{@link HotKeyManager} 等已有组件，
 * 保证自治层是薄编排层。</p>
 */
public class AutonomyActuator {

    private static final Logger logger = LoggerFactory.getLogger(AutonomyActuator.class);

    private final FaultSelfHealing faultSelfHealing;
    private final ObjectProvider<HotKeyManager> hotKeyManagerProvider;

    public AutonomyActuator(FaultSelfHealing faultSelfHealing,
                            ObjectProvider<HotKeyManager> hotKeyManagerProvider) {
        this.faultSelfHealing = faultSelfHealing;
        this.hotKeyManagerProvider = hotKeyManagerProvider;
    }

    /**
     * 执行单个计划动作，结果写回 action.executionStatus / executionDetail。
     */
    public void execute(PlannedAction action) {
        AutonomyActionType type = action.getActionType();
        try {
            switch (type) {
                // L1 降级已在 MultiLevelCache 请求路径内完成，此处仅确认策略生效
                case ENSURE_L1_DEGRADE -> {
                    action.setExecutionStatus("SUCCESS");
                    action.setExecutionDetail("已依赖 MultiLevelCache 请求路径自动降级至 L1，无需额外调用");
                }
                case WARMUP_HOT_KEYS -> {
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
                case TRIGGER_REDIS_RECOVERY -> {
                    boolean ok = faultSelfHealing.triggerRecovery("Redis");
                    action.setExecutionStatus(ok ? "SUCCESS" : "FAILED");
                    action.setExecutionDetail(ok ? "Redis 自愈已触发/连接正常" : "Redis 自愈未成功，请人工检查");
                }
                case TRIGGER_RABBITMQ_RECOVERY -> {
                    boolean ok = faultSelfHealing.triggerRecovery("RabbitMQ");
                    action.setExecutionStatus(ok ? "SUCCESS" : "FAILED");
                    action.setExecutionDetail(ok ? "RabbitMQ 自愈已触发/连接正常" : "RabbitMQ 自愈未成功");
                }
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
}
