package com.ms.middleware.autonomy.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.context.AutonomyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账本 JSON 兼容：旧 schema（含 mqDegraded 字段）与损坏 JSON 的容错反序列化。
 */
class AutonomyRunSerdeTest {

    private AutonomyRunSerde serde;

    @BeforeEach
    void setUp() {
        serde = new AutonomyRunSerde(new ObjectMapper().findAndRegisterModules());
    }

    /** 模拟 Redis 中旧版 JSON：context 含已废弃的 mqDegraded/cacheDegraded 字段 */
    @Test
    void deserializesLegacyJsonWithMqDegradedField() {
        String legacyJson = """
                {
                  "runId": "d73cb81a",
                  "tenant": "order-system",
                  "status": "EXECUTING",
                  "startedAt": "2026-07-14T08:00:00Z",
                  "context": {
                    "mqFailedCount": 3,
                    "mqFailedWarnThreshold": 3,
                    "cacheHitRate": 1.0,
                    "cacheHitRateWarnThreshold": 0.5,
                    "issues": ["MQ 消费失败（窗口内）偏高: 3"],
                    "mqDegraded": true,
                    "cacheDegraded": false
                  }
                }
                """;

        AutonomyRun run = serde.fromJson(legacyJson).orElseThrow();

        assertEquals("d73cb81a", run.getRunId());
        assertEquals(AutonomyRunStatus.EXECUTING, run.getStatus());
        assertFalse(run.isLedgerCorrupted());
        assertTrue(run.getContext().isMqDegraded());
    }

    @Test
    void writesSchemaVersionOnSerialize() {
        AutonomyRun run = new AutonomyRun();
        run.setRunId("new-run");
        run.setTenant("order-system");

        String json = serde.toJson(run);

        assertTrue(json.contains("\"schemaVersion\":1"));
    }

    @Test
    void returnsStubWhenJsonTotallyInvalid() {
        AutonomyRun stub = serde.fromJson("{not-valid-json").orElseThrow();

        assertTrue(stub.isLedgerCorrupted());
        assertEquals(0, stub.getSchemaVersion());
    }

    @Test
    void stubExtractsRunIdFromPartialJson() {
        String broken = "{\"runId\":\"abc12345\",\"tenant\":\"order-system\",\"context\":{broken";
        AutonomyRun stub = serde.fromJson(broken).orElseThrow();

        assertEquals("abc12345", stub.getRunId());
        assertEquals("order-system", stub.getTenant());
        assertTrue(stub.isLedgerCorrupted());
    }

    @Test
    void roundTripPreservesContextIssues() {
        AutonomyContext ctx = new AutonomyContext();
        ctx.setIssues(List.of("MQ 消费失败（窗口内）偏高: 3"));
        ctx.setMqFailedCount(3);
        ctx.setMqFailedWarnThreshold(3);

        AutonomyRun run = new AutonomyRun();
        run.setRunId("rt1");
        run.setTenant("t1");
        run.setStartedAt(Instant.parse("2026-07-14T08:00:00Z"));
        run.setContext(ctx);

        AutonomyRun restored = serde.fromJson(serde.toJson(run)).orElseThrow();

        assertEquals("rt1", restored.getRunId());
        assertTrue(restored.getContext().getIssues().contains("MQ 消费失败（窗口内）偏高: 3"));
    }
}
