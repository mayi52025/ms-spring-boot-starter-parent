package com.ms.middleware.autonomy.decision;

import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.plan.AutonomyPlan;

/**
 * 自治决策引擎：根据上下文生成处置计划。
 * Phase 1 由 {@link com.ms.middleware.autonomy.plan.AutonomyRuleEngine} 实现；
 * Phase 3 将引入 EasyRules 实现；Phase 5 可选 LLM 增强。
 */
public interface AutonomyDecisionEngine {

    AutonomyPlan plan(AutonomyContext context);
}
