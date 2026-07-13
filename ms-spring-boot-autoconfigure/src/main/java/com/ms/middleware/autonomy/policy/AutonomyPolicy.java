package com.ms.middleware.autonomy.policy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;
import com.ms.middleware.autonomy.plan.PlannedAction;

/**
 * 策略门控：决定动作是自动执行还是仅展示建议。
 *
 * <p>两档证据门槛（「先自治止血，后人工决策」）：</p>
 * <ul>
 *   <li>LOW 风险 + 证据 ≥ {@code auto-execute-min-confidence-low}（默认 0.55）→ AUTO</li>
 *   <li>其他允许风险 + 证据 ≥ {@code auto-execute-min-confidence}（默认 0.7）→ AUTO</li>
 *   <li>风险 &gt; {@code auto-execute-max-risk} → 始终 ADVISE</li>
 * </ul>
 *
 * <p>典型：MQ 踩线限流（LOW、证据 ~0.65）自动执行；延迟重试（MEDIUM）仅备选。</p>
 */
public class AutonomyPolicy {

    private final AutonomyRisk maxAutoRisk;
    /** 标准自动执行最低证据强度 */
    private final double minAutoConfidence;
    /** LOW 风险止血动作最低证据强度 */
    private final double minAutoConfidenceLow;

    public AutonomyPolicy(MsMiddlewareProperties properties) {
        this.maxAutoRisk = AutonomyRisk.fromString(properties.getAutonomy().getAutoExecuteMaxRisk());
        this.minAutoConfidence = properties.getAutonomy().getAutoExecuteMinConfidence();
        this.minAutoConfidenceLow = properties.getAutonomy().getAutoExecuteMinConfidenceLow();
    }

    /**
     * @return AUTO 表示 Actuator 可执行；ADVISE 表示仅记录建议或备选
     */
    public AutonomyPolicyDecision evaluate(PlannedAction action) {
        if (!action.getRisk().isAtMost(maxAutoRisk)) {
            return AutonomyPolicyDecision.ADVISE;
        }
        if (action.getRisk() == AutonomyRisk.LOW
                && action.getConfidence() >= minAutoConfidenceLow) {
            return AutonomyPolicyDecision.AUTO;
        }
        if (action.getConfidence() >= minAutoConfidence) {
            return AutonomyPolicyDecision.AUTO;
        }
        return AutonomyPolicyDecision.ADVISE;
    }

    public double getMinAutoConfidence() {
        return minAutoConfidence;
    }

    public double getMinAutoConfidenceLow() {
        return minAutoConfidenceLow;
    }
}
