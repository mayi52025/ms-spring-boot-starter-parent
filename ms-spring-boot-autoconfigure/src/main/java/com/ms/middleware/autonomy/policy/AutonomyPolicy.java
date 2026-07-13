package com.ms.middleware.autonomy.policy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;
import com.ms.middleware.autonomy.plan.PlannedAction;

/**
 * 策略门控：决定动作是自动执行还是仅展示建议。
 *
 * <p>需同时满足：</p>
 * <ul>
 *   <li>风险 ≤ {@code ms.middleware.autonomy.auto-execute-max-risk}（默认 LOW）</li>
 *   <li>置信度 ≥ {@code ms.middleware.autonomy.auto-execute-min-confidence}（默认 0.7）</li>
 * </ul>
 */
public class AutonomyPolicy {

    private final AutonomyRisk maxAutoRisk;
    /** 自动执行最低置信度阈值 */
    private final double minAutoConfidence;

    public AutonomyPolicy(MsMiddlewareProperties properties) {
        this.maxAutoRisk = AutonomyRisk.fromString(properties.getAutonomy().getAutoExecuteMaxRisk());
        this.minAutoConfidence = properties.getAutonomy().getAutoExecuteMinConfidence();
    }

    /**
     * @return AUTO 表示 Actuator 可执行；ADVISE 表示仅记录建议，等人确认
     */
    public AutonomyPolicyDecision evaluate(PlannedAction action) {
        if (action.getRisk().isAtMost(maxAutoRisk)
                && action.getConfidence() >= minAutoConfidence) {
            return AutonomyPolicyDecision.AUTO;
        }
        return AutonomyPolicyDecision.ADVISE;
    }

    public double getMinAutoConfidence() {
        return minAutoConfidence;
    }
}
