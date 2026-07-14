package com.ms.middleware.autonomy.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutonomyRun JSON 序列化，供 Redisson 账本持久化。
 *
 * <p>Phase 4.5：容错反序列化——旧 schema 字段、局部损坏时仍返回可读 stub，避免控制台整页空白。</p>
 */
public class AutonomyRunSerde {

    private static final Logger logger = LoggerFactory.getLogger(AutonomyRunSerde.class);
    private static final Pattern RUN_ID_PATTERN = Pattern.compile("\"runId\"\\s*:\\s*\"([^\"]+)\"");

    /** 当前写入 Redis 的 schema 版本 */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final ObjectMapper tolerantMapper;

    public AutonomyRunSerde(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.tolerantMapper = JsonMapper.builder()
                .findAndAddModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    public String toJson(AutonomyRun run) {
        if (run.getSchemaVersion() <= 0) {
            run.setSchemaVersion(CURRENT_SCHEMA_VERSION);
        }
        try {
            return objectMapper.writeValueAsString(run);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AutonomyRun: " + run.getRunId(), e);
        }
    }

    /**
     * 反序列化；完全失败时返回 stub（含 runId / tenant / 降级说明），不抛异常。
     */
    public Optional<AutonomyRun> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            AutonomyRun run = tolerantMapper.readValue(json, AutonomyRun.class);
            normalizeSchema(run);
            return Optional.of(run);
        } catch (Exception primary) {
            logger.warn("AutonomyRun 反序列化失败，尝试 stub 降级: {}", primary.getMessage());
            return Optional.of(buildStub(json, primary));
        }
    }

    private void normalizeSchema(AutonomyRun run) {
        if (run.getSchemaVersion() <= 0) {
            run.setSchemaVersion(1);
        }
    }

    /** 从损坏 JSON 提取 runId/tenant，生成最小可读 run */
    private AutonomyRun buildStub(String json, Exception cause) {
        AutonomyRun stub = new AutonomyRun();
        stub.setSchemaVersion(0);
        stub.setLedgerCorrupted(true);
        stub.setRunId(extractField(json, RUN_ID_PATTERN, "unknown"));
        stub.setTenant(extractField(json, Pattern.compile("\"tenant\"\\s*:\\s*\"([^\"]+)\""), "unknown"));
        stub.setStatus(com.ms.middleware.autonomy.AutonomyRunStatus.CLOSED);
        stub.addTimeline(new TimelineEvent(
                stub.getRunId(),
                AutonomyTimelinePhase.STABLE.code(),
                "账本数据部分损坏，详情不可展开（schema 不兼容或字段缺失）"));
        logger.debug("AutonomyRun stub 降级 runId={} cause={}", stub.getRunId(), cause.getMessage());
        return stub;
    }

    private static String extractField(String json, Pattern pattern, String defaultValue) {
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return defaultValue;
    }
}
