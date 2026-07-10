package com.ms.middleware.autonomy.run;

import java.util.List;
import java.util.Optional;

/**
 * 自治运行账本：记录 run、时间线与状态变更。
 * Phase 2 将提供 Redisson 实现；Phase 1 默认内存实现。
 */
public interface AutonomyLedger {

    /** 创建 run 并写入首条 DETECT 时间线 */
    AutonomyRun startRun(AutonomyRun run);

    /** 按当前 tenant 查询单个 run */
    Optional<AutonomyRun> get(String runId);

    List<AutonomyRun> listRecent();

    /** 最近 N 条 run，按 startedAt 倒序 */
    List<AutonomyRun> listRecent(int limit);

    /** 未 STABLE / CLOSED 的 run，供 /api/issues 使用 */
    List<AutonomyRun> listActive();

    /** 追加时间线；实现类应同时发布 ConsoleTimelineEvent */
    void appendTimeline(AutonomyRun run, String phase, String message);

    void update(AutonomyRun run);
}
