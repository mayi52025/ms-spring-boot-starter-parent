package com.ms.middleware.autonomy.run;

import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 账本公共逻辑：租户补全、时间线追加、SSE 事件发布。
 */
public abstract class AbstractAutonomyLedger implements AutonomyLedger {

    protected final ApplicationEventPublisher eventPublisher;
    protected final AutonomyTenantProvider tenantProvider;

    protected AbstractAutonomyLedger(ApplicationEventPublisher eventPublisher,
                                     AutonomyTenantProvider tenantProvider) {
        this.eventPublisher = eventPublisher;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void appendTimeline(AutonomyRun run, String phase, String message) {
        appendTimeline(run, phase, message, null);
    }

    @Override
    public void appendTimeline(AutonomyRun run, String phase, String message, String recommendationId) {
        publishTimeline(run, phase, message, recommendationId);
    }

    /** 追加时间线并广播 SSE；同时 mutate run.timeline 供 REST 查询 */
    protected void publishTimeline(AutonomyRun run, String phase, String message, String recommendationId) {
        TimelineEvent event = new TimelineEvent(run.getRunId(), phase, message);
        if (recommendationId != null && !recommendationId.isBlank()) {
            event.setRecommendationId(recommendationId);
        }
        run.addTimeline(event);
        eventPublisher.publishEvent(new ConsoleTimelineEvent(this, event));
    }

    protected void ensureTenant(AutonomyRun run) {
        if (run.getTenant() == null || run.getTenant().isBlank()) {
            run.setTenant(currentTenant());
        }
    }

    protected String currentTenant() {
        return tenantProvider.getTenant();
    }
}
