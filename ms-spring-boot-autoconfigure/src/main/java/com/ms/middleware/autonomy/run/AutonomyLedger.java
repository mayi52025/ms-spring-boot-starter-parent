package com.ms.middleware.autonomy.run;

import java.util.List;
import java.util.Optional;

/**
 * 自治运行账本：记录 run、时间线与状态变更。
 * Phase 2 将提供 Redisson 实现；Phase 1 默认内存实现。
 */
public interface AutonomyLedger {

    AutonomyRun startRun(AutonomyRun run);

    Optional<AutonomyRun> get(String runId);

    List<AutonomyRun> listRecent();

    List<AutonomyRun> listRecent(int limit);

    List<AutonomyRun> listActive();

    void appendTimeline(AutonomyRun run, String phase, String message);

    void update(AutonomyRun run);
}
