package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;
import com.ms.middleware.autonomy.rule.AutonomyRulesDefaults;
import com.ms.middleware.autonomy.rule.AutonomyRulesProperties;
import com.ms.middleware.autonomy.rule.IncidentConditionEvaluator;
import com.ms.middleware.autonomy.rule.IncidentSummaryFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于规则的默认决策引擎（实现 {@link AutonomyDecisionEngine}）。
 *
 * <p>流程：YAML 识别 incident → {@link IncidentActionCatalog} Runbook 候选
 * → {@link ActionSelector} 规则选优 → 写入 {@link AutonomyPlan}。</p>
 *
 * <p>Step 5 起 incident 识别链、Runbook、配置推荐均来自 {@link AutonomyRulesProperties}。</p>
 */
public class AutonomyRuleEngine implements AutonomyDecisionEngine {

    private final ActionSelector actionSelector;
    private final IncidentActionCatalog actionCatalog;
    private final AutonomyRulesProperties rules;

    /** 无参构造供测试；使用内置默认规则 */
    public AutonomyRuleEngine() {
        this(new ActionSelector(),
                new IncidentActionCatalog(AutonomyRulesDefaults.create()),
                AutonomyRulesDefaults.create());
    }

    public AutonomyRuleEngine(ActionSelector actionSelector) {
        this(actionSelector,
                new IncidentActionCatalog(AutonomyRulesDefaults.create()),
                AutonomyRulesDefaults.create());
    }

    public AutonomyRuleEngine(ActionSelector actionSelector,
                              IncidentActionCatalog actionCatalog,
                              AutonomyRulesProperties rulesProperties) {
        this.actionSelector = actionSelector;
        this.actionCatalog = actionCatalog;
        this.rules = AutonomyRulesDefaults.resolve(rulesProperties);
    }

    @Override
    public AutonomyPlan plan(AutonomyContext context) {
        AutonomyPlan plan = new AutonomyPlan();
        plan.setContext(context);

        if (!context.hasIncident()) {
            plan.setIncidentType("NONE");
            plan.setSummary("中间件状态正常");
            return plan;
        }

        String incidentType = resolveIncidentType(context);
        plan.setIncidentType(incidentType);
        plan.setSummary(buildSummary(incidentType, context));

        List<AutonomyRecommendation> recommendations = buildRecommendations(incidentType);
        List<ActionCandidate> candidates = actionCatalog.candidatesFor(incidentType, context);
        List<PlannedAction> selectedActions = actionSelector.select(candidates, context);

        plan.setActions(selectedActions);
        plan.setRankingSummary(actionSelector.buildSelectionSummary(selectedActions));
        plan.setRulesVersion(rules.getVersion());
        plan.setRecommendations(recommendations);
        return plan;
    }

    /** 按 YAML 有序条件链确定主 incident 类型 */
    String resolveIncidentType(AutonomyContext context) {
        for (AutonomyRulesProperties.IncidentDetectionRule rule : rules.getIncidentDetection()) {
            if (IncidentConditionEvaluator.matchesIncidentCondition(rule.getCondition(), context)) {
                return rule.getIncidentType();
            }
        }
        return "UNKNOWN";
    }

    private String buildSummary(String incidentType, AutonomyContext context) {
        AutonomyRulesProperties.IncidentRuleDefinition definition = rules.getIncidents().get(incidentType);
        if (definition != null && definition.getSummary() != null && !definition.getSummary().isBlank()) {
            return IncidentSummaryFormatter.format(definition.getSummary(), context);
        }
        return "存在预警但未匹配已知 incident 类型";
    }

    /** 配置级推荐（不参与动作选优，由编排器写入 RECOMMEND 时间线） */
    private List<AutonomyRecommendation> buildRecommendations(String incidentType) {
        AutonomyRulesProperties.IncidentRuleDefinition definition = rules.getIncidents().get(incidentType);
        if (definition == null || definition.getRecommendations() == null) {
            return List.of();
        }
        List<AutonomyRecommendation> recommendations = new ArrayList<>();
        for (AutonomyRulesProperties.RuleRecommendationDefinition recDef : definition.getRecommendations()) {
            recommendations.add(new AutonomyRecommendation(
                    recDef.getTitle(),
                    recDef.getDescription(),
                    recDef.getSuggestedConfig()));
        }
        return recommendations;
    }
}
