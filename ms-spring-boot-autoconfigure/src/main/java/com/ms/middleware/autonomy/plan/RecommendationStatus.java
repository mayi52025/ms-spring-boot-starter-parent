package com.ms.middleware.autonomy.plan;

/**
 * 配置推荐的人机协同状态。
 *
 * <p>由 {@link com.ms.middleware.autonomy.adoption.HumanAdoptionService} 在采纳/拒绝 API 中更新，
 * 并写入时间线 phase {@code ACCEPTED} 供审计回放。</p>
 */
public enum RecommendationStatus {

    /** 待运维决策 */
    PENDING,
    /** 已在控制台采纳（审计记录，Step 4 不自动改 Nacos） */
    ACCEPTED,
    /** 已明确拒绝 */
    REJECTED
}
