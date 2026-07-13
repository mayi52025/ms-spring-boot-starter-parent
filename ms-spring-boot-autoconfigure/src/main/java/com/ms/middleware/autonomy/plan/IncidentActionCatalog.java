package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 各 incident 类型的「动作候选池」——对应 SRE Runbook 条目。
 *
 * <p>只声明「有哪些手段、Runbook 顺序、是否治根因」；具体选优由 {@link ActionSelector} 按词典序完成。
 * Step 5 将把此目录外置到 YAML。</p>
 */
public final class IncidentActionCatalog {

    private IncidentActionCatalog() {
    }

    /**
     * 按 incident 类型返回可参与选优的动作候选（不含配置类推荐）。
     *
     * @param incidentType 如 REDIS_UNAVAILABLE、MQ_DEGRADED
     * @param context      当前快照，用于决定是否加入可选候选（如热点预热）
     * @return 该 incident 下的动作候选列表
     */
    public static List<ActionCandidate> candidatesFor(String incidentType, AutonomyContext context) {
        return switch (incidentType) {
            case "REDIS_UNAVAILABLE" -> redisCandidates(context);
            case "RABBITMQ_UNAVAILABLE" -> rabbitCandidates();
            case "MQ_DEGRADED" -> mqCandidates();
            case "CACHE_DEGRADED" -> cacheCandidates(context);
            default -> List.of();
        };
    }

    /**
     * Redis 宕机 Runbook：
     * 1. 自愈（根因） 2. L1 降级确认（止血） 3. 热点预热（有热点时）
     */
    private static List<ActionCandidate> redisCandidates(AutonomyContext context) {
        List<ActionCandidate> list = new ArrayList<>();
        list.add(ActionCandidate.of(
                AutonomyActionType.TRIGGER_REDIS_RECOVERY, 1, true,
                "Redis 不可用，优先触发自愈恢复 L2"));
        list.add(ActionCandidate.of(
                AutonomyActionType.ENSURE_L1_DEGRADE, 2, false,
                "确认 MultiLevelCache 走 L1 降级，保障读路径可用"));
        if (!context.getHotKeys().isEmpty()) {
            list.add(ActionCandidate.of(
                    AutonomyActionType.WARMUP_HOT_KEYS, 3, false,
                    "存在热点 Key，预热本地缓存降低击穿"));
        }
        return list;
    }

    /** Rabbit 宕机 Runbook：连接自愈为唯一自动候选 */
    private static List<ActionCandidate> rabbitCandidates() {
        return List.of(ActionCandidate.of(
                AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY, 1, true,
                "RabbitMQ 不可用，主动触发连接自愈"));
    }

    /**
     * MQ 消费失败 Runbook：
     * 1. 限流止血（LOW） 2. 延迟重试（MEDIUM，通常仅 ADVISE）
     * 执行器 Step 3 接入。
     */
    private static List<ActionCandidate> mqCandidates() {
        List<ActionCandidate> list = new ArrayList<>();
        list.add(ActionCandidate.of(
                AutonomyActionType.THROTTLE_CONSUMER, 1, false,
                "消费失败累计偏高，限流保护下游并争取消化积压"));
        list.add(ActionCandidate.of(
                AutonomyActionType.DELAYED_RETRY_BATCH, 2, false,
                "批量延迟重试失败消息，适合失败率已稳定场景"));
        return list;
    }

    /** 缓存命中率低：有热点时预热为 Runbook 首选 */
    private static List<ActionCandidate> cacheCandidates(AutonomyContext context) {
        if (context.getHotKeys().isEmpty()) {
            return List.of();
        }
        return List.of(ActionCandidate.of(
                AutonomyActionType.WARMUP_HOT_KEYS, 1, false,
                "命中率低且存在热点，执行预热提升命中"));
    }
}
