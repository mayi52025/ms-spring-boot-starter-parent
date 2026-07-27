package com.ms.middleware.autonomy;

/**
 * 自治 run 状态机。
 *
 * <ul>
 *   <li>DETECTED — 刚发现故障，尚未生成计划</li>
 *   <li>PLANNED — 已有 AutonomyPlan</li>
 *   <li>EXECUTING — 动作已评估/执行中</li>
 *   <li>STABLE — 指标恢复正常，可计算 MTTR</li>
 *   <li>ESCALATED — 限流后无改善，升级人工（停止继续 AUTO 加压）</li>
 *   <li>CLOSED — 预留：人工关闭</li>
 * </ul>
 */
public enum AutonomyRunStatus {
    DETECTED,
    PLANNED,
    EXECUTING,
    STABLE,
    ESCALATED,
    CLOSED
}
