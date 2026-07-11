package com.ms.middleware.autonomy.plan;

import java.util.UUID;

/**
 * 需人工确认的配置级建议（展示在控制台推荐区）。
 *
 * <p>{@link #recommendationId} 为稳定标识，供 Step 4
 * {@code POST /api/recommendations/{id}/accept} 采纳审计。</p>
 *
 * <p>suggestedConfig 为 YAML 片段或配置键，Phase 4 可接 Nacos 写回。</p>
 */
public class AutonomyRecommendation {

    private String recommendationId;
    private String title;
    private String description;
    private String suggestedConfig;
    private boolean requiresApproval = true;

    public AutonomyRecommendation() {
        assignRecommendationIdIfAbsent();
    }

    public AutonomyRecommendation(String title, String description, String suggestedConfig) {
        assignRecommendationIdIfAbsent();
        this.title = title;
        this.description = description;
        this.suggestedConfig = suggestedConfig;
    }

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
}
