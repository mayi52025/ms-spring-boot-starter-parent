package com.ms.middleware.autonomy.rule;

import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.AutonomyRuleEngine;
import com.ms.middleware.autonomy.plan.ActionSelector;
import com.ms.middleware.autonomy.plan.IncidentActionCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * YAML incident 识别链与摘要模板。
 */
class AutonomyRulesYamlTest {

    private final ActionSelector actionSelector = new ActionSelector();

    /** 自定义识别链：仅识别 MQ 降级 */
    @Test
    void customDetectionChainOnlyMq() {
        AutonomyRulesProperties rules = new AutonomyRulesProperties();
        AutonomyRulesProperties.IncidentDetectionRule mqOnly = new AutonomyRulesProperties.IncidentDetectionRule();
        mqOnly.setCondition("mq-degraded");
        mqOnly.setIncidentType("MQ_DEGRADED");
        rules.setIncidentDetection(List.of(mqOnly));

        AutonomyRuleEngine engine = new AutonomyRuleEngine(
                actionSelector, new IncidentActionCatalog(rules), rules);

        AutonomyContext ctx = incidentContext();
        ctx.setRedisHealthy(false);
        ctx.setMqFailedCount(15);
        ctx.setMqFailedWarnThreshold(10);

        AutonomyPlan plan = engine.plan(ctx);

        // 自定义链不再优先 Redis，MQ 达阈值即 MQ_DEGRADED
        assertEquals("MQ_DEGRADED", plan.getIncidentType());
    }

    /** 摘要模板占位符应被替换 */
    @Test
    void summaryTemplateSubstitutesPlaceholders() {
        AutonomyRulesProperties rules = AutonomyRulesDefaults.create();
        AutonomyRulesProperties.IncidentRuleDefinition mq = rules.getIncidents().get("MQ_DEGRADED");
        mq.setSummary("失败 {mqFailedCount} / 阈值 {mqFailedWarnThreshold}");

        AutonomyRuleEngine engine = new AutonomyRuleEngine(
                actionSelector, new IncidentActionCatalog(rules), rules);

        AutonomyContext ctx = incidentContext();
        ctx.setMqFailedCount(12);
        ctx.setMqFailedWarnThreshold(10);
        ctx.getIssues().add("MQ 消费失败累计偏高: 12");

        AutonomyPlan plan = engine.plan(ctx);

        assertEquals("失败 12 / 阈值 10", plan.getSummary());
    }

    private AutonomyContext incidentContext() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setRedisHealthy(true);
        ctx.setRabbitMqHealthy(true);
        ctx.setMqFailedWarnThreshold(10);
        ctx.setCacheHitRateWarnThreshold(0.5);
        ctx.setIssues(new ArrayList<>(List.of("incident")));
        return ctx;
    }
}
