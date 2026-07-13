package com.ms.middleware.autonomy.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.ai.HotKeyManager;
import com.ms.middleware.health.FaultSelfHealing;
import com.ms.middleware.metrics.MsMetrics;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 自治「侦察兵」：从现有中间件组件聚合一次巡检快照 {@link AutonomyContext}。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>读取 Redis/Rabbit 健康、命中率、MQ 失败数、热点 Key</li>
 *   <li>把配置阈值写入上下文，保证检测、决策、结案三处标准一致</li>
 *   <li>生成 issues 列表供控制台与 {@link com.ms.middleware.autonomy.plan.AutonomyRuleEngine} 使用</li>
 * </ul>
 */
public class AutonomyContextBuilder {

    private final MsMiddlewareProperties properties;
    private final FaultSelfHealing faultSelfHealing;
    private final MsMetrics metrics;
    private final ObjectProvider<HotKeyManager> hotKeyManagerProvider;

    public AutonomyContextBuilder(MsMiddlewareProperties properties,
                                  FaultSelfHealing faultSelfHealing,
                                  MsMetrics metrics,
                                  ObjectProvider<HotKeyManager> hotKeyManagerProvider) {
        this.properties = properties;
        this.faultSelfHealing = faultSelfHealing;
        this.metrics = metrics;
        this.hotKeyManagerProvider = hotKeyManagerProvider;
    }

    /**
     * 构建当前时刻的中间件上下文。
     * 每次 {@link com.ms.middleware.autonomy.AutonomyScheduler} tick 调用一次，不做缓存。
     */
    public AutonomyContext build() {
        AutonomyContext ctx = new AutonomyContext();
        MsMiddlewareProperties.AutonomyProperties autonomy = properties.getAutonomy();

        // 1. 采集原始信号
        boolean redisHealthy = faultSelfHealing.getComponentHealth("Redis");
        boolean rabbitHealthy = faultSelfHealing.getComponentHealth("RabbitMQ");
        ctx.setRedisHealthy(redisHealthy);
        ctx.setRabbitMqHealthy(rabbitHealthy);
        ctx.setCacheHitRate(metrics.getCacheHitRate());
        ctx.setMqFailedCount(metrics.getMessageFailedCount());
        ctx.setGlobalFailureCount(metrics.getFailureCount());

        // 2. 把配置阈值带入上下文，后续 isMqDegraded/isCacheDegraded 与配置同源
        ctx.setMqFailedWarnThreshold(autonomy.getMqFailedWarnThreshold());
        ctx.setCacheHitRateWarnThreshold(autonomy.getCacheHitRateWarnThreshold());

        hotKeyManagerProvider.ifAvailable(manager ->
                ctx.setHotKeys(new ArrayList<>(manager.getHotKeys())));

        // 3. 生成 issues（与 isMqDegraded/isCacheDegraded 判定逻辑保持一致）
        List<String> issues = new ArrayList<>();
        if (!redisHealthy) {
            issues.add("Redis 不可用，分布式缓存 L2 可能失效");
        }
        if (!rabbitHealthy) {
            issues.add("RabbitMQ 不可用，消息收发可能受影响");
        }
        if (ctx.isCacheDegraded()) {
            issues.add(String.format("缓存命中率偏低: %.1f%%", ctx.getCacheHitRate() * 100));
        }
        if (ctx.isMqDegraded()) {
            issues.add(String.format("MQ 消费失败（窗口内）偏高: %d", ctx.getMqFailedCount()));
        }
        if (!ctx.getHotKeys().isEmpty() && !redisHealthy) {
            issues.add("存在热点 Key 且 Redis 不可用，存在缓存击穿风险");
        }

        ctx.setIssues(issues);
        return ctx;
    }

    /**
     * 判断某次 run 的「主 incident」是否已恢复，用于 STABLE 结案。
     *
     * <p>只关心主故障类型（如 Redis 挂了就不看次要命中率预警），避免次要指标阻止 run 结束。
     * 判定条件与 {@link AutonomyContext#isMqDegraded()} 等保持一致。</p>
     */
    public boolean isIncidentResolved(String incidentType, AutonomyContext context) {
        if (incidentType == null || "NONE".equals(incidentType)) {
            return !context.hasIncident();
        }
        return switch (incidentType) {
            case "REDIS_UNAVAILABLE" -> context.isRedisHealthy();
            case "RABBITMQ_UNAVAILABLE" -> context.isRabbitMqHealthy();
            // MQ 主故障：失败计数降到阈值以下即视为恢复
            case "MQ_DEGRADED" -> !context.isMqDegraded();
            case "CACHE_DEGRADED" -> !context.isCacheDegraded();
            default -> !context.hasIncident();
        };
    }
}
