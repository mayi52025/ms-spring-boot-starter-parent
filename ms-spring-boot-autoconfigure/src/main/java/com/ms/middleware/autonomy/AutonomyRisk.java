package com.ms.middleware.autonomy;

/**
 * 动作风险等级，与 {@link com.ms.middleware.autonomy.AutonomyActionType} 绑定。
 * {@link #isAtMost(AutonomyRisk)} 用于和 auto-execute-max-risk 比较。
 */
public enum AutonomyRisk {
    LOW,
    MEDIUM,
    HIGH;

    public static AutonomyRisk fromString(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        return AutonomyRisk.valueOf(value.trim().toUpperCase());
    }

    public boolean isAtMost(AutonomyRisk max) {
        return this.ordinal() <= max.ordinal();
    }
}
