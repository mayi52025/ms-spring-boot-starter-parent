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
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** 当前 JVM 内正在处理的故障 run；稳定后置 null */
    private volatile String activeRunId;

    public AutonomyOrchestrator(AutonomyContextBuilder contextBuilder,
                                AutonomyDecisionEngine decisionEngine,
                                AutonomyPolicy policy,
                                AutonomyActuator actuator,
                                AutonomyLedger ledger,
                                AutonomyTenantProvider tenantProvider) {
        this.contextBuilder = contextBuilder;
        this.decisionEngine = decisionEngine;
        this.policy = policy;
        this.actuator = actuator;
        this.ledger = ledger;
        this.tenantProvider = tenantProvider;
    }

    /**
     * 单次自治扫描，由 {@link AutonomyScheduler} 定时调用。
     * 无故障时只做稳定判定；有故障时走完整 plan → policy → act 流程。
     */
    public void tick() {
        AutonomyContext context = contextBuilder.build();

        if (!context.hasIncident()) {
            stabilizeActiveRunIfNeeded(context);
            return;
        }

        AutonomyRun run = resolveOrCreateRun(context);
        AutonomyPlan plan = decisionEngine.plan(context);
        run.setPlan(plan);
        run.setStatus(AutonomyRunStatus.PLANNED);
        ledger.appendTimeline(run, "PLAN", plan.getSummary());
        ledger.update(run);

        run.setStatus(AutonomyRunStatus.EXECUTING);
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

        for (var rec : plan.getRecommendations()) {
            ledger.appendTimeline(run, "RECOMMEND", rec.getTitle() + " — " + rec.getDescription());
        }

        ledger.update(run);
        logger.info("Autonomy run {} tenant={} status={} incident={}",
                run.getRunId(), run.getTenant(), run.getStatus(), plan.getIncidentType());
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
        ledger.get(activeRunId).ifPresent(run -> {
            if (run.getStatus() == AutonomyRunStatus.STABLE || run.getStatus() == AutonomyRunStatus.CLOSED) {
                return;
            }
            run.setStatus(AutonomyRunStatus.STABLE);
            run.setStabilizedAt(context.getCapturedAt());
            ledger.appendTimeline(run, "STABLE", "中间件指标恢复正常，本次自治结束");
            ledger.update(run);
            activeRunId = null;
            run.getMttrSeconds().ifPresent(mttr ->
                    logger.info("Autonomy run {} stabilized, mttr={}s", run.getRunId(), mttr));
        });
    }
}
