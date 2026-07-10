package com.ms.middleware.autonomy.policy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;
import com.ms.middleware.autonomy.plan.PlannedAction;

public class AutonomyPolicy {

    private final AutonomyRisk maxAutoRisk;

    public AutonomyPolicy(MsMiddlewareProperties properties) {
        this.maxAutoRisk = AutonomyRisk.fromString(properties.getAutonomy().getAutoExecuteMaxRisk());
    }

    public AutonomyPolicyDecision evaluate(PlannedAction action) {
        if (action.getRisk().isAtMost(maxAutoRisk)) {
            return AutonomyPolicyDecision.AUTO;
        }
        return AutonomyPolicyDecision.ADVISE;
    }
}
