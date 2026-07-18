package com.ms.middleware.autonomy.run;

import org.springframework.context.ApplicationEvent;

/**
 * run 进入 STABLE 并落账后发布；Phase 5.4 RAG 异步索引监听此事件。
 */
public class RunStabilizedEvent extends ApplicationEvent {

    private final AutonomyRun run;

    public RunStabilizedEvent(Object source, AutonomyRun run) {
        super(source);
        this.run = run;
    }

    public AutonomyRun getRun() {
        return run;
    }
}
