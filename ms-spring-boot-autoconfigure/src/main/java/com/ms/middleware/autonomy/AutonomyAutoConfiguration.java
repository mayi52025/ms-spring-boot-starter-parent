package com.ms.middleware.autonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.MsMiddlewareAutoConfiguration;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.ai.HotKeyManager;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.context.AutonomyContextBuilder;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;
import com.ms.middleware.autonomy.insight.DefaultMiddlewareInsightService;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.plan.AutonomyRuleEngine;
import com.ms.middleware.autonomy.policy.AutonomyPolicy;
import com.ms.middleware.autonomy.run.AutonomyLedger;
import com.ms.middleware.autonomy.run.InMemoryAutonomyLedger;
import com.ms.middleware.autonomy.run.RedissonAutonomyLedger;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.autonomy.tenant.SpringEnvironmentTenantProvider;
import com.ms.middleware.health.FaultSelfHealing;
import com.ms.middleware.metrics.MsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import com.ms.middleware.redis.RedissonConnectionManager;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 自治模块 Spring Boot 自动配置。
 *
 * <p>开关：{@code ms.middleware.autonomy.enabled=true}</p>
 * <p>账本：{@code ledger.type=memory}（默认）或 {@code redisson}（需 RedissonClient）</p>
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MsMiddlewareProperties.class)
@AutoConfigureAfter(MsMiddlewareAutoConfiguration.class)
@ConditionalOnProperty(prefix = "ms.middleware.autonomy", name = "enabled", havingValue = "true")
public class AutonomyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AutonomyTenantProvider.class)
    public AutonomyTenantProvider autonomyTenantProvider(Environment environment) {
        return new SpringEnvironmentTenantProvider(environment);
    }

    @Bean
    public AutonomyContextBuilder autonomyContextBuilder(MsMiddlewareProperties properties,
                                                         FaultSelfHealing faultSelfHealing,
                                                         MsMetrics metrics,
                                                         ObjectProvider<HotKeyManager> hotKeyManagerProvider) {
        return new AutonomyContextBuilder(properties, faultSelfHealing, metrics, hotKeyManagerProvider);
    }

    @Bean
    @ConditionalOnMissingBean(AutonomyDecisionEngine.class)
    public AutonomyDecisionEngine autonomyDecisionEngine() {
        return new AutonomyRuleEngine();
    }

    @Bean
    public AutonomyPolicy autonomyPolicy(MsMiddlewareProperties properties) {
        return new AutonomyPolicy(properties);
    }

    @Bean
    public AutonomyActuator autonomyActuator(FaultSelfHealing faultSelfHealing,
                                             ObjectProvider<HotKeyManager> hotKeyManagerProvider) {
        return new AutonomyActuator(faultSelfHealing, hotKeyManagerProvider);
    }

    @Bean
    @ConditionalOnMissingBean(AutonomyMetrics.class)
    public AutonomyMetrics autonomyMetrics(MeterRegistry meterRegistry) {
        return new AutonomyMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(AutonomyLedger.class)
    @ConditionalOnProperty(prefix = "ms.middleware.autonomy.ledger", name = "type", havingValue = "memory", matchIfMissing = true)
    public AutonomyLedger inMemoryAutonomyLedger(ApplicationEventPublisher eventPublisher,
                                                  AutonomyTenantProvider tenantProvider,
                                                  MsMiddlewareProperties properties) {
        int maxRuns = properties.getAutonomy().getLedger().getMaxRuns();
        return new InMemoryAutonomyLedger(eventPublisher, tenantProvider, maxRuns);
    }

    @Bean
    @ConditionalOnMissingBean(AutonomyLedger.class)
    @ConditionalOnProperty(prefix = "ms.middleware.autonomy.ledger", name = "type", havingValue = "redisson")
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonConnectionManager.class)
    public AutonomyLedger redissonAutonomyLedger(RedissonConnectionManager connectionManager,
                                                  ObjectMapper objectMapper,
                                                  ApplicationEventPublisher eventPublisher,
                                                  AutonomyTenantProvider tenantProvider,
                                                  MsMiddlewareProperties properties) {
        MsMiddlewareProperties.LedgerProperties ledger = properties.getAutonomy().getLedger();
        return new RedissonAutonomyLedger(
                connectionManager,
                objectMapper,
                eventPublisher,
                tenantProvider,
                ledger.getKeyPrefix(),
                ledger.getMaxRuns(),
                ledger.getTtlHours());
    }

    @Bean
    @ConditionalOnMissingBean(MiddlewareInsightService.class)
    public MiddlewareInsightService middlewareInsightService(AutonomyLedger ledger,
                                                             MsMetrics metrics,
                                                             AutonomyMetrics autonomyMetrics) {
        return new DefaultMiddlewareInsightService(ledger, metrics, autonomyMetrics);
    }

    @Bean
    public AutonomyOrchestrator autonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                                     AutonomyDecisionEngine decisionEngine,
                                                     AutonomyPolicy policy,
                                                     AutonomyActuator actuator,
                                                     AutonomyLedger ledger,
                                                     AutonomyTenantProvider tenantProvider,
                                                     AutonomyMetrics autonomyMetrics) {
        return new AutonomyOrchestrator(contextBuilder, decisionEngine, policy, actuator, ledger,
                tenantProvider, autonomyMetrics);
    }

    @Bean
    public AutonomyScheduler autonomyScheduler(AutonomyOrchestrator orchestrator) {
        return new AutonomyScheduler(orchestrator);
    }
}
