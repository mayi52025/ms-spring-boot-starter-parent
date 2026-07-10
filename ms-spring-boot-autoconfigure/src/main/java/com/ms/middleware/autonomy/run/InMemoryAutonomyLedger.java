package com.ms.middleware.autonomy.run;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存账本（默认实现，单测与单机部署使用）。
 */
public class InMemoryAutonomyLedger implements AutonomyLedger {

    private static final int DEFAULT_MAX_RUNS = 200;

    private final Map<String, AutonomyRun> runs = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;
    private final AutonomyTenantProvider tenantProvider;
    private final int maxRuns;

    public InMemoryAutonomyLedger(ApplicationEventPublisher eventPublisher,
                                    AutonomyTenantProvider tenantProvider) {
        this(eventPublisher, tenantProvider, DEFAULT_MAX_RUNS);
    }

    public InMemoryAutonomyLedger(ApplicationEventPublisher eventPublisher,
                                    AutonomyTenantProvider tenantProvider,
                                    int maxRuns) {
        this.eventPublisher = eventPublisher;
        this.tenantProvider = tenantProvider;
        this.maxRuns = maxRuns;
    }

    @Override
    public AutonomyRun startRun(AutonomyRun run) {
        ensureTenant(run);
        trimIfNeeded(run.getTenant());
        runs.put(storageKey(run.getTenant(), run.getRunId()), run);
        publishTimeline(run, "DETECT", "检测到中间件异常，runId=" + run.getRunId());
        return run;
    }

    @Override
    public Optional<AutonomyRun> get(String runId) {
        return Optional.ofNullable(runs.get(storageKey(currentTenant(), runId)));
    }

    @Override
    public List<AutonomyRun> listRecent() {
        return listRecent(maxRuns);
    }

    @Override
    public List<AutonomyRun> listRecent(int limit) {
        String tenant = currentTenant();
        return runs.values().stream()
                .filter(run -> tenant.equals(run.getTenant()))
                .sorted(Comparator.comparing(AutonomyRun::getStartedAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<AutonomyRun> listActive() {
        String tenant = currentTenant();
        List<AutonomyRun> active = new ArrayList<>();
        for (AutonomyRun run : runs.values()) {
            if (!tenant.equals(run.getTenant())) {
                continue;
            }
            AutonomyRunStatus status = run.getStatus();
            if (status != AutonomyRunStatus.STABLE && status != AutonomyRunStatus.CLOSED) {
                active.add(run);
            }
        }
        active.sort(Comparator.comparing(AutonomyRun::getStartedAt).reversed());
        return active;
    }

    @Override
    public void appendTimeline(AutonomyRun run, String phase, String message) {
        publishTimeline(run, phase, message);
    }

    @Override
    public void update(AutonomyRun run) {
        ensureTenant(run);
        runs.put(storageKey(run.getTenant(), run.getRunId()), run);
    }

    private void publishTimeline(AutonomyRun run, String phase, String message) {
        TimelineEvent event = new TimelineEvent(run.getRunId(), phase, message);
        run.addTimeline(event);
        eventPublisher.publishEvent(new ConsoleTimelineEvent(this, event));
    }

    private void ensureTenant(AutonomyRun run) {
        if (run.getTenant() == null || run.getTenant().isBlank()) {
            run.setTenant(currentTenant());
        }
    }

    private String currentTenant() {
        return tenantProvider.getTenant();
    }

    private static String storageKey(String tenant, String runId) {
        return tenant + ":" + runId;
    }

    private void trimIfNeeded(String tenant) {
        long tenantCount = runs.values().stream().filter(r -> tenant.equals(r.getTenant())).count();
        if (tenantCount <= maxRuns) {
            return;
        }
        runs.entrySet().stream()
                .filter(e -> tenant.equals(e.getValue().getTenant()))
                .sorted(Comparator.comparing(e -> e.getValue().getStartedAt()))
                .limit(tenantCount - maxRuns + 1)
                .map(Map.Entry::getKey)
                .forEach(runs::remove);
    }
}
