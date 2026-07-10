package com.ms.middleware.autonomy.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AutonomyRun JSON 序列化，供 Redisson 账本持久化。
 * 使用 Spring 注入的 ObjectMapper（已注册 JavaTimeModule）。
 */
public class AutonomyRunSerde {

    private final ObjectMapper objectMapper;

    public AutonomyRunSerde(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    public String toJson(AutonomyRun run) {
        try {
            return objectMapper.writeValueAsString(run);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AutonomyRun: " + run.getRunId(), e);
        }
    }

    public AutonomyRun fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AutonomyRun.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize AutonomyRun", e);
        }
    }
}
