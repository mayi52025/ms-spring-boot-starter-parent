package com.ms.middleware.autonomy.plan;

/**
 * 需人工确认的配置级建议（展示在控制台推荐区）。
 * suggestedConfig 为 YAML 片段或配置键，Phase 3 可接「采纳」API 写回 Nacos。
 */
public class AutonomyRecommendation {

    private String title;
    private String description;
    private String suggestedConfig;
    private boolean requiresApproval = true;

    public AutonomyRecommendation() {
    }

    public AutonomyRecommendation(String title, String description, String suggestedConfig) {
        this.title = title;
        this.description = description;
        this.suggestedConfig = suggestedConfig;
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
