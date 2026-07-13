package com.ms.middleware.autonomy.rule;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.plan.ActionCandidate;
import com.ms.middleware.autonomy.plan.IncidentActionCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * YAML 规则加载与 Runbook 候选解析。
 */
class IncidentActionCatalogTest {

    /** 默认规则：Redis 宕机应含自愈与 L1 降级 */
    @Test
    void defaultRedisRunbookIncludesRecoveryAndDegrade() {
        IncidentActionCatalog catalog = new IncidentActionCatalog(AutonomyRulesDefaults.create());
        AutonomyContext ctx = new AutonomyContext();
        ctx.setRedisHealthy(false);

        List<ActionCandidate> candidates = catalog.candidatesFor("REDIS_UNAVAILABLE", ctx);

        assertTrue(candidates.stream().anyMatch(c -> c.getActionType() == AutonomyActionType.TRIGGER_REDIS_RECOVERY));
        assertTrue(candidates.stream().anyMatch(c -> c.getActionType() == AutonomyActionType.ENSURE_L1_DEGRADE));
    }

    /** hot-keys-present：无热点时不应出现预热候选 */
    @Test
    void warmupOnlyWhenHotKeysPresent() {
        IncidentActionCatalog catalog = new IncidentActionCatalog(AutonomyRulesDefaults.create());
        AutonomyContext ctx = new AutonomyContext();

        List<ActionCandidate> withoutHot = catalog.candidatesFor("REDIS_UNAVAILABLE", ctx);
        assertFalse(withoutHot.stream().anyMatch(c -> c.getActionType() == AutonomyActionType.WARMUP_HOT_KEYS));

        ctx.getHotKeys().add("order:1");
        List<ActionCandidate> withHot = catalog.candidatesFor("REDIS_UNAVAILABLE", ctx);
        assertTrue(withHot.stream().anyMatch(c -> c.getActionType() == AutonomyActionType.WARMUP_HOT_KEYS));
    }

    /** 用户 YAML 覆盖 MQ Runbook：仅保留延迟重试 */
    @Test
    void yamlOverrideReplacesMqRunbook() {
        AutonomyRulesProperties user = new AutonomyRulesProperties();
        AutonomyRulesProperties.IncidentRuleDefinition mq = new AutonomyRulesProperties.IncidentRuleDefinition();
        AutonomyRulesProperties.RuleActionDefinition retry = new AutonomyRulesProperties.RuleActionDefinition();
        retry.setType("DELAYED_RETRY_BATCH");
        retry.setOrder(1);
        retry.setReason("仅重试");
        retry.setWhen("always");
        mq.getActions().add(retry);
        user.getIncidents().put("MQ_DEGRADED", mq);

        IncidentActionCatalog catalog = new IncidentActionCatalog(user);
        List<ActionCandidate> candidates = catalog.candidatesFor("MQ_DEGRADED", new AutonomyContext());

        assertEquals(1, candidates.size());
        assertEquals(AutonomyActionType.DELAYED_RETRY_BATCH, candidates.get(0).getActionType());
    }

    /** 未知动作类型应跳过，不中断解析 */
    @Test
    void unknownActionTypeIsSkipped() {
        AutonomyRulesProperties user = new AutonomyRulesProperties();
        AutonomyRulesProperties.IncidentRuleDefinition def = new AutonomyRulesProperties.IncidentRuleDefinition();
        AutonomyRulesProperties.RuleActionDefinition bad = new AutonomyRulesProperties.RuleActionDefinition();
        bad.setType("NOT_A_REAL_ACTION");
        bad.setOrder(1);
        bad.setWhen("always");
        def.getActions().add(bad);
        AutonomyRulesProperties.RuleActionDefinition good = new AutonomyRulesProperties.RuleActionDefinition();
        good.setType("THROTTLE_CONSUMER");
        good.setOrder(2);
        good.setWhen("always");
        def.getActions().add(good);
        user.getIncidents().put("MQ_DEGRADED", def);

        IncidentActionCatalog catalog = new IncidentActionCatalog(user);
        List<ActionCandidate> candidates = catalog.candidatesFor("MQ_DEGRADED", new AutonomyContext());

        assertEquals(1, candidates.size());
        assertEquals(AutonomyActionType.THROTTLE_CONSUMER, candidates.get(0).getActionType());
    }
}
