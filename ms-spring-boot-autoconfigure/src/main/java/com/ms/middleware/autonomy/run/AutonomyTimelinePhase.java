package com.ms.middleware.autonomy.run;

/**
 * 自治时间线 {@link TimelineEvent#getPhase()} 的标准取值（Phase 3 Step 0 契约）。
 *
 * <p>流水线语义：</p>
 * <pre>
 * DETECT → PLAN → (AUTO | ADVISE) → RECOMMEND → [ACCEPTED] → STABLE
 * </pre>
 *
 * <ul>
 *   <li>{@link #DETECT} — 发现故障，run 创建</li>
 *   <li>{@link #PLAN} — 决策引擎产出计划（含排序理由，Step 2 起增强）</li>
 *   <li>{@link #AUTO} — 策略允许且已自动执行的动作（Step 2 起替代字符串 {@code ACTION}）</li>
 *   <li>{@link #ADVISE} — 超风险或低置信度，仅建议等人确认</li>
 *   <li>{@link #RECOMMEND} — 配置级推荐（非即时执行类动作）</li>
 *   <li>{@link #ACCEPTED} — 运维在控制台采纳某条推荐（Step 4 实现）</li>
 *   <li>{@link #STABLE} — 主 incident 恢复，记录 MTTR，结案</li>
 * </ul>
 *
 * <p>兼容：编排器在 Step 0～1 仍可能写入 {@code ACTION}，语义等同 {@link #AUTO}，Step 2 起统一为 AUTO。</p>
 */
public enum AutonomyTimelinePhase {

    DETECT,
    PLAN,
    AUTO,
    ADVISE,
    RECOMMEND,
    ACCEPTED,
    STABLE;

    /** 写入 {@link TimelineEvent#setPhase(String)} 的标准 code */
    public String code() {
        return name();
    }

    /** {@code ACTION} 为历史 phase，等同 {@link #AUTO} */
    public static boolean isAutoExecution(String phase) {
        return AUTO.code().equals(phase) || "ACTION".equals(phase);
    }
}
