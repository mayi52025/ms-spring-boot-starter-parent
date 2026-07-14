package com.ms.middleware.autonomy.run;

import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 账本公共逻辑：租户补全、时间线追加、SSE 事件发布。
 *
 * <p>所有 {@link AutonomyLedger} 实现必须以 {@link AutonomyTenantProvider#getTenant()} 为边界：
 * 读操作只查当前 tenant，写操作强制把 run 绑定到当前 tenant（Step 6 多应用隔离）。</p>
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

    /**
     * 写入账本前强制绑定当前应用 tenant。
     * <p>避免调用方传入其它应用的 tenant 字段，导致同 Redis 实例下 key 串写。</p>
     */
    protected void ensureTenant(AutonomyRun run) {
        run.setTenant(currentTenant());
    }

    /** 当前进程识别的 tenant，默认来自 {@code spring.application.name} */
    protected String currentTenant() {
        return tenantProvider.getTenant();
    }
}
