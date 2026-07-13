package com.ms.middleware.autonomy.recovery;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * STABLE 结案时的恢复证据：对比故障基线与恢复后指标，供 API/控制台展示。
 *
 * <p>Phase 4 Step 0 契约；由 {@link RecoveryEvidenceBuilder} 在编排器 STABLE 时填充。</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecoveryEvidence {

    /** 主 incident 类型，如 MQ_DEGRADED */
    private String incidentType;
    /** 人类可读摘要，如「MQ窗口失败 5→0（阈值&lt;3）」 */
    private String summary;
    /** 判定条件说明，如 mqFailedCount &lt; mqFailedWarnThreshold */
    private String resolutionRule;
    /** 关键指标前后对比 */
    private List<RecoveryMetricDelta> metrics = new ArrayList<>();

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

    public String getResolutionRule() {
        return resolutionRule;
    }

    public void setResolutionRule(String resolutionRule) {
        this.resolutionRule = resolutionRule;
    }

    public List<RecoveryMetricDelta> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<RecoveryMetricDelta> metrics) {
        this.metrics = metrics != null ? metrics : new ArrayList<>();
    }
}
