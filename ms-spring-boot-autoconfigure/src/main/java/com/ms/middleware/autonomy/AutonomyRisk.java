package com.ms.middleware.autonomy;

/**
 * 自治动作风险等级
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
