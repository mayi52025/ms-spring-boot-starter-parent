package com.ms.middleware.autonomy;

import org.springframework.scheduling.annotation.Scheduled;

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
