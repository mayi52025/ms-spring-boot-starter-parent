package com.ms.middleware.autonomy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.ai.HotKeyManager;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.context.AutonomyContextBuilder;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;
import com.ms.middleware.autonomy.insight.DefaultMiddlewareInsightService;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.plan.AutonomyRuleEngine;
import com.ms.middleware.autonomy.policy.AutonomyPolicy;
import com.ms.middleware.autonomy.run.AutonomyLedger;
import com.ms.middleware.autonomy.run.InMemoryAutonomyLedger;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.autonomy.tenant.SpringEnvironmentTenantProvider;
import com.ms.middleware.health.FaultSelfHealing;
import com.ms.middleware.metrics.MsMetrics;
import org.springframework.beans.factory.ObjectProvider;
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
 * <p>可替换 Bean：{@link com.ms.middleware.autonomy.decision.AutonomyDecisionEngine}、
 * {@link AutonomyLedger}（ledger.type=redisson 时 Phase 2 注册 Redisson 实现）</p>
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MsMiddlewareProperties.class)
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
    @ConditionalOnMissingBean(AutonomyLedger.class)
    @ConditionalOnProperty(prefix = "ms.middleware.autonomy.ledger", name = "type", havingValue = "memory", matchIfMissing = true)
    public AutonomyLedger inMemoryAutonomyLedger(ApplicationEventPublisher eventPublisher,
                                                  AutonomyTenantProvider tenantProvider,
                                                  MsMiddlewareProperties properties) {
        int maxRuns = properties.getAutonomy().getLedger().getMaxRuns();
        return new InMemoryAutonomyLedger(eventPublisher, tenantProvider, maxRuns);
    }

    @Bean
    @ConditionalOnMissingBean(MiddlewareInsightService.class)
    public MiddlewareInsightService middlewareInsightService(AutonomyLedger ledger, MsMetrics metrics) {
        return new DefaultMiddlewareInsightService(ledger, metrics);
    }

    @Bean
    public AutonomyOrchestrator autonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                                     AutonomyDecisionEngine decisionEngine,
                                                     AutonomyPolicy policy,
                                                     AutonomyActuator actuator,
                                                     AutonomyLedger ledger,
                                                     AutonomyTenantProvider tenantProvider) {
        return new AutonomyOrchestrator(contextBuilder, decisionEngine, policy, actuator, ledger, tenantProvider);
    }

    @Bean
    public AutonomyScheduler autonomyScheduler(AutonomyOrchestrator orchestrator) {
        return new AutonomyScheduler(orchestrator);
    }
}
