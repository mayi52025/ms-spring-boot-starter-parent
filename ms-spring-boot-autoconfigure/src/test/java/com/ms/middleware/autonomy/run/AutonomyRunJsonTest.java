package com.ms.middleware.autonomy.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.context.AutonomyContext;
import com.ms.middleware.autonomy.plan.AutonomyPlan;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomyRunJsonTest {

    @Test
    void serializesWithPlainObjectMapperLikeStarter() throws Exception {
        // 模拟 starter 提供的 ObjectMapper（须支持 Instant）
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        AutonomyRun run = new AutonomyRun();
        run.setRunId("r1");
        run.setStartedAt(Instant.parse("2026-07-10T09:00:00Z"));
        run.addTimeline(new TimelineEvent("r1", "DETECT", "test"));

        String json = mapper.writeValueAsString(run);
        assertTrue(json.contains("\"startedAt\""));
        assertTrue(json.contains("\"runId\":\"r1\""));
    }

    @Test
    void serializesActiveRunForIssuesApi() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        AutonomyContext context = new AutonomyContext();
        context.setRedisHealthy(false);
        context.setIssues(List.of("Redis 不可用，分布式缓存 L2 可能失效"));

        AutonomyPlan plan = new AutonomyPlan();
        plan.setIncidentType("REDIS_UNAVAILABLE");
        plan.setSummary("Redis 不可用，启用本地缓存与自愈组合处置");
        plan.setContext(context);

        AutonomyRun run = new AutonomyRun();
        run.setRunId("abc12345");
        run.setTenant("order-system");
        run.setStatus(AutonomyRunStatus.EXECUTING);
        run.setStartedAt(Instant.now());
        run.setContext(context);
        run.setPlan(plan);
        run.addTimeline(new TimelineEvent("abc12345", "DETECT", "检测到中间件异常"));

        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(run));
        assertTrue(json.contains("\"issues\""));
        assertTrue(json.contains("Redis 不可用"));
    }

    @Test
    void serializesRecoveryEvidenceOnStableRun() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        com.ms.middleware.autonomy.recovery.RecoveryEvidence evidence =
                new com.ms.middleware.autonomy.recovery.RecoveryEvidence();
        evidence.setIncidentType("MQ_DEGRADED");
        evidence.setSummary("MQ窗口失败 5→0（阈值<3）");

        AutonomyRun run = new AutonomyRun();
        run.setRunId("stable01");
        run.setStatus(AutonomyRunStatus.STABLE);
        run.setRecoveryEvidence(evidence);

        String json = mapper.writeValueAsString(run);
        assertTrue(json.contains("\"recoveryEvidence\""));
        assertTrue(json.contains("MQ窗口失败 5→0"));
    }
}
