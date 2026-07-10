package com.ms.middleware.autonomy.policy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;
import com.ms.middleware.autonomy.plan.PlannedAction;

/**
 * 策略门控：决定动作是自动执行还是仅展示建议。
 *
 * <p>配置 {@code ms.middleware.autonomy.auto-execute-max-risk} 默认 LOW：
 * LOW 风险动作 AUTO，MEDIUM/HIGH 只写 ADVISE 时间线。</p>
 */
public class AutonomyPolicy {

    private final AutonomyRisk maxAutoRisk;

    public AutonomyPolicy(MsMiddlewareProperties properties) {
        this.maxAutoRisk = AutonomyRisk.fromString(properties.getAutonomy().getAutoExecuteMaxRisk());
    }

    /**
     * @return AUTO 表示 Actuator 可执行；ADVISE 表示仅记录建议，等人确认
     */
    public AutonomyPolicyDecision evaluate(PlannedAction action) {
        if (action.getRisk().isAtMost(maxAutoRisk)) {
            return AutonomyPolicyDecision.AUTO;
        }
        return AutonomyPolicyDecision.ADVISE;
    }
}
