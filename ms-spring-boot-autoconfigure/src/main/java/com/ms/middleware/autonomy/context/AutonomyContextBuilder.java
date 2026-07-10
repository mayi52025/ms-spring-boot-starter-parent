package com.ms.middleware.autonomy.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.ai.HotKeyManager;
import com.ms.middleware.health.FaultSelfHealing;
import com.ms.middleware.metrics.MsMetrics;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 从现有中间件组件聚合一次「巡检快照」。
 *
 * <p>数据来源：</p>
 * <ul>
 *   <li>{@link com.ms.middleware.health.FaultSelfHealing} — Redis / RabbitMQ 健康</li>
 *   <li>{@link com.ms.middleware.metrics.MsMetrics} — 命中率、MQ 失败计数</li>
 *   <li>{@link com.ms.middleware.ai.HotKeyManager} — 热点 Key（可选）</li>
 * </ul>
 * <p>issues 非空即视为有故障（{@link AutonomyContext#hasIncident()}）。</p>
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
     * 构建当前时刻的中间件上下文；每次 tick 调用一次，不缓存。
     */
    public AutonomyContext build() {
        AutonomyContext ctx = new AutonomyContext();
        MsMiddlewareProperties.AutonomyProperties autonomy = properties.getAutonomy();

        // 组件级健康来自 FaultSelfHealing 的探活结果
        boolean redisHealthy = faultSelfHealing.getComponentHealth("Redis");
        boolean rabbitHealthy = faultSelfHealing.getComponentHealth("RabbitMQ");
        ctx.setRedisHealthy(redisHealthy);
        ctx.setRabbitMqHealthy(rabbitHealthy);
        ctx.setCacheHitRate(metrics.getCacheHitRate());
        ctx.setMqFailedCount(metrics.getMessageFailedCount());
        ctx.setGlobalFailureCount(metrics.getFailureCount());

        hotKeyManagerProvider.ifAvailable(manager ->
                ctx.setHotKeys(new ArrayList<>(manager.getHotKeys())));

        // 以下规则与 ms.middleware.autonomy 阈值配置联动，写入可读 issues 供控制台展示
        List<String> issues = new ArrayList<>();
        if (!redisHealthy) {
            issues.add("Redis 不可用，分布式缓存 L2 可能失效");
        }
        if (!rabbitHealthy) {
            issues.add("RabbitMQ 不可用，消息收发可能受影响");
        }
        if (ctx.getCacheHitRate() < autonomy.getCacheHitRateWarnThreshold()) {
            issues.add(String.format("缓存命中率偏低: %.1f%%", ctx.getCacheHitRate() * 100));
        }
        if (ctx.getMqFailedCount() >= autonomy.getMqFailedWarnThreshold()) {
            issues.add(String.format("MQ 消费失败累计偏高: %d", ctx.getMqFailedCount()));
        }
        if (!ctx.getHotKeys().isEmpty() && !redisHealthy) {
            issues.add("存在热点 Key 且 Redis 不可用，存在缓存击穿风险");
        }

        ctx.setIssues(issues);
        return ctx;
    }
}
