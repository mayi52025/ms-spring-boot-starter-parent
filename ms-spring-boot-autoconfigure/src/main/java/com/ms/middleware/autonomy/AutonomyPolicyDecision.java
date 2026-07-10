package com.ms.middleware.autonomy;

/**
 * 策略门控结果。
 * <ul>
 *   <li>AUTO — 允许 Actuator 执行</li>
 *   <li>ADVISE — 仅展示建议（Phase 1 对超风险动作用此值）</li>
 *   <li>DENY — 预留：明确拒绝</li>
 * </ul>
 */
public enum AutonomyPolicyDecision {
    AUTO,
    ADVISE,
    DENY
}
