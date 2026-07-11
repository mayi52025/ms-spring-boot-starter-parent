package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;

/**
 * 计划中的单个可执行动作；经 Policy 评估后可能 AUTO 执行或仅 ADVISE。
 *
 * <p>Phase 3 Step 0：增加 {@link #rank}、{@link #score}、{@link #confidence}，
 * 供 Step 2 排序选优；未排序前均为 0。</p>
 *
 * <p>executionStatus 示例：SUCCESS / FAILED / SKIPPED / ADVISE。</p>
 */
public class PlannedAction {

    private AutonomyActionType actionType;
    private AutonomyRisk risk;
    private String reason;
    /** 1-based 排序位，0 表示尚未参与排序（Step 2 前） */
    private int rank;
    /** 综合得分 0～1，Step 2 ActionRanker 写入 */
    private double score;
    /** 自动执行置信度 0～1，低于阈值时只 ADVISE */
    private double confidence;
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

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
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
