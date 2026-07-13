package com.ms.middleware.autonomy.rule;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自治规则 YAML 绑定：incident 识别链、Runbook 候选、配置推荐。
 *
 * <p>配置前缀：{@code ms.middleware.autonomy.rules}</p>
 * <p>未配置或 incidents 为空时，{@link AutonomyRulesDefaults} 提供与 Step 2 硬编码等价的默认规则。</p>
 */
@ConfigurationProperties(prefix = "ms.middleware.autonomy.rules")
public class AutonomyRulesProperties {

    /** Runbook 版本号，写入 PLAN 时间线供审计；未配置时默认 default@1.0 */
    private String version = "default@1.0";

    /**
     * 有序 incident 识别链：先匹配者优先。
     * 为空时使用内置默认链（Redis → Rabbit → MQ → Cache）。
     */
    private List<IncidentDetectionRule> incidentDetection = new ArrayList<>();

    /**
     * incident 类型 → Runbook + 推荐 + 摘要模板。
     * key 如 REDIS_UNAVAILABLE、MQ_DEGRADED。
     */
    private Map<String, IncidentRuleDefinition> incidents = new LinkedHashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<IncidentDetectionRule> getIncidentDetection() {
        return incidentDetection;
    }

    public void setIncidentDetection(List<IncidentDetectionRule> incidentDetection) {
        this.incidentDetection = incidentDetection != null ? incidentDetection : new ArrayList<>();
    }

    public Map<String, IncidentRuleDefinition> getIncidents() {
        return incidents;
    }

    public void setIncidents(Map<String, IncidentRuleDefinition> incidents) {
        this.incidents = incidents != null ? incidents : new LinkedHashMap<>();
    }

    /** 单条 incident 识别规则 */
    public static class IncidentDetectionRule {

        /**
         * 条件标识，见 {@link IncidentConditionEvaluator}。
         * 如 redis-unhealthy、mq-degraded。
         */
        private String condition;
        /** 匹配后写入 plan 的 incident 类型 */
        private String incidentType;

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getIncidentType() {
            return incidentType;
        }

        public void setIncidentType(String incidentType) {
            this.incidentType = incidentType;
        }
    }

    /** 某一 incident 的完整规则定义 */
    public static class IncidentRuleDefinition {

        /**
         * PLAN 摘要模板，支持占位符：
         * {mqFailedCount}、{mqFailedWarnThreshold}、
         * {cacheHitRatePercent}、{cacheHitRateWarnThresholdPercent}
         */
        private String summary;
        /** Runbook 动作候选 */
        private List<RuleActionDefinition> actions = new ArrayList<>();
        /** 配置级推荐（控制台推荐区） */
        private List<RuleRecommendationDefinition> recommendations = new ArrayList<>();

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<RuleActionDefinition> getActions() {
            return actions;
        }

        public void setActions(List<RuleActionDefinition> actions) {
            this.actions = actions != null ? actions : new ArrayList<>();
        }

        public List<RuleRecommendationDefinition> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<RuleRecommendationDefinition> recommendations) {
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        }
    }

    /** Runbook 单条动作候选 */
    public static class RuleActionDefinition {

        /** 动作类型枚举名，如 TRIGGER_REDIS_RECOVERY */
        private String type;
        /** Runbook 顺序（越小越优先） */
        private int order = 1;
        /** 是否针对根因 */
        private boolean addressesRootCause;
        /** 人类可读理由 */
        private String reason;
        /**
         * 生效条件：always（默认）、hot-keys-present。
         * 不满足时该候选不进入选优池。
         */
        private String when = "always";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public boolean isAddressesRootCause() {
            return addressesRootCause;
        }

        public void setAddressesRootCause(boolean addressesRootCause) {
            this.addressesRootCause = addressesRootCause;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getWhen() {
            return when != null ? when : "always";
        }

        public void setWhen(String when) {
            this.when = when;
        }
    }

    /** 配置级推荐定义 */
    public static class RuleRecommendationDefinition {

        private String title;
        private String description;
        private String suggestedConfig;

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
    }
}
