package com.ms.middleware.autonomy.adoption;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.adoption.nacos.InMemoryNacosConfigDraftService;
import com.ms.middleware.autonomy.adoption.nacos.NacosConfigDraftService;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import com.ms.middleware.autonomy.plan.AutonomyRecommendation;
import com.ms.middleware.autonomy.plan.RecommendationStatus;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * nacos-draft 模式：草稿创建、diff 回写、二次发布与时间线 PUBLISH。
 */
@ExtendWith(MockitoExtension.class)
class HumanAdoptionServiceNacosDraftTest {

    private static final String TENANT = "order-system";
    private static final String RUN_ID = "run-nacos1";

    @Mock
    private AutonomyActuator actuator;

    @Mock
    private AutonomyMetrics autonomyMetrics;

    private InMemoryAutonomyLedger ledger;
    private HumanAdoptionService service;
    private NacosConfigDraftService draftService;

    @BeforeEach
    void setUp() {
        List<Object> events = new ArrayList<>();
        ApplicationEventPublisher publisher = events::add;
        AutonomyTenantProvider tenantProvider = () -> TENANT;
        ledger = new InMemoryAutonomyLedger(publisher, tenantProvider, 20);

        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getAutonomy().getAdoption().setMode("nacos-draft");
        draftService = new InMemoryNacosConfigDraftService(properties);
        service = new HumanAdoptionService(ledger, actuator, autonomyMetrics, draftService);
    }

    /** 采纳时应生成草稿、写入 diff，且未发布生产 */
    @Test
    void acceptRecommendationCreatesDraftWithDiff() {
        AutonomyRecommendation rec = recommendation("rec-draft", "调大 MQ 阈值");
        AutonomyRun run = runWithRecommendation(rec);
        ledger.startRun(run);

        AdoptionRequest request = new AdoptionRequest();
        request.setRunId(RUN_ID);
        request.setOperator("ops-li");

        AdoptionResult result = service.acceptRecommendation("rec-draft", request);

        assertTrue(result.isSuccess());
        assertEquals("rec-draft", result.getDraftId());
        assertNotNull(result.getDiffSummary());
        assertFalse(Boolean.TRUE.equals(result.getNacosPublished()));
        assertEquals(RecommendationStatus.ACCEPTED, rec.getStatus());
        assertNotNull(rec.getDiffSummary());
        assertFalse(rec.isNacosPublished());
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.ACCEPTED.code().equals(e.getPhase())
                        && e.getMessage().contains("draftId=rec-draft")));
    }

    /** 二次确认发布应写 PUBLISH 时间线并标记 nacosPublished */
    @Test
    void publishDraftWritesPublishTimeline() {
        AutonomyRecommendation rec = recommendation("rec-pub", "限流配置");
        AutonomyRun run = runWithRecommendation(rec);
        ledger.startRun(run);

        AdoptionRequest request = new AdoptionRequest();
        request.setRunId(RUN_ID);
        service.acceptRecommendation("rec-pub", request);

        AdoptionResult publish = service.publishRecommendationDraft("rec-pub", request);

        assertTrue(publish.isSuccess());
        assertTrue(rec.isNacosPublished());
        assertTrue(run.getTimeline().stream()
                .anyMatch(e -> AutonomyTimelinePhase.PUBLISH.code().equals(e.getPhase())));
    }

    /** 重复发布应幂等 */
    @Test
    void publishDraftIsIdempotent() {
        AutonomyRecommendation rec = recommendation("rec-idem-pub", "缓存 TTL");
        AutonomyRun run = runWithRecommendation(rec);
        ledger.startRun(run);

        AdoptionRequest request = new AdoptionRequest();
        request.setRunId(RUN_ID);
        service.acceptRecommendation("rec-idem-pub", request);
        service.publishRecommendationDraft("rec-idem-pub", request);

        int timelineSize = run.getTimeline().size();
        AdoptionResult second = service.publishRecommendationDraft("rec-idem-pub", request);

        assertTrue(second.isSuccess());
        assertEquals("ALREADY_PUBLISHED", second.getCode());
        assertEquals(timelineSize, run.getTimeline().size());
    }

    /** audit-only 模式下 publish 应拒绝 */
    @Test
    void publishFailsInAuditOnlyMode() {
        HumanAdoptionService auditOnly = new HumanAdoptionService(
                ledger, actuator, autonomyMetrics, NacosConfigDraftService.noop());
        AutonomyRecommendation rec = recommendation("rec-audit", "仅审计");
        AutonomyRun run = runWithRecommendation(rec);
        ledger.startRun(run);

        AdoptionRequest request = new AdoptionRequest();
        request.setRunId(RUN_ID);
        auditOnly.acceptRecommendation("rec-audit", request);

        AdoptionResult publish = auditOnly.publishRecommendationDraft("rec-audit", request);
        assertFalse(publish.isSuccess());
        assertEquals("NOT_SUPPORTED", publish.getCode());
    }

    private static AutonomyRecommendation recommendation(String id, String title) {
        AutonomyRecommendation rec = new AutonomyRecommendation();
        rec.setRecommendationId(id);
        rec.setTitle(title);
        rec.setDescription("建议说明");
        rec.setSuggestedConfig("ms.middleware.autonomy.mq.throttle-limit=50");
        return rec;
    }

    private static AutonomyRun runWithRecommendation(AutonomyRecommendation rec) {
        AutonomyPlan plan = new AutonomyPlan();
        plan.getRecommendations().add(rec);
        AutonomyRun run = new AutonomyRun();
        run.setRunId(RUN_ID);
        run.setTenant(TENANT);
        run.setPlan(plan);
        return run;
    }
}
