package com.ms.middleware.autonomy;

import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.context.AutonomyContextBuilder;
import com.ms.middleware.autonomy.decision.AutonomyDecisionEngine;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.policy.AutonomyPolicy;
import com.ms.middleware.autonomy.run.AutonomyLedger;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** 当前 JVM 内正在处理的故障 run；稳定后置 null */
    private volatile String activeRunId;

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider,
                                AutonomyMetrics autonomyMetrics) {
        this.contextBuilder = contextBuilder;
        this.decisionEngine = decisionEngine;
        this.policy = policy;
        this.actuator = actuator;
        this.ledger = ledger;
        this.tenantProvider = tenantProvider;
        this.autonomyMetrics = autonomyMetrics;
    }

    /**
     * 单次自治扫描，由 {@link AutonomyScheduler} 定时调用。
     * 无故障时只做稳定判定；有故障时走完整 plan → policy → act 流程。
     */
    public void tick() {
        AutonomyContext context = contextBuilder.build();

        reconcileStaleActiveRuns(context);

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

        // 同一故障周期内已 EXECUTING：只更新上下文并尝试重连，避免每轮重复刷 PLAN/ACTION
        if (run.getStatus() == AutonomyRunStatus.EXECUTING || run.getStatus() == AutonomyRunStatus.PLANNED) {
            run.setContext(context);
            String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
            if (contextBuilder.isIncidentResolved(incidentType, context)) {
                activeRunId = run.getRunId();
                stabilizeActiveRunIfNeeded(context);
                return;
            }
            retryRecoveryForActiveRun(context, run);
            ledger.update(run);
            return;
        }

        AutonomyPlan plan = decisionEngine.plan(context);
        run.setPlan(plan);
        run.setStatus(AutonomyRunStatus.PLANNED);
        ledger.appendTimeline(run, "PLAN", plan.getSummary());
        ledger.update(run);

        run.setStatus(AutonomyRunStatus.EXECUTING);
        executePlanActions(run, plan);

        for (var rec : plan.getRecommendations()) {
            ledger.appendTimeline(run, "RECOMMEND", rec.getTitle() + " — " + rec.getDescription());
        }

        ledger.update(run);
        logger.info("Autonomy run {} tenant={} status={} incident={}",
                run.getRunId(), run.getTenant(), run.getStatus(), plan.getIncidentType());
    }

    /** 应用重启后 activeRunId 丢失时，账本中仍 EXECUTING 的 run 若已恢复则补写 STABLE */
    private void reconcileStaleActiveRuns(AutonomyContext context) {
        for (AutonomyRun run : ledger.listActive()) {
            if (run.getStatus() == AutonomyRunStatus.STABLE || run.getStatus() == AutonomyRunStatus.CLOSED) {
                continue;
            }
            String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : null;
            if (contextBuilder.isIncidentResolved(incidentType, context)) {
                stabilizeRun(run, context);
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

    private void executePlanActions(AutonomyRun run, AutonomyPlan plan) {
        for (PlannedAction action : plan.getActions()) {
            AutonomyPolicyDecision decision = policy.evaluate(action);
            action.setPolicyDecision(decision);
            if (decision == AutonomyPolicyDecision.AUTO) {
                actuator.execute(action);
                ledger.appendTimeline(run, "ACTION",
                        action.getActionType() + " → " + action.getExecutionStatus() + ": " + action.getExecutionDetail());
            } else {
                action.setExecutionStatus("ADVISE");
                action.setExecutionDetail("风险超过 auto-execute-max-risk，仅展示建议，等待人工确认");
                ledger.appendTimeline(run, "ADVISE",
                        action.getActionType() + " 建议: " + action.getReason());
            }
        }
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
                ledger.appendTimeline(run, "ACTION",
                        action.getActionType() + " → SUCCESS: " + action.getExecutionDetail());
            }
        } else if ("RABBITMQ_UNAVAILABLE".equals(incidentType) && !context.isRabbitMqHealthy()) {
            PlannedAction action = new PlannedAction();
            action.setActionType(AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY);
            action.setRisk(AutonomyActionType.TRIGGER_RABBITMQ_RECOVERY.getRisk());
            actuator.execute(action);
            if ("SUCCESS".equals(action.getExecutionStatus())) {
                ledger.appendTimeline(run, "ACTION",
                        action.getActionType() + " → SUCCESS: " + action.getExecutionDetail());
            }
        }
    }

    /** 故障持续期间复用未 STABLE 的 run；否则新建并写入账本 DETECT 事件 */
    private AutonomyRun resolveOrCreateRun(AutonomyContext context) {
        if (activeRunId != null) {
            Optional<AutonomyRun> existing = ledger.get(activeRunId);
            if (existing.isPresent() && existing.get().getStatus() != AutonomyRunStatus.STABLE) {
                AutonomyRun run = existing.get();
                run.setContext(context);
                return run;
            }
        }
        List<AutonomyRun> activeRuns = ledger.listActive();
        if (!activeRuns.isEmpty()) {
            AutonomyRun run = activeRuns.get(0);
            activeRunId = run.getRunId();
            run.setContext(context);
            return run;
        }
        AutonomyRun run = new AutonomyRun();
        run.setRunId(UUID.randomUUID().toString().substring(0, 8));
        run.setTenant(tenantProvider.getTenant());
        run.setContext(context);
        run.setStatus(AutonomyRunStatus.DETECTED);
        activeRunId = run.getRunId();
        return ledger.startRun(run);
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
        run.getMttrSeconds().ifPresentOrElse(mttr -> {
            String incidentType = run.getPlan() != null ? run.getPlan().getIncidentType() : "UNKNOWN";
            ledger.appendTimeline(run, "STABLE",
                    String.format("中间件指标恢复正常，MTTR=%ds，本次自治结束", mttr));
            autonomyMetrics.recordRunStabilized(run.getTenant(), incidentType, mttr);
            logger.info("Autonomy run {} stabilized, mttr={}s incident={}",
                    run.getRunId(), mttr, incidentType);
        }, () -> ledger.appendTimeline(run, "STABLE", "中间件指标恢复正常，本次自治结束"));
        ledger.update(run);
        if (run.getRunId().equals(activeRunId)) {
            activeRunId = null;
        }
    }
}
