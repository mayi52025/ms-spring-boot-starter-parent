package com.ms.middleware.autonomy.run;

/**
 * 自治控制台时间线事件的 phase 标准枚举。
 *
 * <p>一次故障 run 的典型事件顺序：</p>
 * <pre>
 * DETECT → PLAN → (AUTO | ADVISE) → RECOMMEND → [ACCEPTED] → STABLE
 * </pre>
 *
 * <ul>
 *   <li>{@link #DETECT} — 发现故障，创建 run</li>
 *   <li>{@link #PLAN} — 决策引擎生成处置计划（后续可增强排序说明）</li>
 *   <li>{@link #AUTO} — 策略允许且已自动执行的动作</li>
 *   <li>{@link #ADVISE} — 风险或置信度不足，仅展示建议等人确认</li>
 *   <li>{@link #RECOMMEND} — 配置级优化推荐（控制台推荐区）</li>
 *   <li>{@link #ACCEPTED} — 运维采纳某条推荐（采纳 API 写入）</li>
 *   <li>{@link #PUBLISH} — nacos-draft 模式下二次确认发布生产配置</li>
 *   <li>{@link #STABLE} — 主 incident 恢复，记录 MTTR，本次自治结束</li>
 * </ul>
 *
 * <p>兼容：历史代码可能写入字符串 {@code ACTION}，语义与 {@link #AUTO} 相同。</p>
 */
public enum AutonomyTimelinePhase {

    /** 检测到故障 */
    DETECT,
    /** 生成处置计划 */
    PLAN,
    /** 已自动执行 */
    AUTO,
    /** 仅建议，未自动执行 */
    ADVISE,
    /** 配置推荐 */
    RECOMMEND,
    /** 人工采纳推荐 */
    ACCEPTED,
    /** 确认发布 Nacos 草稿到生产 */
    PUBLISH,
    /** 故障恢复，结案 */
    STABLE;

    /** 写入 {@link TimelineEvent#setPhase(String)} 的标准字符串 */
    public String code() {
        return name();
    }

    /**
     * 判断时间线 phase 是否表示「自动执行」类事件。
     * 兼容旧值 ACTION。
     */
    public static boolean isAutoExecution(String phase) {
        return AUTO.code().equals(phase) || "ACTION".equals(phase);
    }
}
