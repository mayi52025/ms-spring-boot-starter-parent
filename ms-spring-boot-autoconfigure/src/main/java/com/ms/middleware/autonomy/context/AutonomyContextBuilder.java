package com.ms.middleware.autonomy.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.ai.HotKeyManager;
import com.ms.middleware.health.FaultSelfHealing;
import com.ms.middleware.metrics.MsMetrics;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

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

    public AutonomyContext build() {
        AutonomyContext ctx = new AutonomyContext();
        MsMiddlewareProperties.AutonomyProperties autonomy = properties.getAutonomy();

        boolean redisHealthy = faultSelfHealing.getComponentHealth("Redis");
        boolean rabbitHealthy = faultSelfHealing.getComponentHealth("RabbitMQ");
        ctx.setRedisHealthy(redisHealthy);
        ctx.setRabbitMqHealthy(rabbitHealthy);
        ctx.setCacheHitRate(metrics.getCacheHitRate());
        ctx.setMqFailedCount(metrics.getMessageFailedCount());
        ctx.setGlobalFailureCount(metrics.getFailureCount());

        hotKeyManagerProvider.ifAvailable(manager ->
                ctx.setHotKeys(new ArrayList<>(manager.getHotKeys())));

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
