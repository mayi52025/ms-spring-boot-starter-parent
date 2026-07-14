package com.ms.middleware.autonomy.run;

import java.util.List;
import java.util.Optional;

/**
 * 自治运行账本：记录 run、时间线与状态变更。
 *
 * <p><b>多应用隔离（Step 6）：</b>所有查询/列表均以 {@link com.ms.middleware.autonomy.tenant.AutonomyTenantProvider}
 * 返回的 tenant 为边界；内存与 Redisson 实现均使用 {@code tenant:runId} 作为存储 key，
 * 同一 Redis 上部署 order-system、payment-service 时互不可见。</p>
 */
public interface AutonomyLedger {

    /** 创建 run 并写入首条 DETECT 时间线（tenant 强制为当前应用） */
    AutonomyRun startRun(AutonomyRun run);

    /** 按当前 tenant + runId 查询；其它 tenant 的同 id run 不可见 */
    Optional<AutonomyRun> get(String runId);

    List<AutonomyRun> listRecent();

    /** 当前 tenant 下最近 N 条 run，按 startedAt 倒序 */
    List<AutonomyRun> listRecent(int limit);

    /** 当前 tenant 下未 STABLE / CLOSED 的 run，供控制台 /api/issues 使用 */
    List<AutonomyRun> listActive();

    /** 追加时间线；实现类应同时发布 ConsoleTimelineEvent */
    void appendTimeline(AutonomyRun run, String phase, String message);

    /**
     * 追加时间线并关联推荐 ID（phase 为 ACCEPTED 时使用）。
     */
    void appendTimeline(AutonomyRun run, String phase, String message, String recommendationId);

    /**
     * 追加带审计信息的时间线（采纳/发布 API 使用）。
     */
    void appendTimeline(AutonomyRun run, String phase, String message, String recommendationId,
                        String operator, String clientIp);

    void update(AutonomyRun run);
}
