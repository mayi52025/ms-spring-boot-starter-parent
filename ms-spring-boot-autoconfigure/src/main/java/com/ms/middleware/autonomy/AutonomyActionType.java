package com.ms.middleware.autonomy;

/**
 * 自治可执行动作类型。
 *
 * <p>THROTTLE_CONSUMER / DELAYED_RETRY_BATCH 在 Step 3 接入执行器；
 * Step 2 起已参与候选排序选优。</p>
 */
public enum AutonomyActionType {
    ENSURE_L1_DEGRADE(AutonomyRisk.LOW, "确认多级缓存走 L1 降级路径"),
    WARMUP_HOT_KEYS(AutonomyRisk.LOW, "预热热点缓存 Key"),
    TRIGGER_REDIS_RECOVERY(AutonomyRisk.LOW, "触发 Redis 连接自愈"),
    TRIGGER_RABBITMQ_RECOVERY(AutonomyRisk.LOW, "触发 RabbitMQ 连接自愈"),
    /** MQ 消费失败场景首选：限流保护下游（执行器 Step 3 接入） */
    THROTTLE_CONSUMER(AutonomyRisk.LOW, "消费端限流以保护下游"),
    /** 批量延迟重试，风险较高，通常仅 ADVISE 或人工采纳后执行 */
    DELAYED_RETRY_BATCH(AutonomyRisk.MEDIUM, "批量延迟重试失败消息");

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
