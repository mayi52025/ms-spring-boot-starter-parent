package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;

/**
 * 自治计划中的单个可执行动作。
 *
 * <p>生命周期：决策引擎创建 → {@link com.ms.middleware.autonomy.policy.AutonomyPolicy} 评估
 * → 通过则 {@link com.ms.middleware.autonomy.act.AutonomyActuator} 执行并写入时间线。</p>
 */
public class PlannedAction {

    /** 动作类型（预热、自愈、限流等） */
    private AutonomyActionType actionType;
    /** 风险等级，用于策略门控 */
    private AutonomyRisk risk;
    /** 人类可读的选用理由，展示在 PLAN 时间线 */
    private String reason;
    /**
     * 排序位（1 表示 Runbook 选优后的首选动作）。
     * 0 表示尚未经过 {@link ActionSelector} 选优。
     */
    private int rank;
    /**
     * 保留字段（JSON 契约兼容），规则选优不使用浮点得分，恒为 0。
     */
    private double score;
    /**
     * 证据强度 0～1，由 {@link EvidenceStrengthEvaluator} 评估。
     * 低于配置阈值时即使 LOW 风险也只 ADVISE 不 AUTO。
     */
    private double confidence;
    /** 策略评估结果：AUTO 或 ADVISE */
    private AutonomyPolicyDecision policyDecision = AutonomyPolicyDecision.ADVISE;
    /** 执行结果：SUCCESS / FAILED / SKIPPED / ADVISE */
    private String executionStatus;
    /** 执行详情，写入 ACTION/AUTO 时间线 */
    private String executionDetail;
    /** 是否已由人工采纳执行（备选 ADVISE 动作） */
    private boolean humanAccepted;

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

    public boolean isHumanAccepted() {
        return humanAccepted;
    }

    public void setHumanAccepted(boolean humanAccepted) {
        this.humanAccepted = humanAccepted;
    }
}
