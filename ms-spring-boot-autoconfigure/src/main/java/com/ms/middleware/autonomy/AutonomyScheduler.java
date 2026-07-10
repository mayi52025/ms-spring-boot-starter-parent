package com.ms.middleware.autonomy;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * 自治定时入口：按 {@code ms.middleware.autonomy.scan-interval-ms} 触发编排器。
 * 默认 30s；生产可按故障敏感度调小，注意与 Redis/MQ 健康检查频率平衡。
 */
public class AutonomyScheduler {

    private final AutonomyOrchestrator orchestrator;

    public AutonomyScheduler(AutonomyOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${ms.middleware.autonomy.scan-interval-ms:30000}")
    public void scan() {
        orchestrator.tick();
    }
}
