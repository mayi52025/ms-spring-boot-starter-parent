package com.ms.middleware.autonomy.plan;

import java.time.Instant;
import java.util.UUID;

/**
 * 控制台「推荐区」展示的配置级建议。
 *
 * <p>与 {@link PlannedAction} 区别：推荐通常不立即执行，需运维在控制台点击采纳；
 * {@link #recommendationId} 用于采纳 API 与账本审计关联。</p>
 */
public class AutonomyRecommendation {

    /**
     * 推荐唯一 ID（8 位），采纳时 URL 路径参数。
     * 新建时自动生成；反序列化旧数据时若为空会懒补。
     */
    private String recommendationId;
    /** 推荐标题，展示在控制台 */
    private String title;
    /** 详细说明 */
    private String description;
    /** 建议修改的配置键或 YAML 片段；后续可对接 Nacos */
    private String suggestedConfig;
    /** 是否默认需要人工确认（生产环境应为 true） */
    private boolean requiresApproval = true;
    /** 人机协同状态，由采纳/拒绝 API 更新 */
    private RecommendationStatus status = RecommendationStatus.PENDING;
    /** 采纳或拒绝时刻 */
    private Instant decidedAt;
    /** 操作人标识，写入审计 */
    private String operator;
    /** 拒绝时的原因（采纳时为 null） */
    private String rejectReason;

    public AutonomyRecommendation() {
        assignRecommendationIdIfAbsent();
    }

    public AutonomyRecommendation(String title, String description, String suggestedConfig) {
        assignRecommendationIdIfAbsent();
        this.title = title;
        this.description = description;
        this.suggestedConfig = suggestedConfig;
    }

    /** 保证每条推荐都有稳定 ID，便于采纳与回放 */
    private void assignRecommendationIdIfAbsent() {
        if (recommendationId == null || recommendationId.isBlank()) {
            this.recommendationId = UUID.randomUUID().toString().substring(0, 8);
        }
    }

    public String getRecommendationId() {
        assignRecommendationIdIfAbsent();
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestedConfig() {
        return suggestedConfig;
    }

    public void setSuggestedConfig(String suggestedConfig) {
        this.suggestedConfig = suggestedConfig;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public RecommendationStatus getStatus() {
        return status != null ? status : RecommendationStatus.PENDING;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
