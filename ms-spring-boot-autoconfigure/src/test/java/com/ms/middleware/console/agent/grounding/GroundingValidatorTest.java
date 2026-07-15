package com.ms.middleware.console.agent.grounding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundingValidatorTest {

    private final GroundingValidator validator = new GroundingValidator();

    @Test
    void relaxedModeAlwaysPasses() {
        GroundingResolution resolution = new GroundingResolution(
                GroundingIntent.ACTIVE_ISSUES,
                List.of(InsightToolInvocation.of(InsightToolName.LIST_ACTIVE_ISSUES)));

        GroundingValidator.ValidationResult result = validator.validate(
                "编造内容",
                List.of(),
                resolution,
                GroundingMode.RELAXED,
                "");

        assertTrue(result.grounded());
    }

    @Test
    void strictModeRequiresTools() {
        GroundingResolution resolution = new GroundingResolution(
                GroundingIntent.ACTIVE_ISSUES,
                List.of(InsightToolInvocation.of(InsightToolName.LIST_ACTIVE_ISSUES)));

        GroundingValidator.ValidationResult result = validator.validate(
                "",
                List.of(),
                resolution,
                GroundingMode.STRICT,
                "【listActiveIssues】\n无活跃故障");

        assertFalse(result.grounded());
        assertTrue(result.fallbackReply().contains("Insight Tool"));
    }

    @Test
    void strictModePassesWhenRequiredToolUsed() {
        GroundingResolution resolution = new GroundingResolution(
                GroundingIntent.METRICS,
                List.of(InsightToolInvocation.of(InsightToolName.GET_METRICS_SUMMARY)));

        GroundingValidator.ValidationResult result = validator.validate(
                "当前 MQ 失败为 0",
                List.of("getMetricsSummary"),
                resolution,
                GroundingMode.STRICT,
                "mqFailedCount=0");

        assertTrue(result.grounded());
    }
}
