package com.ms.middleware.autonomy.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置默认自治规则，与 Step 2 硬编码行为等价。
 *
 * <p>当 {@link AutonomyRulesProperties} 未配置或 incidents 为空时作为兜底；
 * 用户 YAML 可按 incident 类型覆盖单条定义。</p>
 */
public final class AutonomyRulesDefaults {

    private static final AutonomyRulesProperties DEFAULTS = buildDefaults();

    private AutonomyRulesDefaults() {
    }

    /** 返回不可变语义上的默认规则副本（每次调用新实例，避免被调用方篡改全局默认） */
    public static AutonomyRulesProperties create() {
        AutonomyRulesProperties copy = new AutonomyRulesProperties();
        copy.setIncidentDetection(cloneDetection(DEFAULTS.getIncidentDetection()));
        copy.setIncidents(cloneIncidents(DEFAULTS.getIncidents()));
        copy.setVersion(DEFAULTS.getVersion());
        return copy;
    }

    /**
     * 合并用户配置与默认规则：识别链非空则全量替换；incidents 按 key 覆盖合并。
     */
    public static AutonomyRulesProperties resolve(AutonomyRulesProperties user) {
        AutonomyRulesProperties resolved = create();
        if (user == null) {
            return resolved;
        }
        if (user.getIncidentDetection() != null && !user.getIncidentDetection().isEmpty()) {
            resolved.setIncidentDetection(cloneDetection(user.getIncidentDetection()));
        }
        if (user.getIncidents() != null && !user.getIncidents().isEmpty()) {
            Map<String, AutonomyRulesProperties.IncidentRuleDefinition> merged =
                    new LinkedHashMap<>(resolved.getIncidents());
            for (Map.Entry<String, AutonomyRulesProperties.IncidentRuleDefinition> entry
                    : user.getIncidents().entrySet()) {
                merged.put(entry.getKey(), cloneIncident(entry.getValue()));
            }
            resolved.setIncidents(merged);
        }
        if (user.getVersion() != null && !user.getVersion().isBlank()) {
            resolved.setVersion(user.getVersion());
        }
        return resolved;
    }

    private static AutonomyRulesProperties buildDefaults() {
        AutonomyRulesProperties rules = new AutonomyRulesProperties();
        rules.setIncidentDetection(defaultDetection());
        rules.setIncidents(defaultIncidents());
        return rules;
    }

    private static List<AutonomyRulesProperties.IncidentDetectionRule> defaultDetection() {
        List<AutonomyRulesProperties.IncidentDetectionRule> list = new ArrayList<>();
        list.add(detection("redis-unhealthy", "REDIS_UNAVAILABLE"));
        list.add(detection("rabbitmq-unhealthy", "RABBITMQ_UNAVAILABLE"));
        list.add(detection("mq-degraded", "MQ_DEGRADED"));
        list.add(detection("cache-degraded", "CACHE_DEGRADED"));
        return list;
    }

    private static AutonomyRulesProperties.IncidentDetectionRule detection(String condition, String type) {
        AutonomyRulesProperties.IncidentDetectionRule rule = new AutonomyRulesProperties.IncidentDetectionRule();
        rule.setCondition(condition);
        rule.setIncidentType(type);
        return rule;
    }

    private static Map<String, AutonomyRulesProperties.IncidentRuleDefinition> defaultIncidents() {
        Map<String, AutonomyRulesProperties.IncidentRuleDefinition> map = new LinkedHashMap<>();

        AutonomyRulesProperties.IncidentRuleDefinition redis = new AutonomyRulesProperties.IncidentRuleDefinition();
        redis.setSummary("Redis 不可用，启用本地缓存与自愈组合处置");
        redis.getActions().add(action("TRIGGER_REDIS_RECOVERY", 1, true,
                "Redis 不可用，优先触发自愈恢复 L2", "always"));
        redis.getActions().add(action("ENSURE_L1_DEGRADE", 2, false,
                "确认 MultiLevelCache 走 L1 降级，保障读路径可用", "always"));
        redis.getActions().add(action("WARMUP_HOT_KEYS", 3, false,
                "存在热点 Key，预热本地缓存降低击穿", "hot-keys-present"));
        redis.getRecommendations().add(recommendation(
                "缩短本地缓存 TTL 差",
                "可将 distributed.ttl 与 local.ttl 比例调至 1:8，减少 Redis 恢复后陈旧数据窗口",
                "ms.middleware.cache.distributed.ttl / local.ttl"));
        redis.getRecommendations().add(recommendation(
                "开启热点自动预热",
                "确保 ai.hotKey.autoWarmup=true，降低击穿风险",
                "ms.middleware.ai.hotKey.auto-warmup=true"));
        map.put("REDIS_UNAVAILABLE", redis);

        AutonomyRulesProperties.IncidentRuleDefinition rabbit = new AutonomyRulesProperties.IncidentRuleDefinition();
        rabbit.setSummary("RabbitMQ 不可用，触发自愈并关注消息堆积");
        rabbit.getActions().add(action("TRIGGER_RABBITMQ_RECOVERY", 1, true,
                "RabbitMQ 不可用，主动触发连接自愈", "always"));
        rabbit.getRecommendations().add(recommendation(
                "检查 MQ 幂等窗口",
                "故障期间可能重复投递，可适当延长幂等键过期时间（需人工确认）",
                "ms.middleware.mq.idempotent.expiration-hours"));
        map.put("RABBITMQ_UNAVAILABLE", rabbit);

        AutonomyRulesProperties.IncidentRuleDefinition mq = new AutonomyRulesProperties.IncidentRuleDefinition();
        mq.setSummary("MQ 消费失败累计 {mqFailedCount}（阈值 {mqFailedWarnThreshold}），建议排查消费端与幂等");
        mq.getActions().add(action("THROTTLE_CONSUMER", 1, false,
                "消费失败累计偏高，限流保护下游并争取消化积压", "always"));
        mq.getActions().add(action("DELAYED_RETRY_BATCH", 2, false,
                "批量延迟重试失败消息，适合失败率已稳定场景", "always"));
        mq.getRecommendations().add(recommendation(
                "查看失败消息 Trace",
                "在控制台聊天中提供 messageId 可进一步定位",
                null));
        map.put("MQ_DEGRADED", mq);

        AutonomyRulesProperties.IncidentRuleDefinition cache = new AutonomyRulesProperties.IncidentRuleDefinition();
        cache.setSummary("缓存命中率 {cacheHitRatePercent}% 低于阈值 {cacheHitRateWarnThresholdPercent}%，建议预热热点");
        cache.getActions().add(action("WARMUP_HOT_KEYS", 1, false,
                "命中率低且存在热点，执行预热提升命中", "hot-keys-present"));
        map.put("CACHE_DEGRADED", cache);

        return map;
    }

