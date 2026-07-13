package com.ms.middleware.autonomy.adoption;

import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.AutonomyRisk;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.AutonomyRecommendation;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.plan.RecommendationStatus;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.run.InMemoryAutonomyLedger;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 人机采纳服务：幂等、冲突、备选动作执行与时间线审计。
 */
@ExtendWith(MockitoExtension.class)
class HumanAdoptionServiceTest {

    private static final String TENANT = "order-system";
    private static final String RUN_ID = "run-adopt1";

    @Mock
    private AutonomyActuator actuator;

    @Mock
    private AutonomyMetrics autonomyMetrics;

    private InMemoryAutonomyLedger ledger;
    private HumanAdoptionService service;

    @BeforeEach
    void setUp() {
        List<Object> events = new ArrayList<>();
        ApplicationEventPublisher publisher = events::add;
        AutonomyTenantProvider tenantProvider = () -> TENANT;
        ledger = new InMemoryAutonomyLedger(publisher, tenantProvider, 20);
        service = new HumanAdoptionService(ledger, actuator, autonomyMetrics);
    }

    /** 首次采纳推荐应更新状态并写入带 recommendationId 的 ACCEPTED 时间线 */
    @Test
    void acceptRecommendationUpdatesStatusAndTimeline() {
        AutonomyRecommendation rec = recommendation("rec-0001", "调大 MQ 阈值");
        AutonomyRun run = runWithRecommendation(rec);
        ledger.startRun(run);

        AdoptionRequest request = new AdoptionRequest();
        request.setRunId(RUN_ID);
        request.setOperator("ops-zhang");

        AdoptionResult result = service.acceptRecommendation("rec-0001", request);

        assertTrue(result.isSuccess());
        assertEquals("OK", result.getCode());
        assertEquals(RecommendationStatus.ACCEPTED, rec.getStatus());
        assertEquals("ops-zhang", rec.getOperator());
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.ACCEPTED.code().equals(e.getPhase())
                        && "rec-0001".equals(e.getRecommendationId())));
    }

    /** 重复采纳应幂等返回 ALREADY_ACCEPTED，不重复写时间线 */
    @Test
    void acceptRecommendationIsIdempotent() {
        AutonomyRecommendation rec = recommendation("rec-idem", "缓存预热");
        AutonomyRun run = runWithRecommendation(rec);
        ledger.startRun(run);

        AdoptionRequest request = new AdoptionRequest();
        request.setRunId(RUN_ID);

        service.acceptRecommendation("rec-idem", request);
        int timelineSize = run.getTimeline().size();

        AdoptionResult second = service.acceptRecommendation("rec-idem", request);

        assertTrue(second.isSuccess());
        assertEquals("ALREADY_ACCEPTED", second.getCode());
        assertEquals(timelineSize, run.getTimeline().size());
    }

    /** 已采纳的推荐不可再拒绝 */
    @Test
    void rejectAfterAcceptReturnsConflict() {
        AutonomyRecommendation rec = recommendation("rec-conf", "限流配置");
        AutonomyRun run = runWithRecommendation(rec);
        ledger.startRun(run);

        AdoptionRequest request = new AdoptionRequest();
        request.setRunId(RUN_ID);
        service.acceptRecommendation("rec-conf", request);

        AdoptionResult reject = service.rejectRecommendation("rec-conf", request);

        assertFalse(reject.isSuccess());
        assertEquals("CONFLICT", reject.getCode());
    }

    /** 人工采纳备选动作应执行 Actuator 并补 ACCEPTED + AUTO 时间线 */
    @Test
    void acceptAdvisedActionExecutesAndRecordsTimeline() {
        PlannedAction advised = plannedAction(2, AutonomyActionType.THROTTLE_CONSUMER, AutonomyPolicyDecision.ADVISE);
        AutonomyRun run = runWithActions(advised);
        ledger.startRun(run);

        doAnswer(inv -> {
            PlannedAction a = inv.getArgument(0);
            a.setExecutionStatus("SUCCESS");
            a.setExecutionDetail("限流已启用");
            return null;
        }).when(actuator).execute(any(PlannedAction.class));

        AdoptionRequest request = new AdoptionRequest();
        request.setOperator("oncall-li");

        AdoptionResult result = service.acceptAdvisedAction(RUN_ID, 2, request);

        assertTrue(result.isSuccess());
        assertTrue(advised.isHumanAccepted());
        assertEquals(AutonomyPolicyDecision.AUTO, advised.getPolicyDecision());
        verify(actuator).execute(advised);
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.ACCEPTED.code().equals(e.getPhase())));
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.AUTO.code().equals(e.getPhase())));
    }

    /** 已人工采纳的动作再次调用应幂等，不再执行 */
    @Test
    void acceptAdvisedActionSkipsWhenAlreadyAccepted() {
        PlannedAction advised = plannedAction(2, AutonomyActionType.DELAYED_RETRY_BATCH, AutonomyPolicyDecision.ADVISE);
        advised.setHumanAccepted(true);
        AutonomyRun run = runWithActions(advised);
        ledger.startRun(run);

        AdoptionResult result = service.acceptAdvisedAction(RUN_ID, 2, null);

        assertTrue(result.isSuccess());
        verify(actuator, never()).execute(any());
    }

    private static AutonomyRecommendation recommendation(String id, String title) {
        AutonomyRecommendation rec = new AutonomyRecommendation();
        rec.setRecommendationId(id);
        rec.setTitle(title);
        rec.setDescription("建议说明");
        rec.setSuggestedConfig("ms.middleware.autonomy.mq.throttle-limit=50");
        return rec;
    }

    private static PlannedAction plannedAction(int rank, AutonomyActionType type, AutonomyPolicyDecision decision) {
        PlannedAction action = new PlannedAction();
        action.setRank(rank);
        action.setActionType(type);
        action.setRisk(AutonomyRisk.LOW);
        action.setReason("备选方案");
        action.setPolicyDecision(decision);
        action.setExecutionStatus("ADVISE");
        return action;
    }

    private static AutonomyRun runWithRecommendation(AutonomyRecommendation rec) {
        AutonomyPlan plan = new AutonomyPlan();
        plan.getRecommendations().add(rec);
        AutonomyRun run = baseRun();
        run.setPlan(plan);
        return run;
    }

    private static AutonomyRun runWithActions(PlannedAction... actions) {
        AutonomyPlan plan = new AutonomyPlan();
        for (PlannedAction a : actions) {
            plan.getActions().add(a);
        }
        AutonomyRun run = baseRun();
        run.setPlan(plan);
        return run;
    }

    private static AutonomyRun baseRun() {
        AutonomyRun run = new AutonomyRun();
        run.setRunId(RUN_ID);
        run.setTenant(TENANT);
        return run;
    }
}
