package com.ms.middleware.autonomy.adoption.nacos;

/**
 * 创建 Nacos 配置草稿的请求上下文。
 */
public class NacosDraftRequest {

    private String applicationName;
    private String recommendationId;
    private String suggestedConfig;
    private String operator;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getSuggestedConfig() {
        return suggestedConfig;
    }

    public void setSuggestedConfig(String suggestedConfig) {
        this.suggestedConfig = suggestedConfig;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
