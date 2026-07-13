package com.ms.middleware.autonomy.recovery;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 单条指标的恢复前后对比。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecoveryMetricDelta {

    /** 机器可读键，如 mqFailedCount */
    private String key;
    /** 展示标签，如 MQ窗口失败次数 */
    private String label;
    private String beforeValue;
    private String afterValue;
    /** 恢复判定参考，如 &lt;3 或 ≥50% */
    private String threshold;

    public RecoveryMetricDelta() {
    }

    public RecoveryMetricDelta(String key, String label, String beforeValue, String afterValue, String threshold) {
        this.key = key;
        this.label = label;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.threshold = threshold;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }
}
