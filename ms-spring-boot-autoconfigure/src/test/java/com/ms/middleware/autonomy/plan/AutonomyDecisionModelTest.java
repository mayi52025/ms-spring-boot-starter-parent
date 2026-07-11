package com.ms.middleware.autonomy.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.AutonomyRisk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证决策模型字段（排序分、推荐 ID）的 JSON 序列化契约，
 * 确保账本持久化与控制台 API 能正确读写。
 */
class AutonomyDecisionModelTest {

    /** PlannedAction 的 rank/score/confidence 应能序列化到 Redis 账本 */
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

    /** 新建推荐应自动分配 8 位 recommendationId */
    @Test
    void recommendationAssignsStableId() {
        AutonomyRecommendation rec = new AutonomyRecommendation(
                "延长幂等窗口", "故障期可能重复投递", "ms.middleware.mq.idempotent.expiration-hours");

        assertNotNull(rec.getRecommendationId());
        assertEquals(8, rec.getRecommendationId().length());
    }

    /** 从 JSON 反序列化后 recommendationId 不应丢失 */
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
