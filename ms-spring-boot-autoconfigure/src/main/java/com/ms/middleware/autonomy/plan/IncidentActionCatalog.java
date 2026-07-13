package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.rule.AutonomyRulesDefaults;
import com.ms.middleware.autonomy.rule.AutonomyRulesProperties;
import com.ms.middleware.autonomy.rule.IncidentConditionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 各 incident 类型的「动作候选池」——对应 SRE Runbook 条目。
 *
 * <p>Step 5 起从 {@link AutonomyRulesProperties} YAML 加载；未配置时使用 {@link AutonomyRulesDefaults}。</p>
 * <p>只声明候选与顺序；选优仍由 {@link ActionSelector} 按词典序完成。</p>
 */
public class IncidentActionCatalog {

    private static final Logger logger = LoggerFactory.getLogger(IncidentActionCatalog.class);

    /** 测试与无 Spring 场景下的默认目录 */
    private static final IncidentActionCatalog DEFAULT =
            new IncidentActionCatalog(AutonomyRulesDefaults.create());

    private final Map<String, AutonomyRulesProperties.IncidentRuleDefinition> incidents;

    public IncidentActionCatalog(AutonomyRulesProperties rulesProperties) {
        AutonomyRulesProperties resolved = AutonomyRulesDefaults.resolve(rulesProperties);
        this.incidents = resolved.getIncidents();
    }

    /**
     * 静态便捷方法，使用内置默认规则（单测兼容）。
     */
    public static List<ActionCandidate> defaultCandidatesFor(String incidentType, AutonomyContext context) {
        return DEFAULT.candidatesFor(incidentType, context);
    }

    /**
     * 按 incident 类型返回可参与选优的动作候选（不含配置类推荐）。
     *
     * @param incidentType 如 REDIS_UNAVAILABLE、MQ_DEGRADED
     * @param context      当前快照，用于 when 条件过滤
     */
    public List<ActionCandidate> candidatesFor(String incidentType, AutonomyContext context) {
        AutonomyRulesProperties.IncidentRuleDefinition definition = incidents.get(incidentType);
        if (definition == null || definition.getActions() == null) {
            return List.of();
        }
        List<ActionCandidate> list = new ArrayList<>();
        for (AutonomyRulesProperties.RuleActionDefinition actionDef : definition.getActions()) {
            if (!IncidentConditionEvaluator.matchesActionWhen(actionDef.getWhen(), context)) {
                continue;
            }
            AutonomyActionType actionType = parseActionType(actionDef.getType());
            if (actionType == null) {
                logger.warn("忽略未知动作类型: {} (incident={})", actionDef.getType(), incidentType);
                continue;
            }
            list.add(ActionCandidate.of(
                    actionType,
                    actionDef.getOrder(),
                    actionDef.isAddressesRootCause(),
                    actionDef.getReason()));
        }
        return list;
    }

    private AutonomyActionType parseActionType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        try {
            return AutonomyActionType.valueOf(typeName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
