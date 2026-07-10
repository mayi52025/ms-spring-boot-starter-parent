package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;

/**
 * 计划中的单个可执行动作；经 Policy 评估后可能 AUTO 执行或仅 ADVISE。
 * executionStatus 示例：SUCCESS / FAILED / SKIPPED / ADVISE。
 */
public class PlannedAction {

    private AutonomyActionType actionType;
    private AutonomyRisk risk;
    private String reason;
    private AutonomyPolicyDecision policyDecision = AutonomyPolicyDecision.ADVISE;
    private String executionStatus;
    private String executionDetail;

    public AutonomyActionType getActionType() {
        return actionType;
    }

    public void setActionType(AutonomyActionType actionType) {
        this.actionType = actionType;
    }

    public AutonomyRisk getRisk() {
        return risk;
    }

    public void setRisk(AutonomyRisk risk) {
        this.risk = risk;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AutonomyPolicyDecision getPolicyDecision() {
        return policyDecision;
    }

    public void setPolicyDecision(AutonomyPolicyDecision policyDecision) {
        this.policyDecision = policyDecision;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getExecutionDetail() {
        return executionDetail;
    }

    public void setExecutionDetail(String executionDetail) {
        this.executionDetail = executionDetail;
    }
}
