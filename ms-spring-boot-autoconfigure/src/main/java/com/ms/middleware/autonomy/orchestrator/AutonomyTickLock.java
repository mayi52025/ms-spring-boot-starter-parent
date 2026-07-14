package com.ms.middleware.autonomy.orchestrator;

/**
 * 多实例部署时，自治 tick 的集群互斥契约。
 *
 * <p><b>解决什么问题：</b>K8s 多 Pod 各自跑 {@link com.ms.middleware.autonomy.AutonomyScheduler}，
 * 若不加锁，多个 JVM 会在同一扫描周期内同时执行 PLAN/AUTO，导致重复限流、重复写账本。</p>
 *
 * <p><b>与 {@code activeRunId} 的分工：</b></p>
 * <ul>
 *   <li>{@code AutonomyTickLock} — 集群级，保证「同一 tenant 同一时刻只有一个实例进入 tick」</li>
 *   <li>{@code activeRunId} — JVM 内，保证「本进程内复用同一个 run，不重复 DETECT」</li>
 * </ul>
 *
 * <p>单机默认注入 {@link #noop()}，零开销、行为与 Step 5 之前完全一致。</p>
 */
@FunctionalInterface
public interface AutonomyTickLock {

    /**
     * 在持有 tick 锁时执行 action；未获锁则静默跳过本轮（不抛异常，由实现打 debug 日志）。
     *
     * @param tenant 租户标识（通常等于 spring.application.name），用于锁 key 隔离
     * @param action 单次 tick 业务逻辑（检测 → 计划 → 执行 → STABLE 判定）
     */
    void runIfLeader(String tenant, Runnable action);

    /**
     * 空实现：不做任何互斥，直接执行 action。
     * 用于 {@code distributed-lock-enabled=false} 或未接入 Redisson 时的默认行为。
     */
    static AutonomyTickLock noop() {
        return (tenant, action) -> action.run();
    }
}
