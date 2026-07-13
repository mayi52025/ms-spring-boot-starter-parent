package com.ms.middleware.autonomy.policy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;
import com.ms.middleware.autonomy.plan.PlannedAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 {@link AutonomyPolicy} 风险与置信度双门控。
 */
class AutonomyPolicyTest {

    /** LOW 风险且置信度 ≥ 0.7 → AUTO */
    @Test
    void lowRiskHighConfidenceAuto() {
        AutonomyPolicy policy = policyWithMinConfidence(0.7);
        PlannedAction action = action(AutonomyRisk.LOW, 0.82);

        assertEquals(AutonomyPolicyDecision.AUTO, policy.evaluate(action));
    }

    /** LOW 风险但置信度不足 → ADVISE */
    @Test
    void lowRiskLowConfidenceAdvise() {
        AutonomyPolicy policy = policyWithMinConfidence(0.7);
        PlannedAction action = action(AutonomyRisk.LOW, 0.55);

        assertEquals(AutonomyPolicyDecision.ADVISE, policy.evaluate(action));
    }

    /** MEDIUM 风险超过 auto-execute-max-risk → ADVISE */
    @Test
    void mediumRiskAlwaysAdvise() {
        AutonomyPolicy policy = policyWithMinConfidence(0.7);
        PlannedAction action = action(AutonomyRisk.MEDIUM, 0.95);

        assertEquals(AutonomyPolicyDecision.ADVISE, policy.evaluate(action));
    }

    private AutonomyPolicy policyWithMinConfidence(double minConfidence) {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getAutonomy().setAutoExecuteMaxRisk("LOW");
        properties.getAutonomy().setAutoExecuteMinConfidence(minConfidence);
        return new AutonomyPolicy(properties);
    }

    private PlannedAction action(AutonomyRisk risk, double confidence) {
        PlannedAction action = new PlannedAction();
        action.setActionType(AutonomyActionType.THROTTLE_CONSUMER);
        action.setRisk(risk);
        action.setConfidence(confidence);
        return action;
    }
}
