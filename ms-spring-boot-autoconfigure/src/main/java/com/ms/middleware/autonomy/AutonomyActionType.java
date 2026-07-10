package com.ms.middleware.autonomy;

/**
 * 自治可执行动作类型
 */
public enum AutonomyActionType {
    ENSURE_L1_DEGRADE(AutonomyRisk.LOW, "确认多级缓存走 L1 降级路径"),
    WARMUP_HOT_KEYS(AutonomyRisk.LOW, "预热热点缓存 Key"),
    TRIGGER_REDIS_RECOVERY(AutonomyRisk.LOW, "触发 Redis 连接自愈"),
    TRIGGER_RABBITMQ_RECOVERY(AutonomyRisk.LOW, "触发 RabbitMQ 连接自愈");

    private final AutonomyRisk risk;
    private final String description;

    AutonomyActionType(AutonomyRisk risk, String description) {
        this.risk = risk;
        this.description = description;
    }

    public AutonomyRisk getRisk() {
        return risk;
    }

    public String getDescription() {
        return description;
    }
}