    private static AutonomyRulesProperties.RuleActionDefinition action(
            String type, int order, boolean rootCause, String reason, String when) {
        AutonomyRulesProperties.RuleActionDefinition def = new AutonomyRulesProperties.RuleActionDefinition();
        def.setType(type);
        def.setOrder(order);
        def.setAddressesRootCause(rootCause);
        def.setReason(reason);
        def.setWhen(when);
        return def;
    }

    private static AutonomyRulesProperties.RuleRecommendationDefinition recommendation(
            String title, String description, String suggestedConfig) {
        AutonomyRulesProperties.RuleRecommendationDefinition rec =
                new AutonomyRulesProperties.RuleRecommendationDefinition();
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setSuggestedConfig(suggestedConfig);
        return rec;
    }

    private static List<AutonomyRulesProperties.IncidentDetectionRule> cloneDetection(
            List<AutonomyRulesProperties.IncidentDetectionRule> source) {
        List<AutonomyRulesProperties.IncidentDetectionRule> list = new ArrayList<>();
        for (AutonomyRulesProperties.IncidentDetectionRule item : source) {
            AutonomyRulesProperties.IncidentDetectionRule copy = new AutonomyRulesProperties.IncidentDetectionRule();
            copy.setCondition(item.getCondition());
            copy.setIncidentType(item.getIncidentType());
            list.add(copy);
        }
        return list;
    }

    private static Map<String, AutonomyRulesProperties.IncidentRuleDefinition> cloneIncidents(
            Map<String, AutonomyRulesProperties.IncidentRuleDefinition> source) {
        Map<String, AutonomyRulesProperties.IncidentRuleDefinition> map = new LinkedHashMap<>();
        for (Map.Entry<String, AutonomyRulesProperties.IncidentRuleDefinition> entry : source.entrySet()) {
            map.put(entry.getKey(), cloneIncident(entry.getValue()));
        }
        return map;
    }

    private static AutonomyRulesProperties.IncidentRuleDefinition cloneIncident(
            AutonomyRulesProperties.IncidentRuleDefinition source) {
        AutonomyRulesProperties.IncidentRuleDefinition copy = new AutonomyRulesProperties.IncidentRuleDefinition();
        copy.setSummary(source.getSummary());
        for (AutonomyRulesProperties.RuleActionDefinition action : source.getActions()) {
            AutonomyRulesProperties.RuleActionDefinition actionCopy = new AutonomyRulesProperties.RuleActionDefinition();
            actionCopy.setType(action.getType());
            actionCopy.setOrder(action.getOrder());
            actionCopy.setAddressesRootCause(action.isAddressesRootCause());
            actionCopy.setReason(action.getReason());
            actionCopy.setWhen(action.getWhen());
            copy.getActions().add(actionCopy);
        }
        for (AutonomyRulesProperties.RuleRecommendationDefinition rec : source.getRecommendations()) {
            AutonomyRulesProperties.RuleRecommendationDefinition recCopy =
                    new AutonomyRulesProperties.RuleRecommendationDefinition();
            recCopy.setTitle(rec.getTitle());
            recCopy.setDescription(rec.getDescription());
            recCopy.setSuggestedConfig(rec.getSuggestedConfig());
            copy.getRecommendations().add(recCopy);
        }
        return copy;
    }
}
