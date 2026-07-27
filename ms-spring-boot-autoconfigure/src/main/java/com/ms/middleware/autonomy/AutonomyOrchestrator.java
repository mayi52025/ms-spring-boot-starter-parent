package com.ms.middleware.autonomy;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.context.AutonomyContextBuilder;
import com.ms.middleware.autonomy.context.AutonomyContextSnapshot;
import com.ms.middleware.autonomy.recovery.RecoveryEvidence;
import com.ms.middleware.autonomy.recovery.RecoveryEvidenceBuilder;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.policy.AutonomyPolicy;
import com.ms.middleware.autonomy.run.AutonomyLedger;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
import com.ms.middleware.autonomy.orchestrator.AutonomyTickLock;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.autonomy.run.RunStabilizedEvent;
import com.ms.middleware.metrics.MsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 自治编排核心：把「检测 → 计划 → 门控 → 执行 → 稳定判定」串成一次 tick。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>同一故障周期内复用同一个 run（{@link #activeRunId}），避免每次扫描都新建记录</li>
 *   <li>指标恢复正常时写入 STABLE 并计算 MTTR，然后清空 activeRunId</li>
 *   <li>高风险动作只写 ADVISE 时间线，不调用 Actuator（见 {@link AutonomyPolicy}）</li>
 *   <li>MQ AUTO 限流后：超时 {@code SAFETY_UNWIND} / 无改善 {@code ESCALATE}，不经 LLM、不自动 publish</li>
 *   <li>多实例时由 {@link AutonomyTickLock} 保证集群内仅一个 JVM 执行 {@link #tick()}</li>
 * </ul>
 */
public class AutonomyOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AutonomyOrchestrator.class);

    private final AutonomyContextBuilder contextBuilder;
    private final AutonomyDecisionEngine decisionEngine;
    private final AutonomyPolicy policy;
    private final AutonomyActuator actuator;
    private final AutonomyLedger ledger;
    private final AutonomyTenantProvider tenantProvider;
    private final AutonomyMetrics autonomyMetrics;
    private final MsMetrics msMetrics;
    private final MsMiddlewareProperties properties;
    /** 集群 tick 互斥；单机为 noop，多实例为 Redisson 实现 */
    private final AutonomyTickLock tickLock;
    /** 可选：STABLE 后发布事件供 RAG 等旁路消费；单测可传 null */
    private final ApplicationEventPublisher eventPublisher;
    /** 可注入 Clock，单测用 fixed/offset 验证超时回撤 */
    private Clock clock;

    /** 当前 JVM 内正在处理的故障 run；稳定后置 null（只管本进程，不管多 Pod） */
    private volatile String activeRunId;

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider,
                                AutonomyMetrics autonomyMetrics,
                                MsMetrics msMetrics) {
        this(contextBuilder, decisionEngine, policy, actuator, ledger, tenantProvider,
                autonomyMetrics, msMetrics, new MsMiddlewareProperties(),
                AutonomyTickLock.noop(), null, Clock.systemUTC());
    }

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider,
                                AutonomyMetrics autonomyMetrics,
                                MsMetrics msMetrics,
                                MsMiddlewareProperties properties) {
        this(contextBuilder, decisionEngine, policy, actuator, ledger, tenantProvider,
                autonomyMetrics, msMetrics, properties, AutonomyTickLock.noop(), null, Clock.systemUTC());
    }

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider,
                                AutonomyMetrics autonomyMetrics,
                                MsMetrics msMetrics,
                                AutonomyTickLock tickLock) {
        this(contextBuilder, decisionEngine, policy, actuator, ledger, tenantProvider,
                autonomyMetrics, msMetrics, new MsMiddlewareProperties(), tickLock, null, Clock.systemUTC());
    }

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider,
                                AutonomyMetrics autonomyMetrics,
                                MsMetrics msMetrics,
                                AutonomyTickLock tickLock,
                                ApplicationEventPublisher eventPublisher) {
        this(contextBuilder, decisionEngine, policy, actuator, ledger, tenantProvider,
                autonomyMetrics, msMetrics, new MsMiddlewareProperties(), tickLock, eventPublisher, Clock.systemUTC());
    }

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider,
                                AutonomyMetrics autonomyMetrics,
                                MsMetrics msMetrics,
                                MsMiddlewareProperties properties,
                                AutonomyTickLock tickLock,
                                ApplicationEventPublisher eventPublisher) {
        this(contextBuilder, decisionEngine, policy, actuator, ledger, tenantProvider,
                autonomyMetrics, msMetrics, properties, tickLock, eventPublisher, Clock.systemUTC());
    }

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider,
                                AutonomyMetrics autonomyMetrics,
                                MsMetrics msMetrics,
                                MsMiddlewareProperties properties,
                                AutonomyTickLock tickLock,
                                ApplicationEventPublisher eventPublisher,
                                Clock clock) {
        this.contextBuilder = contextBuilder;
        this.decisionEngine = decisionEngine;
        this.policy = policy;
        this.actuator = actuator;
        this.ledger = ledger;
        this.tenantProvider = tenantProvider;
        this.autonomyMetrics = autonomyMetrics;
        this.msMetrics = msMetrics;
        this.properties = properties != null ? properties : new MsMiddlewareProperties();
        this.tickLock = tickLock != null ? tickLock : AutonomyTickLock.noop();
        this.eventPublisher = eventPublisher;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /** 单测注入时钟（超时回撤） */
    void useClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /**
     * 单次自治扫描入口，由 {@link AutonomyScheduler} 定时调用。
     */
    public void tick() {
        tickLock.runIfLeader(tenantProvider.getTenant(), this::doTick);
    }

    /** tick 实际业务：检测 → 计划 → 门控 → 执行 → 安全回撤/升级 → STABLE */
    private void doTick() {
        AutonomyContext context = contextBuilder.build();

        reconcileStaleActiveRuns(context);
        context = contextBuilder.build();

        if (shouldStabilizeActiveRun(context)) {
            stabilizeActiveRunIfNeeded(context);
            if (!context.hasIncident()) {
                return;
            }
        }

        if (!context.hasIncident()) {
            return;
        }

        AutonomyRun run = resolveOrCreateRun(context);

        // ESCALATED：不再 AUTO 加压；仅当 incident 恢复时允许 STABLE 清限流结案
        if (run.getStatus() == AutonomyRunStatus.ESCALATED) {
            String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
            AutonomyContext latest = contextBuilder.build();
            run.setContext(latest);
            if (contextBuilder.isIncidentResolved(incidentType, latest)) {
                activeRunId = run.getRunId();
                stabilizeActiveRunIfNeeded(latest);
            } else {
                ledger.update(run);
            }
            return;
        }

        // 同一故障周期内已 EXECUTING：安全兜底 → 自愈 → STABLE 判定
        if (run.getStatus() == AutonomyRunStatus.EXECUTING || run.getStatus() == AutonomyRunStatus.PLANNED) {
            String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
            AutonomyContext latest = contextBuilder.build();
            run.setContext(latest);
            if (applyMqThrottleSafetyGuards(run, latest)) {
                return;
            }
            if (run.getStatus() == AutonomyRunStatus.ESCALATED) {
                ledger.update(run);
                return;
            }
            retryRecoveryForActiveRun(latest, run);
            latest = contextBuilder.build();
            run.setContext(latest);
            if (contextBuilder.isIncidentResolved(incidentType, latest)) {
                activeRunId = run.getRunId();
                stabilizeActiveRunIfNeeded(latest);
                return;
            }
            ledger.update(run);
            return;
        }

        AutonomyPlan plan = decisionEngine.plan(context);
        run.setPlan(plan);
        run.setStatus(AutonomyRunStatus.PLANNED);
        String planDetail = plan.getSummary();
        if (plan.getRulesVersion() != null && !plan.getRulesVersion().isBlank()) {
            planDetail = planDetail + " | runbook=" + plan.getRulesVersion();
        }
        if (plan.getRankingSummary() != null && !plan.getRankingSummary().isBlank()) {
            planDetail = planDetail + " | " + plan.getRankingSummary();
        }
        ledger.appendTimeline(run, "PLAN", planDetail);
        ledger.update(run);

        recordPlanMetrics(run, plan);

        run.setStatus(AutonomyRunStatus.EXECUTING);
        executePlanActions(run, plan, context);

        for (var rec : plan.getRecommendations()) {
            ledger.appendTimeline(run, "RECOMMEND", rec.getTitle() + " — " + rec.getDescription());
            autonomyMetrics.recordRecommendation(run.getTenant(), plan.getIncidentType());
        }

        ledger.update(run);
        logger.info("Autonomy run {} tenant={} status={} incident={}",
                run.getRunId(), run.getTenant(), run.getStatus(), plan.getIncidentType());
    }

    /**
     * MQ 限流安全兜底（规则化，无 LLM）：
     * <ol>
     *   <li>超时 → disable + SAFETY_UNWIND，保持 EXECUTING 待恢复或继续观测</li>
     *   <li>连续 N tick 无改善 → disable + ESCALATED + ADVISE</li>
     * </ol>
     *
     * @return true 表示本 tick 已处理完毕（调用方应 return）
     */
    private boolean applyMqThrottleSafetyGuards(AutonomyRun run, AutonomyContext context) {
        if (run.isMqThrottleSafetyConsumed()) {
            return false;
        }
        String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
        if (!"MQ_DEGRADED".equals(incidentType)) {
            return false;
        }
        Instant enabledAt = run.getMqThrottleEnabledAt();
        if (enabledAt == null && actuator.isMqThrottleEnabled()) {
            enabledAt = Optional.ofNullable(actuator.getMqThrottleEnabledAt())
                    .orElse(Optional.empty())
                    .orElse(null);
            if (enabledAt != null) {
                run.setMqThrottleEnabledAt(enabledAt);
            }
        }
        if (enabledAt == null && !actuator.isMqThrottleEnabled()) {
            return false;
        }

        MsMiddlewareProperties.MqActuatorProperties mq = properties.getAutonomy().getMq();
        Instant now = clock.instant();

        // 1) 超时强制回撤
        long maxSeconds = mq.getThrottleMaxDurationSeconds();
        if (maxSeconds > 0 && enabledAt != null) {
            long elapsed = Duration.between(enabledAt, now).getSeconds();
            if (elapsed >= maxSeconds) {
                safetyUnwindThrottle(run, incidentType,
                        String.format("限流超时保护（已持续 %ds ≥ %ds），强制关闭限流，防止误杀常态化",
                                elapsed, maxSeconds));
                ledger.update(run);
                return true;
            }
        }

        // 2) 无改善升级（仅在限流仍开启或已记录启用基线时统计）
        int needTicks = mq.getThrottleNoImproveTicks();
        if (needTicks > 0 && run.getMqFailedCountAtThrottle() >= 0) {
            long current = context.getMqFailedCount();
            long baseline = run.getMqFailedCountAtThrottle();
            // 未下降：相对启用时的失败数没有变好
            boolean noImprove = current >= baseline;
            if (noImprove) {
                run.setThrottleNoImproveTicks(run.getThrottleNoImproveTicks() + 1);
            } else {
                run.setThrottleNoImproveTicks(0);
            }
            if (run.getThrottleNoImproveTicks() >= needTicks) {
                long threshold = context.getMqFailedWarnThreshold();
                escalateForNoImprove(run, incidentType, current, baseline, threshold, needTicks);
                ledger.update(run);
                return true;
            }
        }
        return false;
    }

    private void safetyUnwindThrottle(AutonomyRun run, String incidentType, String message) {
        actuator.clearMqThrottle();
        run.setMqThrottleSafetyConsumed(true);
        ledger.appendTimeline(run, AutonomyTimelinePhase.SAFETY_UNWIND.code(), message);
        autonomyMetrics.recordThrottleSafetyUnwind(run.getTenant(), incidentType);
        logger.warn("Autonomy run {} SAFETY_UNWIND: {}", run.getRunId(), message);
    }

    private void escalateForNoImprove(AutonomyRun run,
                                      String incidentType,
                                      long current,
                                      long baseline,
                                      long threshold,
                                      int needTicks) {
        actuator.clearMqThrottle();
        run.setMqThrottleSafetyConsumed(true);
        run.setStatus(AutonomyRunStatus.ESCALATED);
        String escalateMsg = String.format(
                "限流后连续 %d 次 tick 无改善（mqFailedCount=%d，启用时=%d，阈值=%d），已关闭限流并升级人工",
                needTicks, current, baseline, threshold);
        ledger.appendTimeline(run, AutonomyTimelinePhase.ESCALATE.code(), escalateMsg);
        ledger.appendTimeline(run, AutonomyTimelinePhase.ADVISE.code(),
                "需人工排查消费端/幂等/下游，勿继续自动加压；恢复后可自然 STABLE 或人工结案");
        autonomyMetrics.recordRunEscalated(run.getTenant(), incidentType, "throttle_no_improve");
        logger.warn("Autonomy run {} ESCALATED: {}", run.getRunId(), escalateMsg);
        if (run.getRunId().equals(activeRunId)) {
            // 保持 activeRunId，便于后续恢复走 STABLE；不再新建 AUTO run
            activeRunId = run.getRunId();
        }
    }

    private void recordThrottleEnabled(AutonomyRun run, AutonomyContext context) {
        // 以编排时钟为准写入账本，便于单测用 fixed Clock 验证超时
        Instant at = clock.instant();
        run.setMqThrottleEnabledAt(at);
        run.setMqFailedCountAtThrottle(context != null ? context.getMqFailedCount() : 0);
        run.setThrottleNoImproveTicks(0);
        run.setMqThrottleSafetyConsumed(false);
    }

    /** 应用重启后 activeRunId 丢失时，账本中仍 EXECUTING 的 run 若已恢复则补写 STABLE */
    private void reconcileStaleActiveRuns(AutonomyContext context) {
        for (AutonomyRun run : ledger.listActive()) {
            if (run.getStatus() == AutonomyRunStatus.STABLE || run.getStatus() == AutonomyRunStatus.CLOSED) {
                continue;
            }
            String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
            if (run.getStatus() == AutonomyRunStatus.EXECUTING || run.getStatus() == AutonomyRunStatus.PLANNED) {
                AutonomyContext latest = contextBuilder.build();
                if (applyMqThrottleSafetyGuards(run, latest)) {
                    continue;
                }
            }
            retryRecoveryForActiveRun(context, run);
            AutonomyContext latest = contextBuilder.build();
            if (contextBuilder.isIncidentResolved(incidentType, latest)) {
                stabilizeRun(run, latest);
            }
        }
    }

    /** 当前 run 的主 incident 已恢复，或全局无故障 → 应进入 STABLE */
    private boolean shouldStabilizeActiveRun(AutonomyContext context) {
        if (activeRunId == null) {
            return !context.hasIncident();
        }
        return ledger.get(activeRunId)
                .map(run -> {
                    if (run.getStatus() == AutonomyRunStatus.STABLE || run.getStatus() == AutonomyRunStatus.CLOSED) {
                        return false;
                    }
                    String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
                    return contextBuilder.isIncidentResolved(incidentType, context);
                })
                .orElse(!context.hasIncident());
    }

    /**
     * 执行计划动作：仅 rank#1 且通过 Policy 的动作为 AUTO，其余写入 ADVISE。
     */
    private void executePlanActions(AutonomyRun run, AutonomyPlan plan, AutonomyContext context) {
        for (PlannedAction action : plan.getActions()) {
            AutonomyPolicyDecision decision;
            if (action.getRank() == 1) {
                decision = policy.evaluate(action);
            } else {
                decision = AutonomyPolicyDecision.ADVISE;
            }
            action.setPolicyDecision(decision);

            if (decision == AutonomyPolicyDecision.AUTO) {
                actuator.execute(action);
                ledger.appendTimeline(run, "AUTO",
                        action.getActionType() + " → " + action.getExecutionStatus()
                                + ": " + action.getExecutionDetail());
                autonomyMetrics.recordActionAuto(
                        run.getTenant(),
                        plan.getIncidentType(),
                        action.getActionType().name(),
                        "auto");
                if (action.getActionType() == AutonomyActionType.THROTTLE_CONSUMER
                        && "SUCCESS".equals(action.getExecutionStatus())) {
                    recordThrottleEnabled(run, context);
                }
            } else {
                action.setExecutionStatus("ADVISE");
                String detail = action.getRank() == 1
                        ? String.format("证据强度 %.2f 未达自动门槛（LOW≥%.2f 或 标准≥%.2f），需人工确认",
                        action.getConfidence(), policy.getMinAutoConfidenceLow(), policy.getMinAutoConfidence())
                        : String.format("备选方案 #%d（Runbook 顺位），非首选不自动执行，可人工采纳",
                        action.getRank());
                action.setExecutionDetail(detail);
                ledger.appendTimeline(run, "ADVISE",
                        action.getActionType() + " 建议: " + action.getReason() + "（" + detail + "）");
            }
        }
    }

    /** PLAN 完成后记录 rank#1 证据强度 */
    private void recordPlanMetrics(AutonomyRun run, AutonomyPlan plan) {
        if (plan.getActions() == null || plan.getActions().isEmpty()) {
            return;
        }
        PlannedAction top = plan.getActions().stream()
                .filter(a -> a.getRank() == 1)
                .findFirst()
                .orElse(plan.getActions().get(0));
        autonomyMetrics.recordPlanConfidence(run.getTenant(), plan.getIncidentType(), top.getConfidence());
    }

    /** EXECUTING 期间若组件仍不可用，静默重试自愈（不写重复 PLAN） */
    private void retryRecoveryForActiveRun(AutonomyContext context, AutonomyRun run) {
        if (run.getPlan() == null) {
            return;
        }
        String incidentType = run.getPlan().getIncidentType();
        if ("REDIS_UNAVAILABLE".equals(incidentType) && !context.isRedisHealthy()) {
            PlannedAction action = new PlannedAction();
            action.setActionType(AutonomyActionType.TRIGGER_REDIS_RECOVERY);
            action.setRisk(AutonomyActionType.TRIGGER_REDIS_RECOVERY.getRisk());
            actuator.execute(action);
            if ("SUCCESS".equals(action.getExecutionStatus())) {
                ledger.appendTimeline(run, "AUTO",
                        action.getActionType() + " → SUCCESS: " + action.getExecutionDetail());
            }
        } else if ("RABBITMQ_UNAVAILABLE".equals(incidentType) && !context.isRabbitMqHealthy()) {
            PlannedAction action = new PlannedAction();
            action.setActionType(AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY);
            action.setRisk(AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY.getRisk());
            actuator.execute(action);
            if ("SUCCESS".equals(action.getExecutionStatus())) {
                ledger.appendTimeline(run, "AUTO",
                        action.getActionType() + " → SUCCESS: " + action.getExecutionDetail());
            }
        }
    }

    /** 故障持续期间复用未 STABLE 的 run；否则新建并写入账本 DETECT 事件 */
    private AutonomyRun resolveOrCreateRun(AutonomyContext context) {
        if (activeRunId != null) {
            Optional<AutonomyRun> existing = ledger.get(activeRunId);
            if (existing.isPresent() && existing.get().getStatus() != AutonomyRunStatus.STABLE
                    && existing.get().getStatus() != AutonomyRunStatus.CLOSED) {
                AutonomyRun run = existing.get();
                ensureIncidentBaseline(run, context);
                run.setContext(context);
                return run;
            }
        }
        List<AutonomyRun> activeRuns = ledger.listActive();
        if (!activeRuns.isEmpty()) {
            AutonomyRun run = activeRuns.get(0);
            activeRunId = run.getRunId();
            ensureIncidentBaseline(run, context);
            run.setContext(context);
            return run;
        }
        AutonomyRun run = new AutonomyRun();
        run.setRunId(UUID.randomUUID().toString().substring(0, 8));
        run.setTenant(tenantProvider.getTenant());
        ensureIncidentBaseline(run, context);
        run.setContext(context);
        run.setStatus(AutonomyRunStatus.DETECTED);
        activeRunId = run.getRunId();
        return ledger.startRun(run);
    }

    /** 首次进入故障周期时冻结基线上下文，供 STABLE recoveryEvidence 对比 */
    private void ensureIncidentBaseline(AutonomyRun run, AutonomyContext context) {
        if (run.getIncidentBaseline() == null && context != null) {
            run.setIncidentBaseline(AutonomyContextSnapshot.copy(context));
        }
    }

    /** 中间件指标已正常，但之前有过故障 → 标记 STABLE 并记录 MTTR */
    private void stabilizeActiveRunIfNeeded(AutonomyContext context) {
        if (activeRunId == null) {
            return;
        }
        ledger.get(activeRunId).ifPresent(run -> stabilizeRun(run, context));
    }

    private void stabilizeRun(AutonomyRun run, AutonomyContext context) {
        if (run.getStatus() == AutonomyRunStatus.STABLE || run.getStatus() == AutonomyRunStatus.CLOSED) {
            return;
        }
        run.setStatus(AutonomyRunStatus.STABLE);
        run.setStabilizedAt(context.getCapturedAt());
        run.setContext(context);
        String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : "UNKNOWN";
        RecoveryEvidence evidence = RecoveryEvidenceBuilder.build(
                incidentType, run.getIncidentBaseline(), context);
        run.setRecoveryEvidence(evidence);
        run.getMttrSeconds().ifPresentOrElse(mttr -> {
            if ("MQ_DEGRADED".equals(incidentType)) {
                actuator.clearMqThrottle();
                msMetrics.clearRecentMqFailures();
            }
            ledger.appendTimeline(run, "STABLE", RecoveryEvidenceBuilder.formatStableMessage(evidence, mttr));
            autonomyMetrics.recordRunStabilized(run.getTenant(), incidentType, mttr);
            logger.info("Autonomy run {} stabilized, mttr={}s incident={} evidence={}",
                    run.getRunId(), mttr, incidentType, evidence.getSummary());
        }, () -> ledger.appendTimeline(run, "STABLE",
                RecoveryEvidenceBuilder.formatStableMessageWithoutMttr(evidence)));
        ledger.update(run);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RunStabilizedEvent(this, run));
        }
        if (run.getRunId().equals(activeRunId)) {
            activeRunId = null;
        }
    }
}
