package com.ms.middleware.autonomy.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyRisk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 Step 0：决策模型字段与 JSON 序列化契约。
 */
class Phase3Step0ModelTest {

    @Test
    void plannedActionSerializesRankScoreConfidence() throws Exception {
        PlannedAction action = new PlannedAction();
        action.setActionType(AutonomyActionType.TRIGGER_REDIS_RECOVERY);
        action.setRisk(AutonomyRisk.LOW);
        action.setReason("Redis 不可用，触发自愈优先");
        action.setRank(1);
        action.setScore(0.85);
        action.setConfidence(0.82);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = mapper.writeValueAsString(action);

        assertTrue(json.contains("\"rank\":1"));
        assertTrue(json.contains("\"score\":0.85"));
        assertTrue(json.contains("\"confidence\":0.82"));

        PlannedAction restored = mapper.readValue(json, PlannedAction.class);
        assertEquals(1, restored.getRank());
        assertEquals(0.85, restored.getScore(), 0.001);
        assertEquals(0.82, restored.getConfidence(), 0.001);
    }

    @Test
    void recommendationAssignsStableId() {
        AutonomyRecommendation rec = new AutonomyRecommendation(
                "延长幂等窗口", "故障期可能重复投递", "ms.middleware.mq.idempotent.expiration-hours");

        assertNotNull(rec.getRecommendationId());
        assertEquals(8, rec.getRecommendationId().length());
    }

    @Test
    void recommendationIdPreservedOnDeserialize() throws Exception {
        AutonomyRecommendation rec = new AutonomyRecommendation("t", "d", "cfg");
        String id = rec.getRecommendationId();

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = mapper.writeValueAsString(rec);
        AutonomyRecommendation restored = mapper.readValue(json, AutonomyRecommendation.class);

        assertEquals(id, restored.getRecommendationId());
    }
}
