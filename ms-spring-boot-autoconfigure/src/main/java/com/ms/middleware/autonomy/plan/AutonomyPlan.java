package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.context.AutonomyContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 决策引擎产出的一次处置计划，挂在 {@link com.ms.middleware.autonomy.run.AutonomyRun} 上。
 * incidentType 用于分类与搜索（如 REDIS_UNAVAILABLE、MQ_DEGRADED）。
 */
public class AutonomyPlan {

    private String incidentType;
    private String summary;
    private AutonomyContext context;
    private List<PlannedAction> actions = new ArrayList<>();
    private List<AutonomyRecommendation> recommendations = new ArrayList<>();

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public AutonomyContext getContext() {
        return context;
    }

    public void setContext(AutonomyContext context) {
        this.context = context;
    }

    public List<PlannedAction> getActions() {
        return actions;
    }

    public void setActions(List<PlannedAction> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public List<AutonomyRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<AutonomyRecommendation> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }
}
